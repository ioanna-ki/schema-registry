package com.snowplowanalytics.snowplow.utils

import com.fasterxml.jackson.databind.JsonNode

import org.json4s._
import org.json4s.jackson.JsonMethods._

object Utils {

  /**
    * Removes null fields from a jsonNode and its children
    * jackson.parseJson is safe as the node already contain a valid json structure.
    * @param json a JsonNode, that may contain nulls
    * @return a JsonNode without nulls
    */
  def removeNullFields(json: JsonNode): JsonNode = {
    val withNulls: JValue = jackson.parseJson(json.toPrettyString)
    asJsonNode(withNulls.noNulls)
  }
}
