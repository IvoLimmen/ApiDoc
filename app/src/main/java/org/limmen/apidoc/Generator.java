package org.limmen.apidoc;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.limmen.apidoc.asciidoc.AsciiDoc;
import org.limmen.apidoc.asciidoc.TableCellModifier;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponses;

public class Generator {

  private AsciiDoc adoc;

  private List<Path> generated = new ArrayList<>();

  private Path outputDir;

  private boolean book;

  public Generator(boolean book) {
    this.book = book;
  }

  public void generate(Path inputFile, Path outputDir) throws IOException {

    this.outputDir = outputDir;
    var fileName = inputFile.getFileName().toString();
    fileName = fileName.substring(0, fileName.indexOf("."));
    var outputFile = Path.of(outputDir.toString(), fileName + ".adoc");

    var objectMapper = Json.mapper();
    var openApi = objectMapper.readValue(inputFile.toFile(), OpenAPI.class);

    try (var printStream = new PrintStream(outputFile.toFile())) {

      adoc = new AsciiDoc(printStream, book ? 1 : 0);

      handleInfo(openApi.getInfo());
      handlePathsSorted(openApi);
      handleComponentSorted(openApi.getComponents());
    }

    generated.add(Path.of(fileName + ".adoc"));
  }

  public void createBook() throws IOException {

    // cretate index here
    var outputFile = Path.of(outputDir.toString(), "index.adoc");
    try (var printStream = new PrintStream(outputFile.toFile())) {

      adoc = new AsciiDoc(printStream, 0);

      for (Path g : generated) {
        adoc.include(g.toString());
      }
    }
  }

  private void handleInfo(Info info) {
    adoc.section1(info.getTitle());
    if (info.getDescription() != null && info.getDescription().length() > 0) {
      adoc.par(info.getDescription());
    }
    adoc.par("Version " + info.getVersion());
    if (info.getTermsOfService() != null && info.getTermsOfService().length() > 0) {
      adoc.par(info.getTermsOfService());
    }
    if (info.getContact() != null) {
      adoc.section2("Contact information");
      if (info.getContact().getName() != null && info.getContact().getName().length() > 0) {
        adoc.par("Name: " + info.getContact().getName());
      }
      if (info.getContact().getEmail() != null && info.getContact().getEmail().length() > 0) {
        adoc.par("Email: " + info.getContact().getEmail());
      }
      if (info.getContact().getUrl() != null && info.getContact().getUrl().length() > 0) {
        adoc.par("Url: " + info.getContact().getUrl());
      }
    }
  }

  private void handlePathsSorted(OpenAPI openApi) {

    var sortedSet = new TreeSet<String>(openApi.getPaths().keySet());

    adoc.section2("Paths");

    sortedSet.forEach(key -> {
      this.handlePath(key, openApi.getPaths().get(key));
    });
  }

  private void handleComponentSorted(Components components) {
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

    adoc.section3(path);
    adoc.par(AsciiDoc.italic("(" + operation.getOperationId() + ")"));

    // original call
    adoc.codeBlock("shell", method.toUpperCase() + " " + path);

    // deprecated?
    if (operation.getDeprecated() != null && operation.getDeprecated().booleanValue()) {
      adoc.par(AsciiDoc.warning("This API is deprecated!"));
    }

    // description
    if (operation.getDescription() != null && operation.getDescription().length() > 0) {
      adoc.section4("Description");
      adoc.par(operation.getDescription());
    }
    // summary
    if (operation.getSummary() != null && operation.getSummary().length() > 0) {
      adoc.section4("Summary");
      adoc.par(operation.getSummary());
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
      if (response.getContent() != null && !response.getContent().isEmpty()) {
        adoc.tableCellStarter(TableCellModifier.ASCIIDOC);
        handleContent(response.getContent());
      } else {
        adoc.tableCell("");
      }
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
    if (requestBody.getRequired() != null && requestBody.getRequired().booleanValue()) {
      adoc.par("This is a required request body");
    }

    handleContent(requestBody.getContent());
  }

  private void handleContent(Content content) {
    if (content == null || content.isEmpty()) {
      return;
    }

    var mediaTypes = content.keySet();

    mediaTypes.forEach(mediaType -> {
      adoc.ul(mediaType + ": " + getSchemaRefValue(content.get(mediaType).getSchema()));
    });
    adoc.eol();
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
      adoc.tableCell(getSchemaRefValue(parameter.getSchema()));
      adoc.tableCell("");
      adoc.tableEndRow();
    });
    adoc.tableEnd();
  }

  private String getSchemaRefValue(Schema<?> schema) {
    if (schema == null) {
      return "";
    }

    // array of a certain type?
    if ("array".equals(schema.getType())) {
      return getArraySchemaRefValue((ArraySchema) schema);
    } else {
      // do we have a schema reference?
      if (schema.get$ref() != null) {
        return AsciiDoc.link(AsciiDoc.refName(schema.get$ref()));
      }
    }

    return schema.getType();
  }

  private String getArraySchemaRefValue(ArraySchema schema) {
    if (schema.getItems() != null && schema.getItems().get$ref() != null) {
      return "Array of " + AsciiDoc.link(AsciiDoc.refName(schema.getItems().get$ref()));
    } else {
      return schema.getType();
    }
  }
}
