package org.limmen.apidoc.asciidoc;

public enum TableCellModifier {
  
  ASCIIDOC("a"),
  STRONG("s"),
  LITERAL("l"),
  MONOSPACE("m"),
  DEFAULT("d"),
  EMPHASIS("e"),
  HEADER("h"),
  ;

  private String value;

  TableCellModifier(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
