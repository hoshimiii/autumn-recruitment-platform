package dev.recruitops.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = { "recruitops.security.mode=oidc",
    "recruitops.security.audience=recruitops-api", "spring.datasource.url=jdbc:sqlite:file:security-test?mode=memory&cache=shared" })
@AutoConfigureMockMvc
class OidcSecurityContractTest {
  static HttpServer issuerServer;
  static RSAKey key;
  static String issuer;
  static String loginNonce;
  static {
    try {
      var pair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
      key = new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey((RSAPrivateKey) pair.getPrivate()).keyID("fixture").build();
      issuerServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      issuer = "http://127.0.0.1:" + issuerServer.getAddress().getPort();
      issuerServer.createContext("/", exchange -> {
        String path = exchange.getRequestURI().getPath();
        String json;
        try {
          if (path.contains("well-known")) json = """
              {"issuer":"%s","authorization_endpoint":"%s/authorize","token_endpoint":"%s/token",
              "jwks_uri":"%s/jwks","response_types_supported":["code"],"subject_types_supported":["public"],
              "id_token_signing_alg_values_supported":["RS256"]}
              """.formatted(issuer, issuer, issuer, issuer);
          else if (path.equals("/jwks")) json = new JWKSet(key.toPublicJWK()).toString();
          else if (path.equals("/token")) {
            exchange.getRequestBody().readAllBytes();
            json = "{\"access_token\":\"" + token(issuer, "recruitops-api", "REVIEWER", 300, null)
                + "\",\"token_type\":\"Bearer\",\"expires_in\":300,\"id_token\":\""
                + token(issuer, "recruitops-ui", "REVIEWER", 300, loginNonce) + "\"}";
          } else json = "{}";
          byte[] body = json.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
        } catch (Exception error) { exchange.sendResponseHeaders(500, -1); }
        finally { exchange.close(); }
      });
      issuerServer.start();
    } catch (Exception error) { throw new ExceptionInInitializerError(error); }
  }

  @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
    registry.add("recruitops.security.issuer-uri", () -> issuer);
    registry.add("spring.security.oauth2.client.provider.recruitops.issuer-uri", () -> issuer);
    registry.add("spring.security.oauth2.client.registration.recruitops.provider", () -> "recruitops");
    registry.add("spring.security.oauth2.client.registration.recruitops.client-id", () -> "recruitops-ui");
    registry.add("spring.security.oauth2.client.registration.recruitops.client-secret", () -> "fixture-secret");
    registry.add("spring.security.oauth2.client.registration.recruitops.scope", () -> "openid");
    registry.add("spring.security.oauth2.client.registration.recruitops.authorization-grant-type", () -> "authorization_code");
  }
  @AfterAll static void close() { issuerServer.stop(0); }
  @Autowired MockMvc mvc;
  @Autowired OperationAuthorizer authorizer;
  @Autowired dev.recruitops.application.ManualItemService manualItems;
  @Autowired ObjectMapper json;

  @Test void publicQueriesAndAnonymousSessionAreAvailableButWritesNeedIdentityAndCsrf() throws Exception {
    mvc.perform(get("/api/v1/jobs")).andExpect(status().isOk());
    mvc.perform(get("/api/v1/auth/session")).andExpect(status().isOk())
        .andExpect(jsonPath("mode").value("oidc")).andExpect(jsonPath("authenticated").value(false))
        .andExpect(jsonPath("capabilities").isEmpty()).andExpect(jsonPath("csrfToken").isNotEmpty());
    mvc.perform(post("/api/v1/manual-items").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/sources")).andExpect(status().isUnauthorized());
    assertFalse(authorizer.can("ADMIN"));
    assertThrows(org.springframework.security.authorization.AuthorizationDeniedException.class,
        () -> manualItems.create(null));
  }

  @Test void signedBearerRolesWorkWithoutCsrfAndInvalidTokensFail() throws Exception {
    mvc.perform(get("/api/v1/auth/session").header("Authorization", "Bearer " + token(issuer, "recruitops-api", "COLLECTOR", 300, null)))
        .andExpect(status().isOk()).andExpect(jsonPath("roles[0]").value("COLLECTOR"));
    String input = "{\"platform\":\"fixture\",\"companyName\":\"Acme\",\"requiredAction\":\"Review\"}";
    mvc.perform(post("/api/v1/manual-items").header("Authorization", "Bearer " + token(issuer, "recruitops-api", "COLLECTOR", 300, null))
        .contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isCreated());
    mvc.perform(post("/api/v1/manual-items").header("Authorization", "Bearer " + token(issuer, "recruitops-api", "VIEWER", 300, null))
        .contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isForbidden());
    for (String invalid : new String[] { "malformed", token(issuer, "recruitops-api", "ADMIN", -300, null),
        token("https://foreign.test", "recruitops-api", "ADMIN", 300, null), token(issuer, "other-api", "ADMIN", 300, null) }) {
      mvc.perform(post("/api/v1/manual-items").header("Authorization", "Bearer " + invalid)
          .contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isUnauthorized());
    }
    String viewer = "Bearer " + token(issuer, "recruitops-api", "VIEWER", 300, null);
    for (String path : new String[] { "sources", "agent-settings", "manual-items", "review-items", "runs", "browser-tasks" })
      mvc.perform(get("/api/v1/" + path).header("Authorization", viewer)).andExpect(status().isForbidden());
  }

  @Test void realAuthorizationCodeExchangeCreatesSessionAndEnforcesCsrf() throws Exception {
    var start = mvc.perform(get("/oauth2/authorization/recruitops")).andExpect(status().is3xxRedirection()).andReturn();
    String location = start.getResponse().getHeader("Location");
    loginNonce = query(location, "nonce");
    MockHttpSession session = (MockHttpSession) start.getRequest().getSession(false);
    var callback = mvc.perform(get("/login/oauth2/code/recruitops").session(session)
        .param("code", "fixture-code").param("state", query(location, "state")))
        .andExpect(status().is3xxRedirection()).andReturn();
    assertFalse(callback.getResponse().getHeader("Location").contains("error"));
    session = (MockHttpSession) callback.getRequest().getSession(false);
    mvc.perform(get("/api/v1/auth/session").session(session)).andExpect(status().isOk())
        .andExpect(jsonPath("authenticated").value(true)).andExpect(jsonPath("roles[0]").value("REVIEWER"))
        .andExpect(jsonPath("logoutUrl").value("/api/v1/auth/logout"));
    mvc.perform(post("/api/v1/manual-items").session(session).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/auth/logout").session(session)).andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/auth/logout").session(session).header("Authorization", "Bearer malformed"))
        .andExpect(status().isUnauthorized());
    var csrfResponse = mvc.perform(get("/api/v1/auth/session").session(session)).andReturn().getResponse();
    var csrf = json.readTree(csrfResponse.getContentAsString());
    mvc.perform(post("/api/v1/auth/logout").session(session).cookie(csrfResponse.getCookie("XSRF-TOKEN"))
        .header("Origin", "http://127.0.0.1:5173")
        .header(csrf.get("csrfHeader").asText(), csrf.get("csrfToken").asText())).andExpect(status().isNoContent())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
        .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    assertTrue(session.isInvalid());
  }

  @Test void logoutPreflightAllowsOnlyTheConfiguredFrontendOriginAndCsrfHeader() throws Exception {
    mvc.perform(options("/api/v1/auth/logout").header("Origin", "http://127.0.0.1:5173")
        .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "X-XSRF-TOKEN"))
        .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
        .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
        .andExpect(header().string("Access-Control-Allow-Headers", "X-XSRF-TOKEN"));
    mvc.perform(options("/api/v1/auth/logout").header("Origin", "https://foreign.test")
        .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "X-XSRF-TOKEN"))
        .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }

  private static String token(String tokenIssuer, String audience, String role, long seconds, String nonce) throws Exception {
    var claims = new JWTClaimsSet.Builder().issuer(tokenIssuer).subject("fixture-operator").audience(audience)
        .issueTime(Date.from(Instant.now())).expirationTime(Date.from(Instant.now().plusSeconds(seconds)))
        .claim("realm_access", Map.of("roles", java.util.List.of(role)));
    if (nonce != null) claims.claim("nonce", nonce);
    var signed = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).keyID("fixture").build(), claims.build());
    signed.sign(new RSASSASigner(key));
    return signed.serialize();
  }
  private static String query(String url, String key) {
    for (String item : URI.create(url).getRawQuery().split("&")) {
      String[] parts = item.split("=", 2);
      if (parts[0].equals(key)) return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
    }
    throw new IllegalArgumentException("Missing " + key);
  }
}
