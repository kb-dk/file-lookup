# file-lookup

Simple webservice for locating files. 

## Overview 

Scans a given number of file paths recursively at regular intervals and provides fast
lookup of file paths from file names.

Currently the implementation is limited to 2*10^9 (2 billion) files.

***Note:*** The service operates under the requirement that all tracked filenames are unique. 

## Requirements

 * Java 11, Maven 3
 * Tomcat 9 for production deployment

## Development notes

Update webservice functionality by editing `src/main/openapi/openapi.yaml`.

Start a Jetty web server with the application:
```
mvn jetty:run
```

The default port is 8080 and ping can be accessed at
[http://localhost:8080/file-lookup/ping](http://localhost:8080/file-lookup/ping)

The Swagger-UI is available at http://localhost:8080/file-lookup/api-docs?url=openapi.json

## About OpenAPI 1.3

[OpenAPI 1.3](https://swagger.io/specification/) generates interfaces and skeleton code for webservices.
It also generates online documentation, which includes sample calls and easy testing of the endpoints.

Everything is defined centrally in the file [src/main/openapi/openapi.yaml](src/main/openapi/openapi.yaml).

The interfaces and models generated from the OpenAPI definition are stored in `target/generated-sources/`.
They are recreated on each `mvn package`.

Skeleton classes are added to `dk/kb/lookup/api/impl/` if they are not already present (there is no overwriting).
