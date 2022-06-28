## Schema Registry

#### A REST-service for validating JSON documents against JSON Schemas


### Contributor
Ioanna Kiriakidou @ iikiriakidou@gmail.com

### How to:

- First lets start a Cassandra cluster using the `docker-compose.yml` in a terminal

    ```
    $ docker-compose up
    
    $ docker ps
    
    $ docker exec -it schema-registry_cassandra_1 cqlsh
    ```

    This will start a shell to query cassandra.
- Now, lets start the server (at `localhost:7777`)
  ```
  $ sbt run
  ```

- curl examples:
  ```
  $ curl http://localhost:7777/registry/schema-config -X POST -d '{"schema": "./src/main/resources/schema-config.json"}' -H "Content-Type: application/json"
  $ curl http://localhost:7777/registry/schema-config
  $ curl http://localhost:7777/validate/schema-config -X POST -d '{"document": "./src/main/resources/config.json"}' -H 'Content-Type: application/json'
  ```
  

- if we restart the server it will retrieve its state from cassandra, this way the uploaded schemas will be fetched.

### notes:

To query Cassandra
```
$ cqlsh> select * from akka.messages ;
$ cqlsh> truncate table akka.messages ;
```

truncate will delete the state in case we want to start from scratch.
