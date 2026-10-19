package cl.duoc.pedidos360.bff.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.client.DownstreamClient;

/** Reportería y KPIs (ms-pedidos360-report, alimentado por eventos de RabbitMQ). Solo el rol Admin. */
@RestController
@RequestMapping("/api/report")
@PreAuthorize("hasRole('Admin')")
@Tag(name = "Reportería")
public class ReportController {

    private final DownstreamClient downstream;

    public ReportController(DownstreamClient downstream) {
        this.downstream = downstream;
    }

    @GetMapping("/kpis")
    @Operation(summary = "Pedidos totales, activos, ventas, lead time promedio y pedidos por estado")
    public ResponseEntity<String> kpis(@AuthenticationPrincipal Jwt jwt) {
        return downstream.report("/api/report/kpis", jwt);
    }

    @GetMapping("/sales-by-hour")
    @Operation(summary = "Ventas por hora de las últimas N horas")
    public ResponseEntity<String> salesByHour(@RequestParam(defaultValue = "24") int hours,
                                              @AuthenticationPrincipal Jwt jwt) {
        return downstream.report("/api/report/sales-by-hour?hours={hours}", jwt, hours);
    }

    @GetMapping("/top-products")
    @Operation(summary = "Productos más vendidos")
    public ResponseEntity<String> topProducts(@RequestParam(defaultValue = "5") int limit,
                                              @AuthenticationPrincipal Jwt jwt) {
        return downstream.report("/api/report/top-products?limit={limit}", jwt, limit);
    }
}
