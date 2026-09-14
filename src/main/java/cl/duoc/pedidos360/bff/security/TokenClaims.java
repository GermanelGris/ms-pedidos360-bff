package cl.duoc.pedidos360.bff.security;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

/** Lectura de claims del access token de Microsoft Entra ID. */
public final class TokenClaims {

    private TokenClaims() {
    }

    public static List<String> roles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null ? roles : List.of();
    }

    public static List<String> scopes(Jwt jwt) {
        String scp = jwt.getClaimAsString("scp");
        return scp == null || scp.isBlank() ? List.of() : List.of(scp.split(" "));
    }

    /** oid = identificador inmutable del usuario en el tenant. */
    public static String userId(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        return oid != null ? oid : jwt.getSubject();
    }

    public static String userName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name == null) {
            name = jwt.getClaimAsString("preferred_username");
        }
        return name != null ? name : userId(jwt);
    }
}
