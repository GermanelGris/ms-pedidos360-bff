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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.client.DownstreamClient;

@RestController
@RequestMapping("/api/catalog/products")
@Tag(name = "Catálogo")
public class CatalogController {

    private final DownstreamClient downstream;

    public CatalogController(DownstreamClient downstream) {
        this.downstream = downstream;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    @Operation(summary = "Listar productos")
    public ResponseEntity<String> list(@AuthenticationPrincipal Jwt jwt) {
        return downstream.catalog(HttpMethod.GET, "/api/catalog/products", null, jwt);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('Admin', 'Operador', 'Cliente')")
    @Operation(summary = "Detalle de un producto")
    public ResponseEntity<String> get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return downstream.catalog(HttpMethod.GET, "/api/catalog/products/" + id, null, jwt);
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Crear producto (solo Admin)")
    public ResponseEntity<String> create(@RequestBody String body, @AuthenticationPrincipal Jwt jwt) {
        return downstream.catalog(HttpMethod.POST, "/api/catalog/products", body, jwt);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Actualizar producto, precio y stock (solo Admin)")
    public ResponseEntity<String> update(@PathVariable Long id, @RequestBody String body,
                                         @AuthenticationPrincipal Jwt jwt) {
        return downstream.catalog(HttpMethod.PUT, "/api/catalog/products/" + id, body, jwt);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Eliminar producto (solo Admin)")
    public ResponseEntity<String> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return downstream.catalog(HttpMethod.DELETE, "/api/catalog/products/" + id, null, jwt);
    }
}
