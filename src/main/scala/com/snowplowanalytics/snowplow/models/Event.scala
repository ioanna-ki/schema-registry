package com.snowplowanalytics.snowplow.models

/**
  * Can be persist the state to Cassandra.
  * In case of failure the server can start from its previous state.
  */
trait Event

object Event {
  case class SchemaCreated(state: SchemaRegistry) extends Event

  case class SchemaGot(state: SchemaRegistry) extends Event

  case class SchemaValidated(state: SchemaRegistry) extends Event

  case class SchemaRegistry(id: String, name: String, schema: String)

}
