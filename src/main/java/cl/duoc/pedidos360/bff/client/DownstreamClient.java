package cl.duoc.pedidos360.bff.client;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriBuilder;

import cl.duoc.pedidos360.bff.security.TokenClaims;

/**
 * Reenvía las peticiones ya autenticadas y autorizadas a los microservicios internos,
 * propagando la identidad del usuario (X-User-*) y su IP de origen (X-Client-Ip).
 */
@Component
public class DownstreamClient {

    private final RestClient ordersClient;
    private final RestClient catalogClient;
    private final RestClient reportClient;
    private final RestClient auditClient;

    public DownstreamClient(@Value("${services.orders-url}") String ordersUrl,
                            @Value("${services.catalog-url}") String catalogUrl,
                            @Value("${services.report-url}") String reportUrl,
                            @Value("${services.audit-url}") String auditUrl) {
        this.ordersClient = RestClient.create(ordersUrl);
        this.catalogClient = RestClient.create(catalogUrl);
        this.reportClient = RestClient.create(reportUrl);
        this.auditClient = RestClient.create(auditUrl);
    }

    public ResponseEntity<String> orders(HttpMethod method, String path, String body, Jwt jwt) {
        return forward(ordersClient.method(method).uri(path), body, jwt);
    }

    public ResponseEntity<String> catalog(HttpMethod method, String path, String body, Jwt jwt) {
        return forward(catalogClient.method(method).uri(path), body, jwt);
    }

    public ResponseEntity<String> report(String path, Map<String, String> query, Jwt jwt) {
        return forward(reportClient.method(HttpMethod.GET).uri(builder -> withQuery(builder.path(path), query)), null, jwt);
    }

    public ResponseEntity<String> audit(String path, Map<String, String> query, Jwt jwt) {
        return forward(auditClient.method(HttpMethod.GET).uri(builder -> withQuery(builder.path(path), query)), null, jwt);
    }

    /** Arma un mapa de query params ignorando los valores vacíos: query("user", u, "type", t). */
    public static Map<String, String> query(String... keyValues) {
        Map<String, String> query = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            if (keyValues[i + 1] != null && !keyValues[i + 1].isBlank()) {
                query.put(keyValues[i], keyValues[i + 1]);
            }
        }
        return query;
    }

    private static URI withQuery(UriBuilder builder, Map<String, String> query) {
        query.keySet().forEach(key -> builder.queryParam(key, "{" + key + "}"));
        return builder.build(query);
    }

    private ResponseEntity<String> forward(RestClient.RequestBodySpec request, String body, Jwt jwt) {
        request.accept(MediaType.APPLICATION_JSON)
                .header("X-User-Id", TokenClaims.userId(jwt))
                .header("X-User-Name", URLEncoder.encode(TokenClaims.userName(jwt), StandardCharsets.UTF_8))
                .header("X-User-Email", Objects.toString(jwt.getClaimAsString("preferred_username"), ""))
                .header("X-User-Roles", String.join(",", TokenClaims.roles(jwt)))
                .header("X-Client-Ip", clientIp());
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return request.exchange((clientRequest, clientResponse) -> {
            String content = new String(clientResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
            return ResponseEntity.status(clientResponse.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(content);
        });
    }

    /** IP del cliente: primera IP de X-Forwarded-For (API Gateway) o la IP remota directa. */
    private static String clientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servlet) {
            HttpServletRequest request = servlet.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "";
    }
}
