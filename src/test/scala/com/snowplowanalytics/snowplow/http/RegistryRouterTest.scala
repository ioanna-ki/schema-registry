package com.snowplowanalytics.snowplow.http

import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.persistence.testkit.PersistenceTestKitPlugin
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.snowplowanalytics.snowplow.actors.PersistentSchema
import com.snowplowanalytics.snowplow.models.{Command, Event, Response}
import com.snowplowanalytics.snowplow.models.Event.SchemaRegistry
import com.snowplowanalytics.snowplow.models.Response.{
  GetSchemaResponse,
  SchemaCreatedResponse,
  SchemaValidatedResponse
}
import com.typesafe.config.ConfigFactory
import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpecLike

import scala.util.{Failure, Success}

object TestSpec {

  val config =
    ConfigFactory.parseString("""
    akka.actor.allow-java-serialization = on,
    akka.test.single-expect-default = 10s
    """).withFallback(PersistenceTestKitPlugin.config)
}

class RegistryRouterTest
  extends ScalaTestWithActorTestKit(TestSpec.config)
  with AnyFunSpecLike
  with GivenWhenThen
  with LogCapturing {

  private val eventSourcedTestKit =
    EventSourcedBehaviorTestKit[Command, Event, SchemaRegistry](
      system,
      PersistentSchema("config-schema")
    )

  describe("A Schema Registry") {
    it("should create a schema from a specified location") {
      Given("a create command with a path to a json file")
      val createCommand =
        Command.CreateSchema("config-schema", "./src/test/resources/schema-config.json", _)

      When("we run createCommand")
      val result = eventSourcedTestKit.runCommand[Response](createCommand)

      Then("we should get the expected response")
      val expectedResponse = SchemaCreatedResponse(
        "config-schema",
        "config-schema",
        Some("""{
               |  "properties" : {
               |    "source" : {
               |      "type" : "string"
               |    },
               |    "destination" : {
               |      "type" : "string"
               |    }
               |  },
               |  "required" : [ "source", "destination" ]
               |}""".stripMargin),
        None
      )

      result.reply shouldBe expectedResponse
    }

    it("should respond with a failure when the schema is not uploaded") {
      Given("a create command with a path to a json file")
      val createCommand =
        Command.CreateSchema("new_schema", "not_existing_path", _)

      When("we run createCommand")
      val result = eventSourcedTestKit.runCommand[Response](createCommand)

      Then("we should get the expected response")
      val expectedResponse = SchemaCreatedResponse(
        "new_schema",
        "new_schema",
        None,
        Some("java.io.FileNotFoundException: not_existing_path (No such file or directory)")
      )

      result.reply shouldBe expectedResponse
    }

    it("should get a schema given its name") {
      Given("a create command with a path to a json file")
      val getCommand = Command.GetSchema("config-schema", _)

      When("we run getCommand")
      val result = eventSourcedTestKit.runCommand[Response](getCommand)

      Then("we should get the expected response")
      result.reply shouldBe GetSchemaResponse(Some("""{
                                                     |  "properties" : {
                                                     |    "source" : {
                                                     |      "type" : "string"
                                                     |    },
                                                     |    "destination" : {
                                                     |      "type" : "string"
                                                     |    }
                                                     |  },
                                                     |  "required" : [ "source", "destination" ]
                                                     |}""".stripMargin))
    }

    it("should get an empty response when the schema does not exist") {
      Given("a create command with a path to a json file")
      val getCommand = Command.GetSchema("not-existing-schema", _)

      When("we run getCommand")
      val result = eventSourcedTestKit.runCommand[Response](getCommand)

      Then("we should get an empty response")
      result.reply shouldBe GetSchemaResponse(None)
    }

    it("should validate a document over a schema") {
      Given("a validate command with a path to a json file")
      val validateCommand =
        Command.ValidateSchema("config-schema", "./src/test/resources/config.json", _)

      When("we run validateCommand")
      val result = eventSourcedTestKit.runCommand[Response](validateCommand)

      Then("we should get the expected response")
      result.reply shouldBe SchemaValidatedResponse(Success("success"))
    }

    it("should produce a report with the errors of the validation") {
      Given("a validate command with a path to a json file")
      val validateCommand =
        Command.ValidateSchema("config-schema", "./src/test/resources/malformed-config.json", _)

      When("we run validateCommand")
      val result = eventSourcedTestKit.runCommand[Response](validateCommand)

      Then("we should get the expected response")
      result.reply shouldBe SchemaValidatedResponse(
        Success("""com.github.fge.jsonschema.core.report.ListProcessingReport: failure
                  |--- BEGIN MESSAGES ---
                  |error: object has missing required properties (["destination"])
                  |    level: "error"
                  |    schema: {"loadingURI":"#","pointer":""}
                  |    instance: {"pointer":""}
                  |    domain: "validation"
                  |    keyword: "required"
                  |    required: ["destination","source"]
                  |    missing: ["destination"]
                  |---  END MESSAGES  ---
                  |""".stripMargin)
      )
    }

    it("should respond with a failure when file cannot be validated") {
      Given("a validate command with a not existing path to a json file")
      val validateCommand =
        Command.ValidateSchema("config-schema", "not_existing_path", _)

      When("we run validateCommand")
      val result = eventSourcedTestKit.runCommand[Response](validateCommand)

      Then("we should get a Failure in the response")
      result.reply match {
        case SchemaValidatedResponse(Failure(ex)) =>
          ex.toString shouldBe "java.io.FileNotFoundException: not_existing_path (No such file or directory)"
      }
    }

  }
}
