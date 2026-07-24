package dev.recruitops.infrastructure;

import java.net.URI;

public record FetchResult(
    int status, URI uri, byte[] body, String contentType, String etag, String lastModified) {}
