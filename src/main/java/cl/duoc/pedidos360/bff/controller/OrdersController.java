package cl.duoc.pedidos360.bff.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.client.DownstreamClient;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Pedidos")
public class OrdersController {

    private final DownstreamClient downstream;

    public OrdersController(DownstreamClient downstream) {
        this.downstream = downstream;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    @Operation(summary = "Listar pedidos (el Cliente solo ve los suyos)")
    public ResponseEntity<String> list(@AuthenticationPrincipal Jwt jwt) {
        return downstream.orders(HttpMethod.GET, "/api/orders", null, jwt);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    @Operation(summary = "Detalle de un pedido")
    public ResponseEntity<String> get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return downstream.orders(HttpMethod.GET, "/api/orders/" + id, null, jwt);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Operador', 'Cliente')")
    @Operation(summary = "Crear pedido (Cliente u Operador)")
    public ResponseEntity<String> create(@RequestBody String body, @AuthenticationPrincipal Jwt jwt) {
        return downstream.orders(HttpMethod.POST, "/api/orders", body, jwt);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Admin', 'Operador')")
    @Operation(summary = "Cambiar estado del pedido (Operador o Admin)")
    public ResponseEntity<String> changeStatus(@PathVariable Long id, @RequestBody String body,
                                               @AuthenticationPrincipal Jwt jwt) {
        return downstream.orders(HttpMethod.PATCH, "/api/orders/" + id + "/status", body, jwt);
    }
}
