package org.qubership.cloud.nifi.registry.quarkus.config;

import com.netcracker.cloud.consul.provider.common.TokenStorage;
import io.quarkus.arc.properties.UnlessBuildProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Consul Client Configuration providing token storage taken from property.
 */
@Singleton
public class ConsulClientConfiguration {

    /**
     * Creates token storage instance, getting fixed token
     * to access Consul from property "quarkus.consul.acl-token.token".
     * @param aclToken token for connecting to Consul
     * @return TokenStorage instance
     */
    @Produces
    @ApplicationScoped
    @UnlessBuildProperty(name = "quarkus.consul.acl-token.enabled", stringValue = "false", enableIfMissing = true)
    public TokenStorage propertyBasedTokenStorage(
            @ConfigProperty(name = "quarkus.consul.acl-token.token") String aclToken) {
        return new TokenStorage() {
            @Override
            public String get() {
                return aclToken;
            }

            @Override
            public void update(String s) {
                // nothing
            }
        };
    }
}
