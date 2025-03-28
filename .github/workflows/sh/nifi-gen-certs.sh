#!/bin/bash -e

generate_nifi_certs(){
    if [ ! -f /tmp/tls-certs/nifi/keystore.p12 ]; then
        mkdir -p /tmp/tls-certs/nifi
        chmod 777 /tmp/tls-certs/nifi
        echo 'Generating nifi certs...'
        "$NIFI_TOOLKIT_HOME"/bin/tls-toolkit.sh standalone -n "localhost" --subjectAlternativeNames "nifi" \
          -C "CN=admin, OU=NIFI" -P "${TRUSTSTORE_PASSWORD}" -S "${KEYSTORE_PASSWORD_NIFI}" -o /tmp/tls-certs/nifi
        echo 'Converting nifi certs to PKCS12...'
        keytool -importkeystore -srckeystore /tmp/tls-certs/nifi/localhost/keystore.jks \
          -srcstorepass "${KEYSTORE_PASSWORD_NIFI}" -srcstoretype JKS -deststoretype PKCS12 \
          -destkeystore /tmp/tls-certs/nifi/keystore.p12 -deststorepass "${KEYSTORE_PASSWORD_NIFI}"
        keytool -importkeystore -srckeystore /tmp/tls-certs/nifi/localhost/truststore.jks \
        -srcstorepass "${TRUSTSTORE_PASSWORD}" -srcstoretype JKS -deststoretype PKCS12 \
        -destkeystore /tmp/tls-certs/nifi/truststore.p12 -deststorepass "${TRUSTSTORE_PASSWORD}";
    else
        echo "Certificates already generated, exiting..."
        return 0;
    fi
    echo "Copying CA certificates..."
    mkdir -p /tmp/tls-certs/nifi-registry
    chmod 777 /tmp/tls-certs/nifi-registry
    cp /tmp/tls-certs/nifi/nifi-cert.pem /tmp/tls-certs/nifi/nifi-key.key /tmp/tls-certs/nifi-registry
    echo 'Generating nifi-registry certs...'
    "$NIFI_TOOLKIT_HOME"/bin/tls-toolkit.sh standalone -n "localhost" --subjectAlternativeNames "nifi-registry" \
      -C "CN=admin, OU=NIFI" -P "${TRUSTSTORE_PASSWORD}" -S "${KEYSTORE_PASSWORD_NIFI_REG}" \
      -o /tmp/tls-certs/nifi-registry
    cp /tmp/tls-certs/nifi-registry/localhost/*.jks /tmp/tls-certs/nifi-registry/
    echo 'Converting nifi-registry certs to PKCS12...'
    keytool -importkeystore -srckeystore /tmp/tls-certs/nifi-registry/keystore.jks \
      -srcstorepass "${KEYSTORE_PASSWORD_NIFI_REG}" -srcstoretype JKS -deststoretype PKCS12 \
      -destkeystore /tmp/tls-certs/nifi-registry/keystore.p12 -deststorepass "${KEYSTORE_PASSWORD_NIFI_REG}"
    keytool -importkeystore -srckeystore /tmp/tls-certs/nifi-registry/truststore.jks \
      -srcstorepass "${TRUSTSTORE_PASSWORD}" -srcstoretype JKS -deststoretype PKCS12 \
      -destkeystore /tmp/tls-certs/nifi-registry/truststore.p12 -deststorepass "${TRUSTSTORE_PASSWORD}"
    #make files available to all users:
    chmod -R 777 /tmp/tls-certs/nifi-registry
    chmod -R 777 /tmp/tls-certs/nifi
    return 0;
}

create_newman_cert_config(){
  echo "Generating newman certificate config..."
  NIFI_CLIENT_PASSWORD=$(cat /tmp/tls-certs/nifi/CN=admin_OU=NIFI.password)
  jq -c '' > ./newman-tls-config.json
  echo '[]' | jq --arg clientCert '/tmp/tls-certs/nifi/CN=admin_OU=NIFI.p12' --arg clientPass "$NIFI_CLIENT_PASSWORD" -c \
    '. += [{"name":"localhost-nifi","matches":["https://localhost:8080/*"],
              "pfx":{"src":$clientCert},"passphrase":$clientPass},
           {"name":"localhost-nifi-registry","matches":["https://localhost:18080/*"],
              "pfx":{"src":$clientCert},"passphrase":$clientPass}
          ]' > /tmp/tls-certs/newman-tls-config.json
}

generate_nifi_certs
create_newman_cert_config