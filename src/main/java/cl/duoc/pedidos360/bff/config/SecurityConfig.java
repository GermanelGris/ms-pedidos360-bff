package cl.duoc.pedidos360.bff.config;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderInitializationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * El BFF actúa como Resource Server: igual que el API Gateway, valida el JWT emitido por Microsoft Entra ID
 * antes de permitir consumir cualquier endpoint.
 *
 * <ul>
 *   <li>Firma: con las llaves públicas (JWKS) publicadas por el issuer.</li>
 *   <li>Issuer y vigencia (exp/nbf): {@link JwtValidators#createDefaultWithIssuer(String)}.</li>
 *   <li>Audience: el token debe estar emitido para la app Pedidos360-API.</li>
 *   <li>Scope: todo /api exige el scope delegado de la API.</li>
 *   <li>Rol: claim {@code roles} convertido a ROLE_*, verificado con {@code @PreAuthorize} en cada endpoint.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Value("${pedidos360.security.required-scope}") String requiredScope) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error", "/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/**").hasAuthority("SCOPE_" + requiredScope)
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(SecurityConfig::unauthorized)
                .accessDeniedHandler(SecurityConfig::forbidden))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(SecurityConfig::unauthorized)
                .accessDeniedHandler(SecurityConfig::forbidden));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
            @Value("${spring.security.oauth2.resourceserver.jwt.audiences}") List<String> audiences) {
        // Se resuelve en la primera petición: descarga la metadata OIDC y las llaves (JWKS) del IDaaS
        JwtDecoder decoder = new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder nimbus = NimbusJwtDecoder.withIssuerLocation(issuer).build();
            nimbus.setJwtValidator(tokenValidator(issuer, audiences));
            return nimbus;
        });
        return token -> {
            try {
                return decoder.decode(token);
            } catch (JwtDecoderInitializationException ex) {
                // IDaaS inalcanzable o issuer mal configurado: se responde 401, nunca se deja pasar la petición
                throw new BadJwtException("No fue posible obtener las llaves públicas del IDaaS (" + issuer + ")", ex);
            }
        };
    }

    static OAuth2TokenValidator<Jwt> tokenValidator(String issuer, List<String> audiences) {
        OAuth2TokenValidator<Jwt> issuerAndTimestamps = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audience = jwt -> {
            List<String> tokenAudiences = jwt.getAudience();
            if (tokenAudiences != null && tokenAudiences.stream().anyMatch(audiences::contains)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                    "El token no fue emitido para esta API (aud=" + tokenAudiences + ")", null));
        };
        return new DelegatingOAuth2TokenValidator<>(issuerAndTimestamps, audience);
    }

    /** roles -> ROLE_Admin / ROLE_Operador / ROLE_Cliente ; scp -> SCOPE_Pedidos.ReadWrite */
    static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(roles.convert(jwt));
            authorities.addAll(scopes.convert(jwt));
            return authorities;
        });
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    static void unauthorized(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException ex) throws IOException {
        String message = "Se requiere un token Bearer válido";
        if (ex instanceof OAuth2AuthenticationException oauth && oauth.getError().getDescription() != null) {
            message = oauth.getError().getDescription();
        } else if (ex instanceof AuthenticationServiceException && ex.getMessage() != null) {
            message = ex.getMessage();
        }
        response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
        writeError(request, response, 401, "Unauthorized", message);
    }

    static void forbidden(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException ex) throws IOException {
        writeError(request, response, 403, "Forbidden", "Tu rol o scope no tiene permiso para usar este recurso");
    }

    private static void writeError(HttpServletRequest request, HttpServletResponse response,
            int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s"}"""
                .formatted(Instant.now(), status, error, escape(message), escape(request.getRequestURI())));
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
