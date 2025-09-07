package security;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import io.quarkus.security.identity.SecurityIdentity;

import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class JwtUtils {

    private static final Logger LOG = Logger.getLogger(JwtUtils.class);

    @Inject
    SecurityIdentity securityIdentity;

    private JsonWebToken jwt() {
        return (JsonWebToken) securityIdentity.getPrincipal();
    }

    public UUID getUserIdFromToken() {
        String sub = getClaim("sub", String.class)
                .orElseThrow(() -> new IllegalStateException("Subject claim is missing"));
        LOG.infof("Getting user ID from token: %s", sub);
        return UUID.fromString(sub);
    }

    public String getUserName() {
        return getClaim("name", String.class).orElse(null);
    }

    public String getRole() {
        return getClaim("role", String.class).orElse(null);
    }

    public Set<String> getAllRoles() {
        Set<String> groups = jwt().getGroups();
        LOG.debugf("All user roles from token: %s", groups);
        return groups;
    }

    public String getIssuer() {
        return getClaim("iss", String.class).orElse(null);
    }

    public String getAudience() {
        Object aud = getClaim("aud", Object.class).orElse(null);
        return aud != null ? aud.toString() : null;
    }

    public String getTokenId() {
        return getClaim("jti", String.class).orElse(null);
    }

    public Optional<Instant> getIssuedAt() {
        Long iat = getClaim("iat", Long.class).orElse(null);
        return iat != null ? Optional.of(Instant.ofEpochSecond(iat)) : Optional.empty();
    }

    public Optional<Instant> getExpiresAt() {
        Long exp = getClaim("exp", Long.class).orElse(null);
        return exp != null ? Optional.of(Instant.ofEpochSecond(exp)) : Optional.empty();
    }

    public Optional<Instant> getNotBefore() {
        Long nbf = getClaim("nbf", Long.class).orElse(null);
        return nbf != null ? Optional.of(Instant.ofEpochSecond(nbf)) : Optional.empty();
    }

    public boolean isTokenExpired() {
        return getExpiresAt().map(exp -> Instant.now().isAfter(exp)).orElse(false);
    }

    public <T> Optional<T> getClaim(String claimName, Class<T> clazz) {
        try {
            T value = jwt().getClaim(claimName);
            LOG.debugf("Claim '%s': %s", claimName, value);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            LOG.warnf("Error getting claim '%s': %s", claimName, e.getMessage());
            return Optional.empty();
        }
    }
}