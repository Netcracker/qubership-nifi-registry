#!/bin/bash -e

generate_random_hex_password() {
    #args -- letters, numbers
    echo "$(tr -dc A-F </dev/urandom | head -c "$1")""$(tr -dc 0-9 </dev/urandom | head -c "$2")" | fold -w 1 | shuf | tr -d '\n'
}

generate_random_password() {
    #args -- letters, numbers, special characters
    echo "$(tr -dc '[:lower:]''[:upper:]' </dev/urandom | head -c "$1")""$(tr -dc 0-9 </dev/urandom | head -c "$2")""\
$(tr -dc '!@#%^&*()-+{}=`~,<>./?' </dev/urandom | head -c "$3")" | fold -w 1 | shuf | tr -d '\n'
}

generate_uuid() {
    head=$(head -c 16 /dev/urandom | od -An -t x1 | tr -d ' ')
    echo "${head:0:8}-${head:8:4}-${head:12:4}-${head:16:4}-${head:20:12}"
}

get_next_summary_file_name() {
    current_steps_count=$(find "./test-results/$1" -name "summary_*.txt" | wc -l)
    echo "summary_step$(printf %03d $((current_steps_count + 1))).txt"
}

configure_log_level() {
    local targetPkg="$1"
    local targetLevel="$2"
    local secretId="$3"
    local consulUrl="$4"
    local ns="$5"
    if [ -z "$consulUrl" ]; then
        consulUrl='http://localhost:8500'
    fi
    if [ -z "$ns" ]; then
        ns='local'
    fi
    echo "Configuring log level = $targetLevel for $targetPkg..."
    targetPath=$(echo "logger.$targetPkg" | sed 's|\.|/|g')
    echo "Consul URL = $consulUrl, namespace = $ns, targetPath = $targetPath"
    rm -rf ./consul-put-resp.txt
    respCode=$(curl -X PUT -sS --data "$targetLevel" -w '%{response_code}' -o ./consul-put-resp.txt --header "X-Consul-Token: ${secretId}" \
        "$consulUrl/v1/kv/config/$ns/application/$targetPath")
    echo "Response code = $respCode"
    if [ "$respCode" == "200" ]; then
        echo "Successfully set log level in consul"
        rm -rf ./consul-put-resp.txt
    else
        echo "Failed to set log level in Consul. Response code = $respCode. Error message:"
        cat ./consul-put-resp.txt
        return 1
    fi
}

test_log_level() {
    local targetPkg="$1"
    local targetLevel="$2"
    local resultsDir="$3"
    local containerName="$4"
    local secretId="$5"
    resultsPath="./test-results/$resultsDir"
    echo "Testing Consul logging parameters configuration for package = $targetPkg, level = $targetLevel"
    echo "Results path = $resultsPath"
    configure_log_level "$targetPkg" "$targetLevel" "$secretId" ||
        echo "Consul config failed" >"$resultsPath/failed_consul_config.lst"
    echo "Waiting 20 seconds..."
    sleep 20
    echo "Copying logback.xml..."
    docker cp "$containerName":/opt/nifi-registry/nifi-registry-current/conf/logback.xml "$resultsPath/logback.xml"
    res="0"
    grep "$targetPkg" "$resultsPath/logback.xml" | grep 'logger' | grep "$targetLevel" || res="1"
    summaryFileName=$(get_next_summary_file_name "$resultsDir")
    if [ "$res" == "0" ]; then
        echo "Logback configuration successfully applied"
        echo "| Logging levels configuration                   | Success :white_check_mark: |" >"$resultsPath/$summaryFileName"
    else
        echo "Logback configuration failed to apply"
        echo "NiFi Registry logger config update failed" >"$resultsPath/failed_log_config.lst"
        echo "| Logging levels configuration                   | Failed :x:                 |" >"$resultsPath/$summaryFileName"
    fi
}

prepare_sens_key() {
    echo "Generating temporary sensitive key..."
    NIFI_SENSITIVE_KEY=$(generate_random_hex_password 12 4)
    export NIFI_SENSITIVE_KEY
    echo "$NIFI_SENSITIVE_KEY" >./nifi-sens-key.tmp
}

prepare_results_dir() {
    local resultsDir="$1"
    echo "Preparing output directory $resultsDir"
    mkdir -p "./test-results/$resultsDir/"
}

start_containers_and_wait() {
    local composeFile="$1"
    local resultsDir="$2"
    local waitTimeout="$3"
    local extraComposeFile="$4"
    if [ -z "$waitTimeout" ]; then
        echo "Using default timeout = 180 seconds"
        waitTimeout=180
    fi
    local composeArgs=(-f "$composeFile")
    if [ -n "$extraComposeFile" ]; then
        echo "Using additional compose file: $extraComposeFile"
        composeArgs+=(-f "$extraComposeFile")
    fi
    composeArgs+=(--env-file ./docker.env)
    echo "Starting containers from $composeFile and waiting up to $waitTimeout seconds for them to become healthy..."
    wait_success="1"
    docker compose "${composeArgs[@]}" up -d --wait --wait-timeout "$waitTimeout" || wait_success="0"
    summaryFileName=$(get_next_summary_file_name "$resultsDir")
    if [ "$wait_success" == '0' ]; then
        echo "List of containers:"
        docker ps -a
        echo "Wait failed, nifi registry not available. Last 500 lines of logs for compose $composeFile"
        echo "resultsDir=$resultsDir"
        docker compose "${composeArgs[@]}" logs -n 500 >./nifi_registry_log_tmp.lst
        cat ./nifi_registry_log_tmp.lst
        echo "Wait failed, nifi registry not available" >"./test-results/$resultsDir/failed_nifi_registry_wait.lst"
        mv ./nifi_registry_log_tmp.lst "./test-results/$resultsDir/nifi_registry_log_after_wait.log"
        echo "| Wait for container start                       | Failed :x:                 |" >"./test-results/$resultsDir/$summaryFileName"
        return 1
    fi
    echo "Wait finished successfully. All containers are up and healthy."
    echo "| Wait for container start                       | Success :white_check_mark: |" >"./test-results/$resultsDir/$summaryFileName"
    return 0
}

generate_tls_passwords() {
    echo "Generating passwords..."
    TRUSTSTORE_PASSWORD=$(generate_random_password 8 4 3)
    KEYSTORE_PASSWORD_NIFI=$(generate_random_password 8 4 3)
    KEYSTORE_PASSWORD_NIFI_REG=$(generate_random_password 8 4 3)
    KEYCLOAK_TLS_PASS=$(generate_random_hex_password 8 4)
    export TRUSTSTORE_PASSWORD
    export KEYSTORE_PASSWORD_NIFI
    export KEYSTORE_PASSWORD_NIFI_REG
    export KEYCLOAK_TLS_PASS
}

create_docker_env_file() {
    local runMode="$1"
    echo "Generating environment file for docker-compose..."
    echo "TRUSTSTORE_PASSWORD=$TRUSTSTORE_PASSWORD" >./docker.env
    echo "KEYSTORE_PASSWORD_NIFI=$KEYSTORE_PASSWORD_NIFI" >>./docker.env
    echo "KEYSTORE_PASSWORD_NIFI_REG=$KEYSTORE_PASSWORD_NIFI_REG" >>./docker.env
    if [[ "$runMode" == "upgrade-"* ]]; then
        #in case of upgrade scenarios, use simple password w/o special characters:
        DB_PASSWORD=$(generate_random_hex_password 8 4)
    else
        #in case of main scenarios, use password with special characters:
        DB_PASSWORD=$(generate_random_password 8 4 3)
    fi
    export DB_PASSWORD
    echo "DB_PASSWORD=$DB_PASSWORD" >>./docker.env
    KEYCLOAK_ADMIN_PASSWORD=$(generate_random_hex_password 8 4)
    export KEYCLOAK_ADMIN_PASSWORD
    echo "KEYCLOAK_ADMIN_PASSWORD=$KEYCLOAK_ADMIN_PASSWORD" >>./docker.env
    gitDir="$(pwd)"
    echo "BASE_DIR=$gitDir" >>./docker.env
    echo "KEYCLOAK_TLS_PASS=$KEYCLOAK_TLS_PASS" >>./docker.env
    CONSUL_TOKEN=$(generate_uuid)
    echo "$CONSUL_TOKEN" >./consul-acl-token.tmp
    export CONSUL_TOKEN
    echo "CONSUL_TOKEN=$CONSUL_TOKEN" >>./docker.env
}

generate_add_certs() {
    keytool -genkeypair -alias keycloakCA -keypass "$KEYCLOAK_TLS_PASS" -keystore ./temp-vol/tls-cert/keycloak.p12 -storetype PKCS12 \
        -storepass "$KEYCLOAK_TLS_PASS" -keyalg RSA -dname "CN=keycloakCA" -ext bc:c
    keytool -genkeypair -alias keycloakServer -keypass "$KEYCLOAK_TLS_PASS" -keystore ./temp-vol/tls-cert/keycloak.p12 -storetype PKCS12 \
        -storepass "$KEYCLOAK_TLS_PASS" -keyalg RSA -dname "CN=keycloak" -signer keycloakCA -signerkeypass \
        "$KEYCLOAK_TLS_PASS" -ext SAN=dns:keycloak,dns:localhost
    keytool -importkeystore -srckeystore ./temp-vol/tls-cert/keycloak.p12 -destkeystore ./temp-vol/tls-cert/keycloak-server.p12 -srcstoretype PKCS12 \
        -deststoretype PKCS12 -srcstorepass "$KEYCLOAK_TLS_PASS" -deststorepass "$KEYCLOAK_TLS_PASS" -srcalias \
        keycloakServer -destalias keycloakServer -srckeypass "$KEYCLOAK_TLS_PASS" -destkeypass "$KEYCLOAK_TLS_PASS"
    keytool -exportcert -keystore ./temp-vol/tls-cert/keycloak.p12 -storetype PKCS12 -storepass "$KEYCLOAK_TLS_PASS" -alias keycloakCA -rfc \
        -file ./temp-vol/tls-cert/ca/keycloak-ca.cer
    keytool -importcert -keystore ./temp-vol/tls-cert/keycloak-server.p12 -storetype PKCS12 -storepass "$KEYCLOAK_TLS_PASS" \
        -file ./temp-vol/tls-cert/ca/keycloak-ca.cer -alias keycloak-ca-cer -noprompt
}

setup_env_before_tests() {
    local runMode="$1"
    prepare_results_dir "$runMode"
    generate_tls_passwords
    create_docker_env_file "$runMode"
    if [[ "$runMode" == "plain" ]] || [[ "$runMode" == "tls" ]] || [[ "$runMode" == "tls-quarkus" ]]; then
        mkdir -p ./temp-vol/nifi-reg/database/
        mkdir -p ./temp-vol/nifi-reg/flow-storage/
    else
        mkdir -p ./temp-vol/pg-db/
    fi
    mkdir -p ./temp-vol/tls-cert/
    mkdir -p ./temp-vol/tls-cert/nifi-registry/
    mkdir -p ./temp-vol/tls-cert/pwd/
    if [[ "$runMode" == "oidc" ]]; then
        mkdir -p ./temp-vol/tls-cert/ca/
    fi
    chmod -R 777 ./temp-vol
    #generate keycloak certificates:
    if [[ "$runMode" == "oidc" ]]; then
        generate_add_certs
    fi
}
