package cl.duoc.pedidos360.bff.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.client.DownstreamClient;

/** Timeline de auditoría (ms-pedidos360-audit, alimentado por eventos de RabbitMQ). Solo el rol Admin. */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('Admin')")
@Tag(name = "Auditoría")
public class AuditController {

    private final DownstreamClient downstream;

    public AuditController(DownstreamClient downstream) {
        this.downstream = downstream;
    }

    @GetMapping("/events")
    @Operation(summary = "Eventos con filtros opcionales: usuario, tipo, pedido y rango de fechas (yyyy-MM-dd)")
    public ResponseEntity<String> events(@RequestParam(defaultValue = "") String user,
                                         @RequestParam(defaultValue = "") String type,
                                         @RequestParam(defaultValue = "") String entityId,
                                         @RequestParam(defaultValue = "") String from,
                                         @RequestParam(defaultValue = "") String to,
                                         @RequestParam(defaultValue = "100") int limit,
                                         @AuthenticationPrincipal Jwt jwt) {
        return downstream.audit(
                "/api/audit/events?user={user}&type={type}&entityId={entityId}&from={from}&to={to}&limit={limit}",
                jwt, user, type, entityId, from, to, limit);
    }

    @GetMapping("/orders/{orderId}/timeline")
    @Operation(summary = "Trazabilidad completa de un pedido")
    public ResponseEntity<String> orderTimeline(@PathVariable Long orderId, @AuthenticationPrincipal Jwt jwt) {
        return downstream.audit("/api/audit/orders/{orderId}/timeline", jwt, orderId);
    }
}
