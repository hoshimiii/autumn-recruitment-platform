package dev.recruitops.infrastructure;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Locale;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Component;

@Component
public class DatasetNamespace {
  private final String value;
  public DatasetNamespace(DataSourceProperties properties) {
    String url=properties.getUrl();
    if(url==null || !url.startsWith("jdbc:sqlite:")) throw new IllegalArgumentException("SQLite dataset URL is required");
    String identity;
    if(url.contains(":memory:") || url.contains("mode=memory")) identity=url+":"+ProcessHandle.current().pid();
    else {
      String file=url.substring("jdbc:sqlite:".length()).replaceFirst("^file:","").split("\\?",2)[0];
      Path path=Path.of(file).toAbsolutePath().normalize();
      try { path=path.toRealPath(); } catch(java.io.IOException notYetCreated) { }
      identity=path.toString();
      if(System.getProperty("os.name").startsWith("Windows")) identity=identity.toLowerCase(Locale.ROOT);
    }
    try { value=java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8))).substring(0,24); }
    catch(java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
  }
  public String value() { return value; }
}
