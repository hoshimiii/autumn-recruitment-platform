package dev.recruitops.config;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.support.JdbcTransactionManager;

@Configuration
public class SqliteConfig {
  @Bean public DataSource dataSource(DataSourceProperties properties) {
    ensureSqliteDirectory(properties.getUrl());
    HikariDataSource source = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    source.setMaximumPoolSize(4); source.setMinimumIdle(1);
    source.addDataSourceProperty("foreign_keys", "true");
    source.addDataSourceProperty("busy_timeout", "5000");
    String url = properties.getUrl();
    if (url != null && !url.contains(":memory:") && !url.contains("mode=memory")) {
      source.addDataSourceProperty("journal_mode", "WAL");
    }
    return source;
  }
  @Bean JdbcTemplate jdbcTemplate(DataSource dataSource) { return new JdbcTemplate(dataSource); }
  @Bean PlatformTransactionManager transactionManager(DataSource dataSource) {
    return new JdbcTransactionManager(dataSource);
  }
  private void ensureSqliteDirectory(String url) {
    if (url == null || !url.startsWith("jdbc:sqlite:") || url.contains(":memory:") || url.startsWith("jdbc:sqlite:file:")) return;
    try { Path parent = Path.of(url.substring("jdbc:sqlite:".length())).toAbsolutePath().getParent(); if (parent != null) Files.createDirectories(parent); }
    catch (Exception error) { throw new IllegalStateException("Cannot create SQLite data directory", error); }
  }
}
