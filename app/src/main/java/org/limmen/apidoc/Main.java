package org.limmen.apidoc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class Main implements Callable<Integer> {

  @Parameters(paramLabel = "FILE", description = "OpenApi files to convert to AsciiDoc")
  List<Path> files;

  @Option(names = { "-o", "--output" }, description = "Output directory to write to")
  Path outputDir;

  @Override
  public Integer call() throws Exception {

    var generator = new Generator();

    if (outputDir == null) {
      outputDir = Path.of(System.getProperty("user.dir"));
    }

    for (var file : files) {
      generator.generate(file, outputDir);
    }

    return 0;
  }

  public static void main(String[] args) throws IOException {
    System.exit(new CommandLine(new Main()).execute(args));
  }
}
