package dev.recruitops.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {
  @Bean
  @ConditionalOnProperty(name = "recruitops.security.mode", havingValue = "local", matchIfMissing = true)
  SecurityFilterChain localSecurity(HttpSecurity http,
      @Value("${server.address:127.0.0.1}") String address) throws Exception {
    if (!java.util.List.of("127.0.0.1", "localhost", "::1").contains(address))
      throw new IllegalArgumentException("Local owner mode must bind a loopback address; use OIDC for network access");
    return http.cors(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
  }

  @Bean
  @ConditionalOnProperty(name = "recruitops.security.mode", havingValue = "oidc")
  JwtDecoder trustedJwtDecoder(@Value("${recruitops.security.issuer-uri}") String issuer,
      @Value("${recruitops.security.audience}") String audience) {
    if (audience.isBlank()) throw new IllegalArgumentException("OIDC audience is required");
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
    decoder.setJwtValidator(validators(issuer, audience));
    return decoder;
  }

  static OAuth2TokenValidator<Jwt> validators(String issuer, String audience) {
    OAuth2TokenValidator<Jwt> audienceCheck = jwt -> jwt.getExpiresAt() != null && jwt.getAudience() != null
        && jwt.getAudience().contains(audience)
        ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", "Required audience or expiry is missing", null));
    return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audienceCheck);
  }

  @Bean @Order(1)
  @ConditionalOnProperty(name = "recruitops.security.mode", havingValue = "oidc")
  SecurityFilterChain bearerSecurity(HttpSecurity http, JwtDecoder decoder, OperationAuthorizer authorizer) throws Exception {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> authorizer.issuerRoles(jwt.getClaims()).stream()
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role)).toList());
    // This chain never reads cookies: every Bearer header must pass JWT validation before writes.
    http.securityMatcher(request -> {
      String header = request.getHeader("Authorization");
      return header != null && header.regionMatches(true, 0, "Bearer", 0, 6);
    }).cors(Customizer.withDefaults()).csrf(csrf -> csrf.disable())
        .logout(logout -> logout.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(converter)));
    apiAccess(http);
    return http.build();
  }

  @Bean @Order(2)
  @ConditionalOnProperty(name = "recruitops.security.mode", havingValue = "oidc")
  SecurityFilterChain sessionSecurity(HttpSecurity http, OperationAuthorizer authorizer,
      @Value("${recruitops.security.frontend-url:http://127.0.0.1:5173/}") String frontendUrl) throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .oauth2Login(login -> login.userInfoEndpoint(user -> user.userAuthoritiesMapper(authorities ->
            authorities.stream().filter(OidcUserAuthority.class::isInstance).map(OidcUserAuthority.class::cast)
                .flatMap(oidc -> authorizer.issuerRoles(oidc.getIdToken().getClaims()).stream())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList()))
            .defaultSuccessUrl(frontendUrl, true))
        .logout(logout -> logout.logoutUrl("/api/v1/auth/logout").logoutSuccessHandler((request, response, auth) -> response.setStatus(204)))
        .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
    apiAccess(http);
    return http.build();
  }

  private void apiAccess(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.GET, "/api/v1/jobs", "/api/v1/jobs/**", "/api/v1/exports/**",
            "/api/v1/health", "/api/v1/auth/session").permitAll()
        .requestMatchers("/oauth2/**", "/login/**").permitAll()
        .requestMatchers("/api/v1/agent-settings/**", "/api/v1/projections/**").hasRole("ADMIN")
        .requestMatchers("/api/v1/sources", "/api/v1/sources/**", "/api/v1/manual-items/**", "/api/v1/runs/**",
            "/api/v1/browser-tasks/**").hasAnyRole("COLLECTOR", "ADMIN")
        .requestMatchers("/api/v1/review-items/**").hasAnyRole("REVIEWER", "ADMIN")
        .requestMatchers("/api/**").authenticated().anyRequest().denyAll());
  }
}
