package org.qubership.cloud.nifi.registry.security.authorization.database;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.nifi.registry.security.authorization.AccessPolicy;
import org.apache.nifi.registry.security.authorization.RequestAction;
import org.apache.nifi.registry.security.authorization.database.DatabaseAccessPolicyProvider;
import org.apache.nifi.registry.security.authorization.exception.AuthorizationAccessException;

import java.time.Duration;

public class CachedDatabaseAccessPolicyProvider
        extends DatabaseAccessPolicyProvider {

    private final LoadingCache<PolicyKey, AccessPolicy> accessPolicyCacheByResource =
            Caffeine.newBuilder().
            expireAfterWrite(Duration.ofSeconds(300)).
            build(key -> super.getAccessPolicy(key.getResourceIdentifier(),
                    key.getAction()));

    /**
     * Gets access policy by resource and action.
     * @param resourceIdentifier resource identifier
     * @param action action
     * @return access policy
     * @throws AuthorizationAccessException
     */
    @Override
    public AccessPolicy getAccessPolicy(String resourceIdentifier, RequestAction action)
            throws AuthorizationAccessException {
        PolicyKey pk = new PolicyKey(resourceIdentifier, action);
        return accessPolicyCacheByResource.get(pk);
    }

    @Override
    public AccessPolicy updateAccessPolicy(AccessPolicy accessPolicy) throws AuthorizationAccessException {
        AccessPolicy ap = super.updateAccessPolicy(accessPolicy);
        PolicyKey pk = new PolicyKey(ap.getResource(), ap.getAction());
        accessPolicyCacheByResource.invalidate(pk);
        return ap;
    }

    @Override
    public AccessPolicy deleteAccessPolicy(AccessPolicy accessPolicy) throws AuthorizationAccessException {
        AccessPolicy ap = super.deleteAccessPolicy(accessPolicy);
        PolicyKey pk = new PolicyKey(ap.getResource(), ap.getAction());
        accessPolicyCacheByResource.invalidate(pk);
        return ap;
    }
}
