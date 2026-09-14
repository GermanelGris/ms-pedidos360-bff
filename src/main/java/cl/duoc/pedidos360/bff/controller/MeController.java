package cl.duoc.pedidos360.bff.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.security.TokenClaims;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Usuario")
public class MeController {

    @GetMapping
    @Operation(summary = "Datos del usuario autenticado leídos desde los claims del token")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", TokenClaims.userId(jwt));
        body.put("name", TokenClaims.userName(jwt));
        body.put("username", jwt.getClaimAsString("preferred_username"));
        body.put("roles", TokenClaims.roles(jwt));
        body.put("scopes", TokenClaims.scopes(jwt));
        body.put("issuer", jwt.getClaimAsString("iss"));
        body.put("audience", jwt.getAudience());
        body.put("expiresAt", jwt.getExpiresAt());
        return body;
    }
}
