package com.snowplowanalytics.snowplow.app

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.Http
import akka.util.Timeout
import com.snowplowanalytics.snowplow.actors.Registry
import com.snowplowanalytics.snowplow.http.RegistryRouter
import com.snowplowanalytics.snowplow.models.Command

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Starts the server and binds the routes under localhost:7777
  */
object RegistryApp {

  trait RouteCommand

  case class RetrieveSchemaActor(replyTo: ActorRef[ActorRef[Command]]) extends RouteCommand

  def startHttpServer(registry: ActorRef[Command])(implicit system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    val router = new RegistryRouter(registry)
    val routes = router.routes
    val httpBindingFuture = Http().newServerAt("localhost", 7777).bind(routes)
    httpBindingFuture.onComplete {
      case Success(binding) =>
        system.log.info(
          s"Server online at http://${binding.localAddress.getHostString}:${binding.localAddress.getPort}"
        )
      case Failure(exception) =>
        system.log.error(s"Failed to bind HTTP server, because $exception")
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val rootBehavior: Behavior[RouteCommand] = Behaviors.setup { context =>
      val registryActor = context.spawn(Registry(), "registry")

      Behaviors.receiveMessage {
        case RetrieveSchemaActor(replyTo) =>
          replyTo ! registryActor
          Behaviors.same
      }
    }

    implicit val system: ActorSystem[RouteCommand] = ActorSystem(rootBehavior, "registrySystem")
    import scala.concurrent.duration._
    implicit val ec: ExecutionContext = system.executionContext
    implicit val timeout: Timeout = Timeout(15.seconds)
    val registryActorFuture: Future[ActorRef[Command]] =
      system.ask(replyTo => RetrieveSchemaActor(replyTo))
    registryActorFuture.foreach(startHttpServer)
  }

}
