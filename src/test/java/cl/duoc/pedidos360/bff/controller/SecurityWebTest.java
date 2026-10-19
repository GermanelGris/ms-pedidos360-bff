package cl.duoc.pedidos360.bff.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import cl.duoc.pedidos360.bff.client.DownstreamClient;
import cl.duoc.pedidos360.bff.config.SecurityConfig;

@WebMvcTest(controllers = {MeController.class, OrdersController.class, CatalogController.class,
        MqAdminController.class, ReportController.class, AuditController.class})
@Import(SecurityConfig.class)
class SecurityWebTest {

    private static final String SCOPE = "SCOPE_Pedidos.ReadWrite";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DownstreamClient downstream;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor usuario(String rol) {
        return jwt()
                .jwt(token -> token.claim("roles", List.of(rol)).claim("scp", "Pedidos.ReadWrite").claim("name", "Test " + rol))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol), new SimpleGrantedAuthority(SCOPE));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void tokenSinScopeDeLaApiResponde403() throws Exception {
        mvc.perform(get("/api/me").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void usuarioAutenticadoVeSusClaims() throws Exception {
        mvc.perform(get("/api/me").with(usuario("Cliente")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("Cliente"))
                .andExpect(jsonPath("$.scopes[0]").value("Pedidos.ReadWrite"));
    }

    @Test
    void clienteNoPuedeCrearProductos() throws Exception {
        mvc.perform(post("/api/catalog/products").with(usuario("Cliente"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(downstream);
    }

    @Test
    void adminPuedeCrearProductos() throws Exception {
        given(downstream.catalog(eq(HttpMethod.POST), eq("/api/catalog/products"), anyString(), any()))
                .willReturn(ResponseEntity.status(201).body("{\"id\":1}"));

        mvc.perform(post("/api/catalog/products").with(usuario("Admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Pizza\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void clienteNoPuedeCambiarEstadoDePedido() throws Exception {
        mvc.perform(patch("/api/orders/1/status").with(usuario("Cliente"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACEPTADO\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(downstream);
    }

    @Test
    void operadorPuedeCambiarEstadoDePedido() throws Exception {
        given(downstream.orders(eq(HttpMethod.PATCH), eq("/api/orders/1/status"), anyString(), any()))
                .willReturn(ResponseEntity.ok("{\"id\":1,\"status\":\"ACEPTADO\"}"));

        mvc.perform(patch("/api/orders/1/status").with(usuario("Operador"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACEPTADO\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void operadorNoPuedeAdministrarRabbitMq() throws Exception {
        mvc.perform(get("/api/mq/queues").with(usuario("Operador"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/mq/queues").with(usuario("Cliente"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"q.demo\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(downstream);
    }

    @Test
    void adminListaColas() throws Exception {
        given(downstream.mqAdmin(eq(HttpMethod.GET), eq("/api/mq/queues"), isNull(), any()))
                .willReturn(ResponseEntity.ok("[{\"name\":\"q.cmd.email\"}]"));

        mvc.perform(get("/api/mq/queues").with(usuario("Admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("q.cmd.email"));
    }

    @Test
    void adminEliminaBindingPropagandoLosParametros() throws Exception {
        given(downstream.mqAdmin(eq(HttpMethod.DELETE),
                eq("/api/mq/bindings?exchange={exchange}&queue={queue}&routingKey={routingKey}"), isNull(), any(),
                eq("ex.demo"), eq("q.demo"), eq("demo.#")))
                .willReturn(ResponseEntity.noContent().build());

        mvc.perform(delete("/api/mq/bindings").with(usuario("Admin"))
                        .param("exchange", "ex.demo").param("queue", "q.demo").param("routingKey", "demo.#"))
                .andExpect(status().isNoContent());
    }

    @Test
    void soloAdminVeReporteriaYAuditoria() throws Exception {
        mvc.perform(get("/api/report/kpis").with(usuario("Operador"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit/events").with(usuario("Cliente"))).andExpect(status().isForbidden());
        verifyNoInteractions(downstream);
    }

    @Test
    void adminConsultaKpis() throws Exception {
        given(downstream.report(eq("/api/report/kpis"), any())).willReturn(ResponseEntity.ok("{\"totalOrders\":3}"));

        mvc.perform(get("/api/report/kpis").with(usuario("Admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(3));
    }

    @Test
    void adminFiltraAuditoriaPropagandoLosParametros() throws Exception {
        given(downstream.audit(
                eq("/api/audit/events?user={user}&type={type}&entityId={entityId}&from={from}&to={to}&limit={limit}"),
                any(), eq("operador"), eq("OrderStatusChanged"), eq(""), eq("2026-10-18"), eq(""), eq(50)))
                .willReturn(ResponseEntity.ok("[]"));

        mvc.perform(get("/api/audit/events").with(usuario("Admin")).param("user", "operador")
                        .param("type", "OrderStatusChanged").param("from", "2026-10-18").param("limit", "50"))
                .andExpect(status().isOk());
    }

    @Test
    void soloAdminCreaMetasDeVenta() throws Exception {
        mvc.perform(post("/api/report/goals").with(usuario("Operador"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(downstream);

        given(downstream.reportWrite(eq(HttpMethod.POST), eq("/api/report/goals"), anyString(), any()))
                .willReturn(ResponseEntity.status(201).body("{\"id\":1}"));
        mvc.perform(post("/api/report/goals").with(usuario("Admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Octubre\"}"))
                .andExpect(status().isCreated());
    }
}
