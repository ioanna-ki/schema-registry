package com.snowplowanalytics.snowplow.utils

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import org.scalatest.matchers.should.Matchers
import org.scalatest.GivenWhenThen
import org.scalatest.funspec.AnyFunSpecLike

class UtilsTest extends Matchers with GivenWhenThen with AnyFunSpecLike {

  describe("A Utils.removeNullFields method") {
    it("should remove null fields from a JSON string") {
      Given("a JsonNode with some null fields")
      val json: String =
        """
          |{
          |  "source": "/home/alice/image.iso",
          |  "destination": "/mnt/storage",
          |  "timeout": null,
          |  "chunks": {
          |    "size": null,
          |    "number": null
          |  }
          |}
          |""".stripMargin

      val jsonNode: JsonNode = JsonLoader.fromString(json)

      When("we call removeNullFields")
      val cleanJson: JsonNode = Utils.removeNullFields(jsonNode)

      Then("the result should not contain any nulls")
      cleanJson.has("timeout") shouldBe false
      cleanJson.has("size") shouldBe false
      cleanJson.has("number") shouldBe false
      cleanJson.get("chunks") shouldBe empty
    }
  }
}
