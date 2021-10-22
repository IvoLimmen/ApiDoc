# ApiDoc

I was missing a simple tool that can convert an OpenAPI 3 JSON file to a nice looking document. Since I like AsciiDoc I decided to output it to that format.

## Requirements

Minimal requirements:

* Java 11 or higher
* Maven 3.8.* or higher

## Building the application

Run:

    mvn clean install

This will create an executable JAR in `app/target/apidoc-app-0.1.jar`

## Running the application

After compiling you can run:

    java -jar app/target/apidoc-app-0.1.jar