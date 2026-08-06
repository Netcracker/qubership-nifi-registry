#!/bin/sh -e
# shellcheck disable=SC2153

port="$NIFI_REGISTRY_WEB_HTTPS_PORT"
schema='https'
#unsecured setups serve the actuator endpoint, secured ones reject the unauthenticated call:
expectedCode='401'
if [ -z "$NIFI_REGISTRY_WEB_HTTPS_PORT" ]; then
    port="$NIFI_REGISTRY_WEB_HTTP_PORT"
    if [ -z "$port" ]; then
        port=18080
    fi
    schema='http'
    expectedCode='200'
fi
host='localhost'
if [ -n "$NIFI_REGISTRY_CHECK_HOST" ]; then
    host="$NIFI_REGISTRY_CHECK_HOST"
fi

#with two-way SSL the server rejects the handshake unless a client certificate is presented,
#so reuse the keystore of NiFi Registry itself. Its identity is authenticated but not
#authorized to view the actuator, hence 403 rather than 401.
clientKeystore=''
clientPassword=''
if [ "$AUTH" = 'tls' ] && [ -n "$KEYSTORE_PATH" ]; then
    clientKeystore="$KEYSTORE_PATH"
    clientPassword="$KEYSTORE_PASSWORD"
    if [ -z "$clientPassword" ] && [ -n "$NIFI_REG_KEYSTORE_PASSWORD_PATH" ]; then
        clientPassword=$(cat "$NIFI_REG_KEYSTORE_PASSWORD_PATH")
    fi
    expectedCode='403'
fi

echo "Host=$host, port = $port, schema=$schema, calling /nifi-registry-api/actuator/health"
respFile=$(mktemp)
if [ -n "$clientKeystore" ]; then
    respCode=$(curl -s -k --connect-timeout 2 --max-time 5 -o "$respFile" --write-out "%{http_code}" \
        --cert "$clientKeystore:$clientPassword" --cert-type P12 \
        "$schema://$host:$port/nifi-registry-api/actuator/health")
else
    respCode=$(curl -s -k --connect-timeout 2 --max-time 5 -o "$respFile" --write-out "%{http_code}" \
        "$schema://$host:$port/nifi-registry-api/actuator/health")
fi

echo "Response code = $respCode (expected $expectedCode)"
if [ "$respCode" != "$expectedCode" ]; then
    cat "$respFile"
    rm -rf "$respFile"
    exit 22;
fi
rm -rf "$respFile"
