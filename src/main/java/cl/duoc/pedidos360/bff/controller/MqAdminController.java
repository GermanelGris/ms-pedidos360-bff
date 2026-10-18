package cl.duoc.pedidos360.bff.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.client.DownstreamClient;

/** Administración de RabbitMQ (ms-pedidos360-mq-admin). Solo el rol Admin. */
@RestController
@RequestMapping("/api/mq")
@PreAuthorize("hasRole('Admin')")
@Tag(name = "Administración RabbitMQ")
public class MqAdminController {

    private final DownstreamClient downstream;

    public MqAdminController(DownstreamClient downstream) {
        this.downstream = downstream;
    }

    @GetMapping("/queues")
    @Operation(summary = "Listar colas")
    public ResponseEntity<String> queues(@AuthenticationPrincipal Jwt jwt) {
        return downstream.mqAdmin(HttpMethod.GET, "/api/mq/queues", null, jwt);
    }

    @GetMapping("/queues/{name}")
    @Operation(summary = "Detalle de una cola")
    public ResponseEntity<String> queue(@PathVariable String name, @AuthenticationPrincipal Jwt jwt) {
        return downstream.mqAdmin(HttpMethod.GET, "/api/mq/queues/{name}", null, jwt, name);
    }

    @PostMapping("/queues")
    @Operation(summary = "Crear cola")
    public ResponseEntity<String> createQueue(@RequestBody String body, @AuthenticationPrincipal Jwt jwt) {
        return downstream.mqAdmin(HttpMethod.POST, "/api/mq/queues", body, jwt);
    }

    @DeleteMapping("/queues/{name}")
    @Operation(summary = "Eliminar cola")
    public ResponseEntity<String> deleteQueue(@PathVariable String name, @AuthenticationPrincipal Jwt jwt) {
        return downstream.mqAdmin(HttpMethod.DELETE, "/api/mq/queues/{name}", null, jwt, name);
    }

    @DeleteMapping("/queues/{name}/messages")
    @Operation(summary = "Vaciar cola")
    public ResponseEntity<String> purgeQueue(@PathVariable String name, @AuthenticationPrincipal Jwt jwt) {
        return downstream.mqAdmin(HttpMethod.DELETE, "/api/mq/queues/{name}/messages", null, jwt, name);
    }

    @GetMapping("/exchanges")
    @Operation(summary = "Listar exchanges")
    public ResponseEntity<String> exchanges(@AuthenticationPrincipal Jwt jwt) {
        return downstream.mqAdmin(HttpMethod.GET, "/api/mq/exchanges", null, jwt);
    }

    @PostMapping("/exchanges")
    @Operation(summary = "Crear exchange")
    public ResponseEntity<String> createExchange(@RequestBody String body, @AuthenticationPrincipal Jwt jwt) {
        return downstream.mqAdmin(HttpMethod.POST, "/api/mq/exchanges", body, jwt);
    }

    @DeleteMapping("/exchanges/{name}")
    @Operation(summary = "Eliminar exchange")
    public ResponseEntity<String> deleteExchange(@PathVariable String name, @AuthenticationPrincipal Jwt jwt) {
        return downstream.mqAdmin(HttpMethod.DELETE, "/api/mq/exchanges/{name}", null, jwt, name);
    }

    @GetMapping("/bindings")
    @Operation(summary = "Listar bindings")
    public ResponseEntity<String> bindings(@AuthenticationPrincipal Jwt jwt) {
        return downstream.mqAdmin(HttpMethod.GET, "/api/mq/bindings", null, jwt);
    }

    @PostMapping("/bindings")
    @Operation(summary = "Crear binding")
    public ResponseEntity<String> createBinding(@RequestBody String body, @AuthenticationPrincipal Jwt jwt) {
        return downstream.mqAdmin(HttpMethod.POST, "/api/mq/bindings", body, jwt);
    }

    @DeleteMapping("/bindings")
    @Operation(summary = "Eliminar binding (exchange + queue + routingKey)")
    public ResponseEntity<String> deleteBinding(@RequestParam String exchange, @RequestParam String queue,
                                                @RequestParam(defaultValue = "") String routingKey,
                                                @AuthenticationPrincipal Jwt jwt) {
        return downstream.mqAdmin(HttpMethod.DELETE,
                "/api/mq/bindings?exchange={exchange}&queue={queue}&routingKey={routingKey}", null, jwt,
                exchange, queue, routingKey);
    }

    @PostMapping("/messages")
    @Operation(summary = "Publicar mensaje de prueba")
    public ResponseEntity<String> publish(@RequestBody String body, @AuthenticationPrincipal Jwt jwt) {
        return downstream.mqAdmin(HttpMethod.POST, "/api/mq/messages", body, jwt);
    }
}
