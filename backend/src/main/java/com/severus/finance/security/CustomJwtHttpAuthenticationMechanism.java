package com.severus.finance.security;

import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpCredentialTransport;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Collections;
import java.util.Set;

@Alternative
@Priority(1)
@ApplicationScoped
public class CustomJwtHttpAuthenticationMechanism implements HttpAuthenticationMechanism {

    @Inject
    JwtService jwtService;

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        String path = context.request().path();
        // Allow public endpoints to pass through without authentication check
        if (path.startsWith("/q/") || path.equals("/api/v1/auth/login") || path.equals("/api/v1/auth/refresh")) {
            return Uni.createFrom().nullItem();
        }

        String authHeader = context.request().getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                try {
                    JsonWebToken jwt = jwtService.verifyToken(token);
                    Set<String> roles = jwt.getGroups() != null ? jwt.getGroups() : Set.of("USER");
                    SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                            .setPrincipal(jwt)
                            .addRoles(roles)
                            .addAttribute("org.eclipse.microprofile.jwt.JsonWebToken", jwt)
                            .build();
                    return Uni.createFrom().item(identity);
                } catch (Exception e) {
                    org.jboss.logging.Logger.getLogger(CustomJwtHttpAuthenticationMechanism.class).error("JWT verification failed in CustomJwtHttpAuthenticationMechanism", e);
                    return Uni.createFrom().nullItem();
                }
            }
        }

        return Uni.createFrom().nullItem();
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        ChallengeData challenge = new ChallengeData(401, "Bearer", "realm=\"severus-finance\"");
        return Uni.createFrom().item(challenge);
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Collections.emptySet();
    }

    @Override
    public Uni<HttpCredentialTransport> getCredentialTransport(RoutingContext context) {
        return Uni.createFrom().item(new HttpCredentialTransport(HttpCredentialTransport.Type.AUTHORIZATION, "Bearer"));
    }
}
