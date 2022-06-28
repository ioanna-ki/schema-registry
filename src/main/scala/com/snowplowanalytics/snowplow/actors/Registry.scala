package com.snowplowanalytics.snowplow.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.ActorContext
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.persistence.typed.PersistenceId
import com.snowplowanalytics.snowplow.models.{Command, Response}

import java.io.IOException
import scala.util.Failure

object Registry {

  import Command._
  import Response._

  sealed trait Event
  case class RegistryCreated(id: String) extends Event

  case class State(schemas: Map[String, ActorRef[Command]])

  def commandHandler(ctx: ActorContext[Command]): (State, Command) => Effect[Event, State] =
    (state, command) =>
      command match {
        case createCommand @ CreateSchema(name, _, _) =>
          val id = name
          val newRegistry = ctx.spawn(PersistentSchema(id), id)
          // store state in Cassandra
          Effect
            .persist(RegistryCreated(id))
            .thenReply(newRegistry)(_ => createCommand)
        case validateCommand @ ValidateSchema(id, _, replyTo) =>
          state.schemas.get(id) match {
            case Some(schema) => Effect.reply(schema)(validateCommand)
            case None =>
              Effect.reply(replyTo)(
                SchemaValidatedResponse(Failure(new IOException("Schema does not exist.")))
              )

          }
        case getCommand @ GetSchema(name, replyTo) =>
          state.schemas.get(name) match {
            case Some(schema) => Effect.reply(schema)(getCommand)
            case None         => Effect.reply(replyTo)(GetSchemaResponse(None))
          }
      }

  def eventHandler(ctx: ActorContext[Command]): (State, Event) => State = (state, event) =>
    event match {
      case RegistryCreated(id) =>
        val schema = ctx
          .child(id) // spawn with first command
          .getOrElse(
            ctx.spawn(PersistentSchema(id), id)
          ) // if in the recovery mode spawn a new actor
          .asInstanceOf[ActorRef[Command]]

        state.copy(state.schemas + (id -> schema))
    }

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("registry"),
      emptyState = State(Map()),
      commandHandler = commandHandler(context),
      eventHandler = eventHandler(context)
    )
  }
}
