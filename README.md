# ms-pedidos360-bff

Backend for Frontend de **Pedidos360**. Es la única puerta de entrada al backend (detrás de AWS API Gateway) y, igual que el API Manager, **valida el JWT emitido por Microsoft Entra ID** antes de reenviar la petición a los microservicios internos.

## Seguridad

| Validación | Dónde |
|---|---|
| Firma del token (JWKS del IDaaS) | `SecurityConfig#jwtDecoder` |
| Issuer (`iss`) y vigencia (`exp`/`nbf`) | `JwtValidators.createDefaultWithIssuer` |
| Audience (`aud` = API de Pedidos360) | `SecurityConfig#tokenValidator` |
| Scope delegado (`scp` = `Pedidos.ReadWrite`) | `SecurityFilterChain` → `/api/**` |
| Rol (`roles` → `ROLE_*`) | `@PreAuthorize` en cada endpoint |
| Errores | `401` (token ausente/inválido/expirado) y `403` (rol o scope insuficiente) en JSON |

## Endpoints

| Método | Ruta | Roles |
|---|---|---|
| GET | `/api/me` | cualquier usuario autenticado |
| GET | `/api/orders`, `/api/orders/{id}` | Admin, Operador, Cliente |
| POST | `/api/orders` | Operador, Cliente |
| PATCH | `/api/orders/{id}/status` | Admin, Operador |
| GET | `/api/catalog/products`, `/api/catalog/products/{id}` | Admin, Operador, Cliente |
| POST / PUT / DELETE | `/api/catalog/products[/{id}]` | Admin |

Swagger: `http://localhost:8080/swagger-ui.html`

## Variables de entorno

| Variable | Descripción | Por defecto |
|---|---|---|
| `TENANT_ID` | Id. de directorio (inquilino) de Entra ID | — |
| `API_CLIENT_ID` | Id. de aplicación de `Pedidos360-API` | — |
| `API_SCOPE` | Scope expuesto por la API | `Pedidos.ReadWrite` |
| `ORDERS_URL` | URL interna de orders | `http://localhost:8081` |
| `CATALOG_URL` | URL interna de catalog | `http://localhost:8082` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos | `http://localhost:4200` |

## Ejecutar

```bash
./mvnw test                 # pruebas (validación de token, 401/403 por rol)
./mvnw spring-boot:run      # requiere TENANT_ID y API_CLIENT_ID
docker build -t pedidos360-bff .
```

Requisitos: Java 21, Spring Boot 4.
