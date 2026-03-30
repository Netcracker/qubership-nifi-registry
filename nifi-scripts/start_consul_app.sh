#!/bin/bash -e

# shellcheck source=/dev/null
# shellcheck disable=SC2034
# shellcheck disable=SC2086
. /opt/nifi-registry/scripts/logging_api.sh

info "Starting consul app with options: $CONSUL_CONFIG_JAVA_OPTIONS"
info "Consul integration framework = $NIFI_CONSUL_INT_FRAMEWORK"

if [ "$NIFI_CONSUL_INT_FRAMEWORK" = 'spring' ]; then
    # if mode explicitly set as spring, then run spring:
    eval "$JAVA_HOME"/bin/java "$CONSUL_CONFIG_JAVA_OPTIONS" \
        -jar "$NIFI_REGISTRY_HOME"/utility-lib/qubership-nifi-registry-consul-application.jar &
    consul_pid=$!
else
    # if mode explicitly set as quarkus, use quarkus:
    # or if nothing set, use quarkus:
    eval "$JAVA_HOME"/bin/java "$CONSUL_CONFIG_JAVA_OPTIONS" \
        -jar "$NIFI_REGISTRY_HOME"/utility-lib/qubership-nifi-registry-quarkus-consul-application/quarkus-run.jar &
    consul_pid=$!
fi

info "Consul application pid: $consul_pid"
