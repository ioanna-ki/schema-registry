package com.snowplowanalytics.snowplow.models

import akka.actor.typed.ActorRef
import com.snowplowanalytics.snowplow.models.Command._

/**
  * Holds the JSON payloads of these requests
  * A request can be:
  *  - a create registry request
  *  - a schema validate request
  *  - a get request
  */
sealed trait Request

object Request {

  case class RegistryCreationRequest(schema: String) extends Request {

    def toCommand(name: String, replyTo: ActorRef[Response]): Command =
      CreateSchema(name, schema, replyTo)
  }

  case class SchemaValidateRequest(document: String) extends Request {

    def toCommand(id: String, replyTo: ActorRef[Response]): Command =
      ValidateSchema(id, document, replyTo)
  }

  case class SchemaGetRequest(name: String) extends Request {

    def toCommand(schema: String, replyTo: ActorRef[Response]): Command =
      GetSchema(schema, replyTo)
  }
}
