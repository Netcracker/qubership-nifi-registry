#!/bin/sh -e
# shellcheck disable=SC2153

port="$NIFI_REGISTRY_WEB_HTTPS_PORT"
schema='https'
if [ -z "$NIFI_REGISTRY_WEB_HTTPS_PORT" ]; then
    port="$NIFI_REGISTRY_WEB_HTTP_PORT"
    if [ -z "$port" ]; then
        port=18080
    fi
    schema='http'
fi
host='localhost'
if [ -n "$NIFI_REGISTRY_CHECK_HOST" ]; then
    host="$NIFI_REGISTRY_CHECK_HOST"
fi

echo "Host=$host, port = $port, schema=$schema, calling /nifi-registry-api/actuator/health"
respFile=$(mktemp)
respCode=$(curl -s -k --connect-timeout 2 --max-time 5 -o "$respFile" --write-out "%{http_code}" \
    "$schema://$host:$port/nifi-registry-api/actuator/health")

echo "Response code = $respCode (expected 401)"
if [ "$respCode" != "401" ]; then
    cat "$respFile"
    rm -rf "$respFile"
    exit 22;
fi
rm -rf "$respFile"
