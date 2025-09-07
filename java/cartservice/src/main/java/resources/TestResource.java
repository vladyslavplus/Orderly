package resources;

import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import security.JwtUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Path("api/test")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {

    private static final Logger LOG = Logger.getLogger(TestResource.class.getName());

    @Inject
    JwtUtils jwtUtils;

    @GET
    @Path("/public")
    @PermitAll
    public Response publicEndpoint() {
        return Response.ok(Map.of("message", "This is a public endpoint")).build();
    }

    @GET
    @Path("/test-token")
    @PermitAll
    public Response testToken(@HeaderParam("Authorization") String authHeader) {
        LOG.info("Received Authorization header: " + authHeader);
        return Response.ok(Map.of("authHeader", authHeader)).build();
    }

    @GET
    @Path("/jwt")
    @Authenticated
    public Response testJwt() {
        try {
            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("userIdFromUtils", jwtUtils.getUserIdFromToken().toString());
            tokenInfo.put("userNameFromUtils", jwtUtils.getUserName());
            tokenInfo.put("roleFromUtils", jwtUtils.getRole());
            tokenInfo.put("allRoles", jwtUtils.getAllRoles());
            tokenInfo.put("issuer", jwtUtils.getIssuer());
            tokenInfo.put("audience", jwtUtils.getAudience());
            tokenInfo.put("tokenId", jwtUtils.getTokenId());
            tokenInfo.put("issuedAt", jwtUtils.getIssuedAt().orElse(null));
            tokenInfo.put("expiresAt", jwtUtils.getExpiresAt().orElse(null));
            tokenInfo.put("notBefore", jwtUtils.getNotBefore().orElse(null));
            tokenInfo.put("isExpired", jwtUtils.isTokenExpired());

            return Response.ok(tokenInfo).build();

        } catch (Exception e) {
            LOG.severe("Error in JWT test endpoint: " + e.getMessage());
            return Response.status(500)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
