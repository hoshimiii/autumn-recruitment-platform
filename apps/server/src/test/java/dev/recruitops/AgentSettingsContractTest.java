package dev.recruitops;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpServer;
import dev.recruitops.application.EnvironmentSecretResolver;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AgentSettingsContractTest {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @MockBean EnvironmentSecretResolver secrets;
  private HttpServer server;

  @BeforeEach
  void startServer() throws Exception {
    jdbc.update("""
        UPDATE agent_settings SET enabled=0,base_url='http://127.0.0.1:18080/v1',
        secret_env_ref='SUB2API_API_KEY',scan_model='gpt-5.6-terra',scan_reasoning_effort='high',
        process_model='gpt-5.3-codex-spark',process_reasoning_effort='medium',timeout_seconds=60,
        config_version=1
        """);
    server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
    server.start();
  }

  @AfterEach
  void stopServer() {
    if (server != null) server.stop(0);
  }

  @Test
  void returnsDefaultsWithoutDisclosingAKey() throws Exception {
    mvc.perform(get("/api/v1/agent-settings"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("enabled").value(false))
        .andExpect(jsonPath("baseUrl").value("http://127.0.0.1:18080/v1"))
        .andExpect(jsonPath("secretEnvRef").value("SUB2API_API_KEY"))
        .andExpect(jsonPath("secretConfigured").value(false))
        .andExpect(jsonPath("scanModel").value("gpt-5.6-terra"))
        .andExpect(jsonPath("scanReasoningEffort").value("high"))
        .andExpect(jsonPath("processModel").value("gpt-5.3-codex-spark"))
        .andExpect(jsonPath("configVersion").value(1))
        .andExpect(jsonPath("secretValue").doesNotExist());
  }

  @Test
  void updatesOnlyValidLocalSettingsAndIncrementsVersion() throws Exception {
    String body = """
        {"enabled":true,"baseUrl":"http://localhost:18080/v1/","secretEnvRef":"LOCAL_KEY",
        "scanModel":"scan","scanReasoningEffort":"xhigh","processModel":"process",
        "processReasoningEffort":"low","timeoutSeconds":30}
        """;
    mvc.perform(put("/api/v1/agent-settings").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("baseUrl").value("http://localhost:18080/v1"))
        .andExpect(jsonPath("configVersion").value(2));
    mvc.perform(put("/api/v1/agent-settings").contentType(MediaType.APPLICATION_JSON)
        .content(body.replace("LOCAL_KEY", "bad-key")))
        .andExpect(status().isBadRequest());
    mvc.perform(put("/api/v1/agent-settings").contentType(MediaType.APPLICATION_JSON)
        .content(body.replace("http://localhost:18080/v1/", "http://example.com/v1")))
        .andExpect(status().isBadRequest());
    mvc.perform(put("/api/v1/agent-settings").contentType(MediaType.APPLICATION_JSON)
        .content(body.replace("http://localhost:18080/v1/", "ftp://localhost:18080/v1")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsTamperedSettingsBeforeResolvingTheSecret() throws Exception {
    jdbc.update("UPDATE agent_settings SET enabled=1,base_url='https://example.com/v1',secret_env_ref='LOCAL_KEY'");

    mvc.perform(post("/api/v1/agent-settings/check"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("connected").value(false))
        .andExpect(jsonPath("secretConfigured").value(false))
        .andExpect(jsonPath("models", hasSize(0)))
        .andExpect(jsonPath("message").value("Settings are invalid"));

    verifyNoInteractions(secrets);
  }

  @Test
  void checksOnlyTheModelCatalogWithAuthorizationAndNoSecretDisclosure() throws Exception {
    AtomicReference<String> authorization = new AtomicReference<>();
    server.createContext("/v1/models", exchange -> {
      authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
      byte[] body = "{\"data\":[{\"id\":\"gpt-5.6-terra\"},{\"id\":\"gpt-5.3-codex-spark\"},{\"id\":\"gpt-5.6-terra\"}]}"
          .getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    String body = "{\"enabled\":false,\"baseUrl\":\"" + baseUrl + "\",\"secretEnvRef\":\"LOCAL_KEY\","
        + "\"scanModel\":\"gpt-5.6-terra\",\"scanReasoningEffort\":\"high\","
        + "\"processModel\":\"gpt-5.3-codex-spark\",\"processReasoningEffort\":\"medium\",\"timeoutSeconds\":5}";
    when(secrets.resolve("LOCAL_KEY")).thenReturn(Optional.of("not-returned"));
    mvc.perform(put("/api/v1/agent-settings").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk()).andExpect(jsonPath("secretConfigured").value(true))
        .andExpect(jsonPath("secretValue").doesNotExist());
    mvc.perform(post("/api/v1/agent-settings/check"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("connected").value(true))
        .andExpect(jsonPath("secretConfigured").value(true))
        .andExpect(jsonPath("models", hasSize(2)))
        .andExpect(jsonPath("models", containsInAnyOrder("gpt-5.3-codex-spark", "gpt-5.6-terra")))
        .andExpect(jsonPath("scanModelAvailable").value(true))
        .andExpect(jsonPath("processModelAvailable").value(true))
        .andExpect(jsonPath("message").value("Model catalog available"))
        .andExpect(jsonPath("secretValue").doesNotExist());
    org.junit.jupiter.api.Assertions.assertEquals("Bearer not-returned", authorization.get());
  }

  @Test
  void checkFailsClosedWhenTheSecretIsMissing() throws Exception {
    mvc.perform(post("/api/v1/agent-settings/check"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("connected").value(false))
        .andExpect(jsonPath("secretConfigured").value(false))
        .andExpect(jsonPath("models", hasSize(0)))
        .andExpect(jsonPath("message").value("Secret is not configured"));
  }
}
