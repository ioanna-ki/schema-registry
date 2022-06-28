package com.snowplowanalytics.snowplow.models

import akka.actor.typed.ActorRef

sealed trait Command

/**
  * The messages that an Actor may receive.
  */
object Command {

  case class CreateSchema(name: String, schema: String, replyTo: ActorRef[Response]) extends Command

  case class ValidateSchema(id: String, document: String, replyTo: ActorRef[Response])
    extends Command

  case class GetSchema(name: String, replyTo: ActorRef[Response]) extends Command
}
