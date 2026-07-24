package dev.recruitops.application;

public class CollectionException extends RuntimeException {
  private final String status;

  private CollectionException(String status, String message, Throwable cause) {
    super(message, cause);
    this.status = status;
  }

  public String status() { return status; }

  public static CollectionException blocked(String message) {
    return new CollectionException("BLOCKED", message, null);
  }

  public static CollectionException failed(String message) {
    return new CollectionException("FAILED", message, null);
  }

  public static CollectionException failed(String message, Throwable cause) {
    return new CollectionException("FAILED", message, cause);
  }

  public static CollectionException parseFailed(String message, Throwable cause) {
    return new CollectionException("PARSE_FAILED", message, cause);
  }
}
