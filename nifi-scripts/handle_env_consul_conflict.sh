#!/bin/bash -e

# shellcheck source=/dev/null
. /opt/nifi-registry/scripts/logging_api.sh

# if environment variables are not set, read them from nifi-registry.properties
# this allows us to get default values as well as values from Consul.
if [ -f "${NIFI_REGISTRY_HOME}"/conf/nifi-registry.properties ]; then
    # IFS is the 'internal field separator'. In this case, file uses '='
    IFS="="
    while read -r key value
    do
        if [[ "$key" == "nifi.registry.db.maxConnections" ]]; then
            if [[ -z "$NIFI_REGISTRY_DB_MAX_CONNS" ]] && [[ -n "$value" ]]; then
                export NIFI_REGISTRY_DB_MAX_CONNS="$value"
            fi
        fi
        if [[ "$key" == "nifi.registry.db.sql.debug" ]]; then
            if [[ -z "$NIFI_REGISTRY_DB_DEBUG_SQL" ]] && [[ -n "$value" ]]; then
                export NIFI_REGISTRY_DB_DEBUG_SQL="$value"
            fi
        fi
        if [[ "$key" == "nifi.registry.security.user.oidc.connect.timeout" ]]; then
            if [[ -z "$NIFI_REGISTRY_SECURITY_USER_OIDC_CONNECT_TIMEOUT" ]] && [[ -n "$value" ]]; then
                export NIFI_REGISTRY_SECURITY_USER_OIDC_CONNECT_TIMEOUT="$value"
            fi
        fi
        if [[ "$key" == "nifi.registry.security.user.oidc.read.timeout" ]]; then
            if [[ -z "$NIFI_REGISTRY_SECURITY_USER_OIDC_READ_TIMEOUT" ]] && [[ -n "$value" ]]; then
                export NIFI_REGISTRY_SECURITY_USER_OIDC_READ_TIMEOUT="$value"
            fi
        fi
    done < "${NIFI_REGISTRY_HOME}"/conf/nifi-registry.properties
    unset IFS
fi
