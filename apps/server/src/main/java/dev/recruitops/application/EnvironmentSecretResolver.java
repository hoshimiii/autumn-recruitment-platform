package dev.recruitops.application;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentSecretResolver {
  public Optional<String> resolve(String name) {
    return Optional.ofNullable(System.getenv(name)).filter(value -> !value.isBlank());
  }
}
