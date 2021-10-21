package org.limmen.apidoc;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.TreeSet;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;

public class Main {

  private AsciiDoc adoc;

  public static void main(String[] args) throws IOException {

    var swaggerFile = Path.of(args[0]);

    new Main(swaggerFile);
  }

  public Main(Path swaggerFile) throws IOException {

    var objectMapper = Json.mapper();

    var openApi = objectMapper.readValue(swaggerFile.toFile(), OpenAPI.class);

    try (var printStream = new PrintStream(Path.of(System.getProperty("user.home"), "output.adoc").toFile())) {

      adoc = new AsciiDoc(printStream);

      handleInfo(openApi);
      handlePaths(openApi);
      handleComponent(openApi.getComponents());
    }
  }

  private void handleInfo(OpenAPI openApi) {
    adoc.section1(openApi.getInfo().getTitle());
    adoc.par(openApi.getInfo().getDescription());
    adoc.par("Version " + openApi.getInfo().getVersion());
  }

  private void handlePaths(OpenAPI openApi) {

    var sortedSet = new TreeSet<String>(openApi.getPaths().keySet());

    adoc.section2("Paths");

    sortedSet.forEach(key -> {
      this.handlePath(key, openApi.getPaths().get(key));
    });
  }

  private void handleComponent(Components components) {
    if (components == null) {
      return;
    }

    adoc.section2("Models");

    var sortedSchemas = new TreeSet<>(components.getSchemas().keySet());

    sortedSchemas.forEach(key -> {
      handleModel(key, components.getSchemas().get(key));
    });
  }

  private void handleModel(String name, Schema<?> schema) {
    adoc.section3(name);

    // type
    adoc.par("Type of " + AsciiDoc.italic(schema.getType()));    

    // description
    if (schema.getDescription() != null && schema.getDescription().length() > 0) {
      adoc.section4("Description");
      adoc.par(schema.getDescription());
    }    

    adoc.section4("Properties");
    adoc.tableHeader("1,1,1,2,2", "|Name|Type|Format|Description|Example");
    schema.getProperties().entrySet().forEach(entry -> {
      adoc.tableCell(entry.getKey());
      var value = entry.getValue();
      adoc.tableCell(value.getType());
      adoc.tableCell(value.getFormat() == null ? "" : value.getFormat());
      StringBuilder desc = new StringBuilder();
      if (value.getDescription() != null) {
        desc.append(value.getDescription());
      }
      if (value.getDefault() != null) {
        if (desc.length() > 0) {
          desc.append("\n");
        }
        desc.append("Default: ");
        desc.append(value.getDefault());
      }
      adoc.tableCell(desc.toString());
      adoc.tableCell(value.getExample() == null ? "" : value.getExample().toString());
      adoc.tableEndRow();  
    });
    adoc.tableEnd();
  }

  private void handlePath(String key, PathItem pathItem) {
    adoc.section3(key);

    handleOperation("delete", key, pathItem.getDelete());
    handleOperation("get", key, pathItem.getGet());
    handleOperation("head", key, pathItem.getHead());
    handleOperation("options", key, pathItem.getOptions());
    handleOperation("patch", key, pathItem.getPatch());
    handleOperation("post", key, pathItem.getPost());
    handleOperation("put", key, pathItem.getPut());
    handleOperation("trace", key, pathItem.getTrace());
  }

  private void handleOperation(String method, String path, Operation operation) {
    if (operation == null) {
      return;
    }

    // original call
    adoc.codeBlock("shell", method.toUpperCase() + " " + path);

    // description
    if (operation.getDescription() != null && operation.getDescription().length() > 0) {
      adoc.section4("Description");
      adoc.par(operation.getDescription());
    }

    // requestBody
    handleRequestBody(operation.getRequestBody());

    // parameters
    handleParameters(operation.getParameters());

    // responses
    handleResponses(operation.getResponses());
  }

  private void handleResponses(ApiResponses responses) {
    if (responses == null || responses.isEmpty()) {
      return;
    }

    adoc.section4("Responses");
    var responseCodes = new TreeSet<>(responses.keySet());

    adoc.tableHeader("1,2,2", "|Response code|Description|Content");
    responseCodes.forEach(responseCode -> {
      adoc.tableCell(responseCode);
      var response = responses.get(responseCode);
      adoc.tableCell(response.getDescription());
      StringBuilder value = new StringBuilder();
      if (response.getContent() != null) {
        response.getContent().forEach((key, entry) -> {
          value.append(AsciiDoc.subscript(key));
          value.append("\n");
          value.append(getSchemaValue(entry.getSchema()));
        });
      }
      adoc.tableCell(value.toString());
      adoc.tableEndRow();
    });
    adoc.tableEnd();
  }

  private void handleRequestBody(RequestBody requestBody) {
    if (requestBody == null) {
      return;
    }

    adoc.section4("Request body");
    if (requestBody.getDescription() != null && requestBody.getDescription().length() > 0) {
      adoc.par(requestBody.getDescription());
    }

    var content = requestBody.getContent().keySet();

    // adoc.par("See " + getSchemaValue(content.getSchema()));
  }

  private void handleParameters(List<Parameter> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return;
    }

    adoc.section4("Parameters");

    adoc.tableHeader("1,2,3,1,1", "|Type|Name|Description|Schema|Default");
    parameters.forEach(parameter -> {
      adoc.tableCell(parameter.getIn());
      adoc.tableCell(parameter.getName());

      var description = "";
      if (parameter.getDescription() != null && parameter.getDescription().length() > 0) {
        description += parameter.getDescription();
      }
      if (parameter.getExample() != null) {
        description += "For example:\n";
        description += AsciiDoc.monospaced(parameter.getExample().toString());
        description += "\n";
      }

      adoc.tableCell(description);
      adoc.tableCell(getSchemaValue(parameter.getSchema()));
      adoc.tableCell("");
      adoc.tableEndRow();
    });
    adoc.tableEnd();
  }

  private String getSchemaValue(Schema<?> schema) {
    var value = "";

    // do we have a type with a schema reference?
    if (schema.getType() != null) {
      if (schema.get$ref() != null) {
        value = AsciiDoc.link(AsciiDoc.refName(schema.get$ref())) + " " + schema.getType();
      } else {
        value = schema.getType();
      }
    } else {
      // do we have a schema reference?
      if (schema.get$ref() != null) {
        value = AsciiDoc.link(AsciiDoc.refName(schema.get$ref()));
      }
    }

    return value;
  }
}
