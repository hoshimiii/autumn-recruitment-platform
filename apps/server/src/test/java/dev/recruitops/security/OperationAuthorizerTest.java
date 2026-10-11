package dev.recruitops.security;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class OperationAuthorizerTest {
  @AfterEach void clear() { SecurityContextHolder.clearContext(); }

  @Test void localOwnerIsExplicitAndInvalidModesFailClosed() {
    assertTrue(new OperationAuthorizer("local", "", "realm_access.roles").can("ADMIN"));
    assertThrows(IllegalArgumentException.class, () -> new OperationAuthorizer("disabled", "", "roles"));
    assertThrows(IllegalArgumentException.class, () -> new OperationAuthorizer("oidc", "", "roles"));
  }

  @Test void oidcDoesNotTrustMissingContextOrSubmittedAuthorities() {
    OperationAuthorizer authorizer = new OperationAuthorizer("oidc", "https://issuer.test", "realm_access.roles");
    assertFalse(authorizer.can("ADMIN"));
    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
        "user", "", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    assertFalse(authorizer.can("ADMIN"));
  }

  @Test void issuerRolesAreBoundedAndAdminEscalates() {
    OperationAuthorizer authorizer = new OperationAuthorizer("oidc", "https://issuer.test", "realm_access.roles");
    authenticate("https://issuer.test", "COLLECTOR");
    assertTrue(authorizer.can("COLLECTOR"));
    assertFalse(authorizer.can("REVIEWER"));
    assertFalse(authorizer.can("ADMIN"));
    authenticate("https://issuer.test", "ADMIN");
    assertTrue(authorizer.can("REVIEWER"));
    authenticate("https://foreign.test", "ADMIN");
    assertFalse(authorizer.can("ADMIN"));
  }

  private void authenticate(String issuer, String role) {
    Jwt jwt = Jwt.withTokenValue("validated-by-filter").header("alg", "RS256")
        .issuer(issuer).subject("operator").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300))
        .claim("realm_access", Map.of("roles", List.of(role))).build();
    SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,
        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }
}
