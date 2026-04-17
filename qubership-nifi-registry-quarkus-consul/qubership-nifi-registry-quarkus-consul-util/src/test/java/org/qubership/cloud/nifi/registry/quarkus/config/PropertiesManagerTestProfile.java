package org.qubership.cloud.nifi.registry.quarkus.config;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class PropertiesManagerTestProfile
        implements QuarkusTestProfile {

    public PropertiesManagerTestProfile () {
        //default constructor
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "NIFI_REGISTRY_HOME", "./test-location",
                "nifi.registry.home", "./test-location2",
                "nifi.registry.env.prop1", "1",
                "nifi.registry.env.prop2", "2"
        );
    }
}
