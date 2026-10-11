package dev.recruitops.security;

import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("operationAuthorizer")
public class OperationAuthorizer {
  public static final List<String> ROLES = List.of("VIEWER", "COLLECTOR", "REVIEWER", "ADMIN");
  private final String mode;
  private final String issuer;
  private final String roleClaim;

  public OperationAuthorizer(@Value("${recruitops.security.mode:local}") String mode,
      @Value("${recruitops.security.issuer-uri:}") String issuer,
      @Value("${recruitops.security.role-claim:realm_access.roles}") String roleClaim) {
    if (!List.of("local", "oidc").contains(mode)) throw new IllegalArgumentException("Security mode must be local or oidc");
    if (mode.equals("oidc") && (issuer.isBlank() || !URI.create(issuer).isAbsolute()))
      throw new IllegalArgumentException("OIDC mode requires an absolute issuer URI");
    if (roleClaim.isBlank()) throw new IllegalArgumentException("OIDC role claim is required");
    this.mode = mode;
    this.issuer = issuer;
    this.roleClaim = roleClaim;
  }

  public String mode() { return mode; }
  public boolean can(String role) {
    if (!ROLES.contains(role)) return false;
    return mode.equals("local") || (authenticated() && (roles().contains("ADMIN") || roles().contains(role)));
  }
  public boolean authenticated() { return mode.equals("local") || trustedClaims() != null; }
  public List<String> roles() {
    if (mode.equals("local")) return List.of("ADMIN");
    Map<String, Object> claims = trustedClaims();
    return claims == null ? List.of() : issuerRoles(claims);
  }
  public List<String> capabilities() { return ROLES.stream().filter(this::can).toList(); }

  List<String> issuerRoles(Map<String, Object> claims) {
    Object value = claims;
    for (String part : roleClaim.split("\\.")) value = value instanceof Map<?, ?> map ? map.get(part) : null;
    return value instanceof Collection<?> roles ? ROLES.stream().filter(roles::contains).toList() : List.of();
  }

  private Map<String, Object> trustedClaims() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return null;
    if (auth instanceof JwtAuthenticationToken bearer) {
      var token = bearer.getToken();
      return issuer.equals(token.getClaimAsString("iss")) && token.getExpiresAt() != null
          && token.getExpiresAt().isAfter(Instant.now()) ? token.getClaims() : null;
    }
    if (auth instanceof OAuth2AuthenticationToken login && login.getAuthorizedClientRegistrationId().equals("recruitops")
        && login.getPrincipal() instanceof OidcUser user) {
      return issuer.equals(user.getIdToken().getClaimAsString("iss")) ? user.getIdToken().getClaims() : null;
    }
    return null;
  }
}
