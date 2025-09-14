package security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;
import io.quarkus.security.identity.SecurityIdentity;

import java.util.UUID;

@ApplicationScoped
public class JwtUtils {

    @Inject
    SecurityIdentity securityIdentity;

    private JsonWebToken jwt() {
        return (JsonWebToken) securityIdentity.getPrincipal();
    }

    public UUID getUserId() {
        String sub = jwt().getClaim("sub");
        return UUID.fromString(sub);
    }

    public String getRole() {
        return jwt().getClaim("role");
    }
}