package cl.duoc.pedidos360.bff.client;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import cl.duoc.pedidos360.bff.security.TokenClaims;

/**
 * Reenvía las peticiones ya autenticadas y autorizadas a los microservicios internos,
 * propagando la identidad del usuario en headers X-User-*.
 */
@Component
public class DownstreamClient {

    private final RestClient ordersClient;
    private final RestClient catalogClient;
    private final RestClient mqAdminClient;
    private final RestClient reportClient;
    private final RestClient auditClient;

    public DownstreamClient(@Value("${services.orders-url}") String ordersUrl,
                            @Value("${services.catalog-url}") String catalogUrl,
                            @Value("${services.mq-admin-url}") String mqAdminUrl,
                            @Value("${services.report-url}") String reportUrl,
                            @Value("${services.audit-url}") String auditUrl) {
        this.ordersClient = RestClient.create(ordersUrl);
        this.catalogClient = RestClient.create(catalogUrl);
        this.mqAdminClient = RestClient.create(mqAdminUrl);
        this.reportClient = RestClient.create(reportUrl);
        this.auditClient = RestClient.create(auditUrl);
    }

    public ResponseEntity<String> orders(HttpMethod method, String path, String body, Jwt jwt) {
        return forward(ordersClient, method, path, body, jwt);
    }

    public ResponseEntity<String> catalog(HttpMethod method, String path, String body, Jwt jwt) {
        return forward(catalogClient, method, path, body, jwt);
    }

    /** {@code path} es una plantilla: los nombres de colas o routing keys van como variables para codificarse bien. */
    public ResponseEntity<String> mqAdmin(HttpMethod method, String path, String body, Jwt jwt,
                                          Object... uriVariables) {
        return forward(mqAdminClient, method, path, body, jwt, uriVariables);
    }

    /** Reportería (solo lectura). {@code path} es una plantilla con los parámetros de consulta como variables. */
    public ResponseEntity<String> report(String path, Jwt jwt, Object... uriVariables) {
        return forward(reportClient, HttpMethod.GET, path, null, jwt, uriVariables);
    }

    /** Auditoría (solo lectura). {@code path} es una plantilla con los filtros como variables. */
    public ResponseEntity<String> audit(String path, Jwt jwt, Object... uriVariables) {
        return forward(auditClient, HttpMethod.GET, path, null, jwt, uriVariables);
    }

    private ResponseEntity<String> forward(RestClient client, HttpMethod method, String path, String body, Jwt jwt,
                                           Object... uriVariables) {
        RestClient.RequestBodySpec request = client.method(method)
                .uri(path, uriVariables)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-User-Id", TokenClaims.userId(jwt))
                .header("X-User-Name", URLEncoder.encode(TokenClaims.userName(jwt), StandardCharsets.UTF_8))
                .header("X-User-Roles", String.join(",", TokenClaims.roles(jwt)));
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
}
