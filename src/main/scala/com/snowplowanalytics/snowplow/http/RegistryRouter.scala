package com.snowplowanalytics.snowplow.http

import akka.actor.typed.ActorRef
import akka.http.scaladsl.server.Directives.{entity, _}
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.model.headers.Location
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.snowplowanalytics.snowplow.models.Request._
import com.snowplowanalytics.snowplow.models.{Command, Response}
import io.circe.syntax.EncoderOps

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * The available routes of the application.
  * Handles all available [[com.snowplowanalytics.snowplow.models.Request Requests]].
  * Namely:
  * it can post a new schema in the registry, get that schema and
  * validate a Json document using a specified schema.
  * @param registry the identity of an actor instance of type Command
  * @param system an actor system to hold our actors
  */
class RegistryRouter(registry: ActorRef[Command])(implicit system: ActorSystem[_]) {

  import Response._
  import Command._
  implicit val timeout: Timeout = Timeout(15.seconds)

  def createRegistry(name: String, request: RegistryCreationRequest): Future[Response] =
    registry.ask(replyTo => request.toCommand(name, replyTo))

  def getSchema(id: String): Future[Response] = registry.ask(replyTo => GetSchema(id, replyTo))

  def validateSchema(id: String, request: SchemaValidateRequest): Future[Response] =
    registry.ask(replyTo => request.toCommand(id, replyTo))

  // Uploads a schema and returns a response.
  // If command is successful a 201 status code will be returned, along with the proper message.
  // Else we receive a status code 400 and the message contains the exception thrown.
  val createSchemaRoute: Route = path(Segment) { name =>
    post {
      entity(as[RegistryCreationRequest]) { request =>
        onSuccess(createRegistry(name, request)) {
          case SchemaCreatedResponse(_, _, Some(_), _) =>
            complete(
              HttpResponse(
                StatusCodes.Created,
                headers = Seq(Location(s"/registry/$name")),
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  SuccessResponse("uploadSchema", name, "success").asJson.toString()
                )
              )
            )
          case SchemaCreatedResponse(_, name, None, Some(ex)) =>
            complete(
              HttpResponse(
                StatusCodes.BadRequest,
                headers = Seq(Location(s"/registry/$name")),
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  FailureResponse(
                    "uploadSchema",
                    name,
                    "error",
                    s"Failed to upload schema $name. $ex"
                  ).asJson.toString()
                )
              )
            )
        }
      }
    }
  }

  // Gets a schema and pass it in the response.
  // If command is successful a 20 status code will be returned, along with the proper message.
  // Else we receive a status code 404 - the schema cannot be found.
  val getSchemaRoute: Route = path(Segment) { id =>
    get {
      onSuccess(getSchema(id)) {
        case GetSchemaResponse(Some(schema)) =>
          complete(
            HttpResponse(
              StatusCodes.OK,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                SuccessResponseWithSchema("getSchema", id, "success", schema).asJson.toString()
              )
            )
          )
        case GetSchemaResponse(None) =>
          complete(
            HttpResponse(
              StatusCodes.NotFound,
              entity = HttpEntity(
                ContentTypes.`application/json`,
                FailureResponse("getSchema", id, "error", s"Schema $id not found.").asJson
                  .toString()
              )
            )
          )
      }
    }
  }

  // Validates a document over a schema and returns a response.
  // If command is successful a 200 status code will be returned, along with the proper message.
  // The message contains the report of the validation.
  // Else we receive a status code 400 if there is an error when loading either the schema or document
  val validateDocumentRoute: Route = path(Segment) { id =>
    post {
      entity(as[SchemaValidateRequest]) { request =>
        onSuccess(validateSchema(id, request)) {
          // send HTTP response
          case SchemaValidatedResponse(Success(report)) =>
            complete(
              HttpResponse(
                StatusCodes.OK,
                entity = HttpEntity(
                  ContentTypes.`application/json`,
                  report match {
                    case "success" =>
                      SuccessResponse("validatedSchema", id, "success").asJson.toString()
                    case _ =>
                      FailureResponse("validatedSchema", id, "error", report).asJson
                        .toString()
                  }
                )
              )
            )
          case SchemaValidatedResponse(Failure(ex)) =>
            complete(
              StatusCodes.BadRequest,
              FailureResponse(
                "schemaValidate",
                id,
                "error",
                s"Failed to validate: ${request.document}. ${ex.toString}"
              )
            )
        }
      }
    }
  }

  // Chains the routes together
  val routes: Route = pathPrefix("registry") { createSchemaRoute ~ getSchemaRoute } ~
    pathPrefix("validate") { validateDocumentRoute }
}
