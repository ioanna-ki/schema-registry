package com.snowplowanalytics.snowplow.actors

import akka.actor.typed.Behavior
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.PersistenceId
import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.load.configuration.{
  LoadingConfiguration,
  LoadingConfigurationBuilder
}
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import com.snowplowanalytics.snowplow.utils.Utils
import com.snowplowanalytics.snowplow.models.{Command, Event, Response}

import scala.util.{Failure, Success, Try}

object PersistentSchema {

  val builder: LoadingConfigurationBuilder = LoadingConfiguration.newBuilder()

  import Command._
  import Response._
  import Event._

  // handles each command
  val commandHandler: (SchemaRegistry, Command) => Effect[Event, SchemaRegistry] =
    (state, command) =>
      command match {
        case CreateSchema(name, schema, replyTo) =>
          val stateId = name
          Try { JsonLoader.fromPath(schema).toPrettyString } match {
            case Success(s) =>
              Effect
                .persist(
                  SchemaCreated(SchemaRegistry(stateId, name, s))
                ) // stores the schema to Cassandra
                .thenReply(replyTo)(_ => SchemaCreatedResponse(stateId, name, Some(s), None))
            case Failure(exception) =>
              Effect.reply(replyTo)(
                SchemaCreatedResponse(stateId, name, None, Some(exception.toString))
              )
          }

        case ValidateSchema(_, document, replyTo) =>
          // cleans up and validates a document over a schema
          val report = Try {
            val documentLoaded: JsonNode = JsonLoader.fromPath(document)
            val schemaLoaded: JsonNode = JsonLoader.fromString(state.schema)

            val factory = JsonSchemaFactory
              .newBuilder()
              .setLoadingConfiguration(builder.freeze())
              .freeze()

            val schema: JsonSchema = factory.getJsonSchema(schemaLoaded)
            schema.validate(Utils.removeNullFields(documentLoaded))
          }.map(e => if (e.isSuccess) "success" else e.toString)

          Effect.reply(replyTo)(SchemaValidatedResponse(report))

        case GetSchema(name, replyTo) =>
          Effect.reply(replyTo)(
            GetSchemaResponse(schema = if (state.name == name) Some(state.schema) else None)
          )
      }

  val eventHandler: (SchemaRegistry, Event) => SchemaRegistry = (_, event) =>
    event match {
      case SchemaCreated(registry)   => registry
      case SchemaGot(registry)       => registry
      case SchemaValidated(registry) => registry
    }

  // message - event - state
  def apply(id: String): Behavior[Command] = EventSourcedBehavior[Command, Event, SchemaRegistry](
    persistenceId = PersistenceId.ofUniqueId(id),
    emptyState = SchemaRegistry(id, "", ""), // unused
    commandHandler = commandHandler,
    eventHandler = eventHandler
  )
}
