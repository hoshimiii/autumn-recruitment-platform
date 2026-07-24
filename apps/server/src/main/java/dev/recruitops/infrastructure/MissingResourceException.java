package dev.recruitops.infrastructure;

public class MissingResourceException extends RuntimeException {
  public MissingResourceException(String message) { super(message); }
}
