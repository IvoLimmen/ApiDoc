package org.limmen.apidoc;

import java.io.PrintStream;
import java.util.Arrays;

public class AsciiDoc {

  public static String link(String link) {
    return "<<" + link + ">>";
  }

  public static String monospaced(String text) {
    return "`" + text + "`";
  }

  public static String bold(String text) {
    return "*" + text + "*";
  }

  public static String italic(String text) {
    return "_" + text + "_";
  }

  public static String subscript(String text) {
    return "~" + text + "~";
  }

  public static String superscript(String text) {
    return "^" + text + "^";
  }

  public static String refName(String text) {
    var list = text.split("/");
    return list[list.length - 1];
  }

  public static String note(String text) {
    return admonition("note", text);
  }

  public static String tip(String text) {
    return admonition("tip", text);
  }

  public static String important(String text) {
    return admonition("important", text);
  }

  public static String caution(String text) {
    return admonition("caution", text);
  }

  public static String warning(String text) {
    return admonition("warning", text);
  }

  public static String admonition(String admonition, String text) {
    return admonition.toUpperCase() + ": " + text + "\n\n";
  }

  private PrintStream printStream;

  public AsciiDoc(PrintStream printStream) {
    this.printStream = printStream;
  }

  public void section(int deep, String title) {
    this.printStream.print("#".repeat(deep));
    this.printStream.print(" ");
    this.printStream.println(title);
    this.printStream.println();
  }

  public void section1(String title) {
    section(1, title);
  }

  public void section2(String title) {
    section(2, title);
  }

  public void section3(String title) {
    section(3, title);
  }

  public void section4(String title) {
    section(4, title);
  }

  public void section5(String title) {
    section(5, title);
  }

  public void ul(String text) {
    this.printStream.println("* " + text);
  }

  public void ol(String text) {
    this.printStream.println("1. " + text);
  }

  public void exampleBlock(String text) {
    this.printStream.println("[example]");
    this.printStream.println(text);
    this.printStream.println("");
  }

  public void codeBlock(String source, String text) {
    this.printStream.println(String.format("[source,%s]", source));
    this.printStream.println("----");
    this.printStream.println(text);
    this.printStream.println("----");
    this.printStream.println("");
  }

  public void par(String text) {
    this.printStream.println(text);
    this.printStream.println();
  }

  public void tableHeader(String cols, String header) {
    this.printStream.println("[%header,cols='" + cols + "']");
    this.printStream.println("|===");
    Arrays.asList(header.split("\\|")).forEach(h -> {
      if (h != null && h.length() > 0) {
        tableCell(TableCellModifier.HEADER, h);
      }
    });
  }

  public void tableCell(TableCellModifier mod, String cell) {
    this.printStream.println(mod.getValue() + "| " + cell);
  }

  public void tableCell(String cell) {
    this.printStream.println("| " + cell);
  }

  public void tableEndRow() {
    this.printStream.println();
  }

  public void tableEnd() {
    this.printStream.println("|===");
  }
}