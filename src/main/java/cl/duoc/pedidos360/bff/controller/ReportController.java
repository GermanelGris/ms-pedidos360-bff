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

@RestController
@RequestMapping("/api/report")
@Tag(name = "Reportería")
@PreAuthorize("hasRole('Admin')")
public class ReportController {

    private final DownstreamClient downstream;

    public ReportController(DownstreamClient downstream) {
        this.downstream = downstream;
    }

    @GetMapping("/kpis")
    @Operation(summary = "KPIs: pedidos, activos, ventas, lead time y estados (solo Admin)")
    public ResponseEntity<String> kpis(@AuthenticationPrincipal Jwt jwt) {
        return downstream.report("/api/report/kpis", DownstreamClient.query(), jwt);
    }

    @GetMapping("/sales-by-hour")
    @Operation(summary = "Ventas por hora de las últimas N horas (solo Admin)")
    public ResponseEntity<String> salesByHour(@RequestParam(required = false) String hours,
                                              @AuthenticationPrincipal Jwt jwt) {
        return downstream.report("/api/report/sales-by-hour", DownstreamClient.query("hours", hours), jwt);
    }

    @GetMapping("/top-products")
    @Operation(summary = "Productos más vendidos (solo Admin)")
    public ResponseEntity<String> topProducts(@RequestParam(required = false) String limit,
                                              @AuthenticationPrincipal Jwt jwt) {
        return downstream.report("/api/report/top-products", DownstreamClient.query("limit", limit), jwt);
    }
}
