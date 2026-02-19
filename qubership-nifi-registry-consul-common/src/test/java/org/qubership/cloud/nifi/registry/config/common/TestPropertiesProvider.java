package org.qubership.cloud.nifi.registry.config.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TestPropertiesProvider
        implements PropertiesProvider {

    private static final Map<String, String> TEST_VALUES = new HashMap<>();
    static {
        TEST_VALUES.put("logger.org.qubership", "DEBUG");
        TEST_VALUES.put("logger.org.apache.nifi.registry.StdErr", "INFO");
        TEST_VALUES.put("nifi.registry.db.maxConnections", "15");
        TEST_VALUES.put("nifi.registry.security.user.oidc.connect.timeout", "10 secs");
    }

    @Override
    public Set<String> getAllPropertyNamesFromSource() {
        return TEST_VALUES.keySet();
    }

    @Override
    public String getPropertyValue(String propertyName) {
        return TEST_VALUES.get(propertyName);
    }
}
