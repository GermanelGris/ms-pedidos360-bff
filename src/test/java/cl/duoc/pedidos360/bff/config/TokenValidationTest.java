package cl.duoc.pedidos360.bff.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

class TokenValidationTest {

    private static final String ISSUER = "https://login.microsoftonline.com/tenant-test/v2.0";
    private static final String API_CLIENT_ID = "11111111-2222-3333-4444-555555555555";

    private final OAuth2TokenValidator<Jwt> validator =
            SecurityConfig.tokenValidator(ISSUER, List.of(API_CLIENT_ID, "api://" + API_CLIENT_ID));

    private static Jwt token(String issuer, String audience, Instant expiresAt) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuer(issuer)
                .audience(List.of(audience))
                .issuedAt(expiresAt.minusSeconds(7200))
                .expiresAt(expiresAt)
                .build();
    }

    @Test
    void tokenValidoPasaLaValidacion() {
        Jwt jwt = token(ISSUER, API_CLIENT_ID, Instant.now().plusSeconds(600));
        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void tokenDeOtroIssuerEsRechazado() {
        Jwt jwt = token("https://sts.windows.net/tenant-test/", API_CLIENT_ID, Instant.now().plusSeconds(600));
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void tokenParaOtraAudienceEsRechazado() {
        String microsoftGraph = "00000003-0000-0000-c000-000000000000";
        Jwt jwt = token(ISSUER, microsoftGraph, Instant.now().plusSeconds(600));
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void tokenExpiradoEsRechazado() {
        Jwt jwt = token(ISSUER, API_CLIENT_ID, Instant.now().minusSeconds(3600));
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void rolesYScopesSeConviertenEnAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-1")
                .claim("roles", List.of("Admin"))
                .claim("scp", "Pedidos.ReadWrite")
                .build();

        var authentication = SecurityConfig.jwtAuthenticationConverter().convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_Admin", "SCOPE_Pedidos.ReadWrite");
    }
}
