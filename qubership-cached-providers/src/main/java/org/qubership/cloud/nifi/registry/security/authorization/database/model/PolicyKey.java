package org.qubership.cloud.nifi.registry.security.authorization.database.model;

import org.apache.nifi.registry.security.authorization.RequestAction;

import java.util.Objects;

public class PolicyKey {
    private String resourceIdentifier;
    private RequestAction action;

    /**
     *
     * @param newResourceIdentifier
     * @param newAction
     */
    public PolicyKey(final String newResourceIdentifier, final RequestAction newAction) {
        this.resourceIdentifier = newResourceIdentifier;
        this.action = newAction;
    }

    public String getResourceIdentifier() {
        return resourceIdentifier;
    }

    public void setResourceIdentifier(String resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    public RequestAction getAction() {
        return action;
    }

    public void setAction(RequestAction action) {
        this.action = action;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PolicyKey policyKey = (PolicyKey) o;
        return Objects.equals(resourceIdentifier, policyKey.resourceIdentifier) && action == policyKey.action;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceIdentifier, action);
    }
}
