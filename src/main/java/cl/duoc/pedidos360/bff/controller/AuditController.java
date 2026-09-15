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

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Auditoría")
@PreAuthorize("hasAnyRole('Admin', 'Auditor')")
public class AuditController {

    private final DownstreamClient downstream;

    public AuditController(DownstreamClient downstream) {
        this.downstream = downstream;
    }

    @GetMapping("/events")
    @Operation(summary = "Timeline de eventos de negocio con filtros (solo lectura)")
    public ResponseEntity<String> events(@RequestParam(required = false) String user,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false) String entityId,
                                         @RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to,
                                         @RequestParam(required = false) String limit,
                                         @AuthenticationPrincipal Jwt jwt) {
        return downstream.audit("/api/audit/events", DownstreamClient.query(
                "user", user, "type", type, "entityId", entityId, "from", from, "to", to, "limit", limit), jwt);
    }

    @GetMapping("/orders/{orderId}/timeline")
    @Operation(summary = "Trazabilidad completa de un pedido (solo lectura)")
    public ResponseEntity<String> orderTimeline(@PathVariable Long orderId, @AuthenticationPrincipal Jwt jwt) {
        return downstream.audit("/api/audit/orders/" + orderId + "/timeline", DownstreamClient.query(), jwt);
    }
}
