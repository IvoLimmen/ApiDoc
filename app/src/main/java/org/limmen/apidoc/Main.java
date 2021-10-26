package org.limmen.apidoc;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class Main implements Callable<Integer> {

  @Parameters(arity = "1..*", description = "OpenApi files to convert to AsciiDoc. Can be a file or a URL. On a URL you can specify a file name by adding `|filename.json`")
  private List<String> files;

  @Option(names = { "-o", "--output" }, description = "Output directory to write to")
  private Path outputDir;
  
  @Override
  public Integer call() throws Exception {

    var generator = new Generator();

    if (outputDir == null) {
      outputDir = Path.of(System.getProperty("user.dir"));
    }

    var allFiles = new ArrayList<Path>();

    for (var file : files) {
      if (file != null && file.startsWith("http")) {
        if (file.contains("|")) {
          var index = file.indexOf("|");
          var fileName = file.substring(1 + index);
          allFiles.add(toPath(URI.create(file.substring(0, index)), fileName));
        } else {
          var url = URI.create(file);
          allFiles.add(toPath(url, url.getHost()));
        }
      } else {
        allFiles.add(Path.of(file));
      }
    }

    for (var file : allFiles) {
      generator.generate(file, outputDir);
    }

    return 0;
  }

  public static void main(String[] args) throws Exception {
    System.exit(new CommandLine(new Main()).execute(args));
  }

  private Path toPath(URI url, String fileName) throws Exception {
    var client = HttpClient.newBuilder()
        .build();

    var request = HttpRequest.newBuilder().GET()
        .uri(url)        
        .build();

    var response = client.send(request, BodyHandlers.ofString());

    var name = Path.of(System.getProperty("java.io.tmpdir"), fileName);
    Files.writeString(name, response.body());

    return name;
  }
}
