package com.snowplowanalytics.snowplow.models

import scala.util.Try

/**
  * The available responses that will be parsed as JSON payloads
  * A response can be:
  *  - a response when uploading a schema. Field `message` will contain a value if an error occur
  *  - a response with a report produced when validating a schema
  *  - a get schema response that will return None if the schema could not be fetched
  *  - a successful response
  *  - a successful response that also holds a schema
  *  - a failure response
  */
sealed trait Response

object Response {

  case class SchemaCreatedResponse(
    id: String,
    name: String,
    schema: Option[String],
    message: Option[String])
    extends Response

  case class SchemaValidatedResponse(tryReport: Try[String]) extends Response

  case class GetSchemaResponse(schema: Option[String]) extends Response

  case class SuccessResponse(action: String, id: String, status: String) extends Response

  case class SuccessResponseWithSchema(action: String, id: String, status: String, schema: String)
    extends Response

  case class FailureResponse(action: String, id: String, status: String, message: String)
    extends Response

}
