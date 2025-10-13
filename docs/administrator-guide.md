# Administrator's Guide

Qubership-nifi-registry is built on top of Apache NiFi Registry.
Apache NiFi Registry is a service providing version control and abilities to export/import for Apache NiFi flow.
This guide contains details on features added or customized by qubership-nifi-registry.
Refer to Apache NiFi Registry [System Administrator’s Guide](https://nifi.apache.org/docs/nifi-registry-docs/html/administration-guide.html) for details on standard features.

## Environment variables

The table below describes environment variables supported by qubership-nifi-registry.

| Parameter                                        | Required                          | Default | Description                                                                                                                                                                                                                                            |
|--------------------------------------------------|-----------------------------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| AUTH                                             | N                                 |         | Authentication method to support. One of: tls (mTLS), oidc (mTLS and OIDC), ldap (mTLS and LDAP).                                                                                                                                                      |
| INITIAL_ADMIN_IDENTITY                           | Y (if AUTH = oidc or tls or ldap) |         | The identity of an initial admin user that will be granted access to the UI and given the ability to create additional users, groups, and policies. The value of this property could be a DN when using certificates or LDAP, or a Kerberos principal. |
| OIDC_DISCOVERY_URL_NEW                           | Y (if AUTH = oidc)                |         | The Discovery Configuration URL for the OpenID Connect Provider                                                                                                                                                                                        |
| OIDC_CLIENT_ID                                   | Y (if AUTH = oidc)                |         | The Client ID for NiFi registered with the OpenID Connect Provider                                                                                                                                                                                     |
| OIDC_CLIENT_SECRET                               | Y (if AUTH = oidc)                |         | The Client Secret for NiFi registered with the OpenID Connect Provider                                                                                                                                                                                 |
| KEY_PASSWORD                                     | Y (if AUTH = oidc or tls or ldap) |         | The key password for secret key stored in the keystore.                                                                                                                                                                                                |
| KEYSTORE_PASSWORD                                | Y (if AUTH = oidc or tls or ldap) |         | The keystore password for the keystore.                                                                                                                                                                                                                |
| TRUSTSTORE_PASSWORD                              | Y (if AUTH = oidc or tls or ldap) |         | The truststore password.                                                                                                                                                                                                                               |
| NIFI_REGISTRY_WEB_HTTP_PORT                      | N                                 | 18080   | The HTTP port.                                                                                                                                                                                                                                         |
| NIFI_REGISTRY_WEB_HTTPS_PORT                     | N                                 |         | The HTTPS host. It is blank by default.                                                                                                                                                                                                                |
| NIFI_REGISTRY_WEB_HTTP_HOST                      | N                                 |         | The HTTP host. It is blank by default.                                                                                                                                                                                                                 |
| NIFI_REG_JVM_HEAP_INIT                           | N                                 | 512m    | Initial heap memory reserved by JVM. Defines value for Xms JVM startup argument.                                                                                                                                                                       |
| NIFI_REG_JVM_HEAP_MAX                            | N                                 | 512m    | Maximum heap memory reserved by JVM. Defines value for Xmx JVM startup argument.                                                                                                                                                                       |
| NIFI_REG_XSS                                     | N                                 |         | Thread stack size. Defines value for JVM startup argument.                                                                                                                                                                                             |
| NIFI_DEBUG_NATIVE_MEMORY                         | N                                 |         | Enables Native Memory Tracking feature in JVM, if set to non-empty value. Adds `-XX:NativeMemoryTracking=detail` to Java startup arguments.                                                                                                            |
| NIFI_REG_ADDITIONAL_JVM_ARGS                     | N                                 |         | A list of additional Java startup arguments. Must be valid list of arguments separated by spaces just like in command-line.                                                                                                                            |
| NIFI_REG_USE_PGDB                                | N                                 |         | If set to `true`, forces to use PostgreSQL database for storage. Connection parameters for DB are defined by environment variables (NIFI_REG_DB_URL, NIFI_REG_DB_USERNAME, NIFI_REG_DB_PASSWORD) or via script `GetDBConnectionDetails.sh`.            |
| NIFI_REG_DB_FLOW_AUTHORIZERS                     | N                                 | cached  | Sets types of UserGroupProvider and AccessPolicyProvider to use for accessing DB. Allowed values: cached (providers accessing DB and caching data in-memory), standard (OOB Apache NiFi providers without caches). Default: cached.                    |
| NIFI_REG_DB_URL                                  | Y (if NIFI_REG_USE_PGDB = true)   |         | Defines Database connection URL. URL must be compliant with PostgreSQL JDBC driver.                                                                                                                                                                    |
| NIFI_REG_DB_USERNAME                             | Y (if NIFI_REG_USE_PGDB = true)   |         | Defines username for DB connection.                                                                                                                                                                                                                    |
| NIFI_REG_DB_PASSWORD                             | Y (if NIFI_REG_USE_PGDB = true)   |         | Defines password for DB connection.                                                                                                                                                                                                                    |
| NIFI_REG_MIGRATE_TO_DB                           | N                                 |         | If set to `true` and NIFI_REG_USE_PGDB = true, then qubership-nifi-registry will migrate data from file-storage to PostgreSQL DB, if it's empty. If DB was previously migrated, then migration will be skipped.                                        |
| X_JAVA_ARGS                                      | N                                 |         | A list of additional Java startup arguments. Must be valid list of arguments separated by spaces just like in command-line.                                                                                                                            |
| CONSUL_ENABLED                                   | N                                 | false   | Defines, if Consul integration is enabled (`true`) or not (`false`)                                                                                                                                                                                    |
| CONSUL_URL                                       | Y (if CONSUL_ENABLED = true)      |         | URL to access Consul service. Must be in format: `<hostname>:<port>`.                                                                                                                                                                                  |
| CONSUL_CONFIG_JAVA_OPTIONS                       | N                                 |         | A list of additional Java startup arguments for auxiliary application used for Consul integration.                                                                                                                                                     |
| CONSUL_ACL_TOKEN                                 | N                                 |         | An access token that is used in Consul to manage permissions and security for interactions between NiFi-Registry and Consul.                                                                                                                           |
| NIFI_REGISTRY_DB_MAX_CONNS                       | N                                 | 5       | Maximum number of connections in the connection pool to the database. Defines value for `nifi.registry.db.maxConnections` property.                                                                                                                    |
| NIFI_REGISTRY_DB_DEBUG_SQL                       | N                                 | false   | Whether or not enable debug logging for SQL statements. Defines value for `nifi.registry.db.sql.debug` property.                                                                                                                                       |
| NIFI_REGISTRY_SECURITY_USER_OIDC_CONNECT_TIMEOUT | N                                 | 5 secs  | Socket Connect timeout when communicating with the OpenID Connect Provider. Defines value for `nifi.registry.security.user.oidc.connect.timeout` property.                                                                                             |
| NIFI_REGISTRY_SECURITY_USER_OIDC_READ_TIMEOUT    | N                                 | 5 secs  | Socket Read timeout when communicating with the OpenID Connect Provider. Defines value for `nifi.registry.security.user.oidc.read.timeout` property.                                                                                                   |


## Extension points

Qubership-nifi-registry Docker image has several predefined extension points that could be used to customize its
behavior without significant changes to other parts of the image.
The table below provides a list of such extension points and their description.

| Extension point        | Path                                                 | Description                                                                                                                                                                                                           |
|------------------------|------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| GetDBConnectionDetails | /opt/nifi-registry/scripts/GetDBConnectionDetails.sh | Shell script to dynamically set up DB connection details during startup. This script must set `dbUrl`, `dbUsername` and `dbPassword` environment variables to provide connection details.                             |
| Before Start           | /opt/nifi-registry/scripts/before_start.sh           | Shell script to execute any operations before qubership-nifi-registry startup.                                                                                                                                        |
| Start Extensions       | /opt/nifi-registry/scripts/start_ext.sh              | Shell script, which customizes several aspects via functions: before_args_processing (executed X_JAVA_ARGS environment variable is processed), after_java_end (executed after Java process is stopped or terminated). |

## Volumes and directories

Qubership-nifi-registry Docker image has several volumes that are used for storing data
and several directories that used for storing or injecting data.
The table below provides a list of volumes and directories and their description.

| Name                       | Type      | Path                                                     | Description                                                                                                                                                              |
|----------------------------|-----------|----------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Service configuration      | Volume    | /opt/nifi-registry/nifi-registry-current/conf            | Contains configuration files for qubership-nifi-registry startup.                                                                                                        |
| Logs                       | Volume    | /opt/nifi-registry/nifi-registry-current/logs            | Contains log files.                                                                                                                                                      |
| Run                        | Volume    | /opt/nifi-registry/nifi-registry-current/run             | Contains current nifi registry pid and status file.                                                                                                                      |
| Work                       | Volume    | /opt/nifi-registry/nifi-registry-current/work            | Contains some data used by nifi registry in runtime.                                                                                                                     |
| TLS certificates           | Directory | /tmp/tls-certs                                           | Contains TLS keystore (keystore.p12) and truststore (truststore.p12). Required for startup with AUTH = oidc, tls, ldap.                                                  |
| Configuration data         | Directory | /opt/nifi-registry/nifi-registry-current/persistent_data | Contains metadata database and flow storage, if NIFI_REG_USE_PGDB != `true`.                                                                                             |
| Cached providers extension | Directory | /opt/nifi-registry/nifi-registry-current/ext-cached      | Extension for cached access policy and user group providers. See also environment property `NIFI_REG_DB_FLOW_AUTHORIZERS` and section below dedicated to this extension. |

## Changing logging levels

You can modify logging levels by:
1. Setting `ROOT_LOG_LEVEL` environment variable. Be mindful that this variable allows you to set only root logging level;
2. Setting logging level for specific package in Consul. Consul property name must start with "logger." followed by package name. Value should be one of logging level supported by Logback: ALL, TRACE, DEBUG, INFO, WARN, ERROR, OFF. Property should be located in one of two locations:
    1. `config/${NAMESPACE}/application`, where `NAMESPACE` is a value of `NAMESPACE` environment variable, or value = `local`, if not set.
    2. `config/${NAMESPACE}/${MICROSERVICE_NAME}` or `config/${NAMESPACE}/qubership-nifi-registry`, if `MICROSERVICE_NAME` not set.

## Changing NiFi Registry configuration properties

NiFi Registry configuration properties could be set up in Consul:
1. Property name must start with `nifi.registry.`
2. Property should be located in one of two locations:
    1. `config/${NAMESPACE}/application`, where `NAMESPACE` is a value of `NAMESPACE` environment variable, or value = `local`, if not set.
    2. `config/${NAMESPACE}/${MICROSERVICE_NAME}` or `config/${NAMESPACE}/qubership-nifi-registry`, if `MICROSERVICE_NAME` not set.

To change NiFi Registry properties:
1. Change the NiFi Registry properties in Consul as per your requirements.
2. Restart qubership-nifi-registry container.

The list of properties that can be configured via Consul:
- nifi.registry.web.https.application.protocols
- nifi.registry.web.jetty.threads
- nifi.registry.web.should.send.server.version
- nifi.registry.db.maxConnections (env variable `NIFI_REGISTRY_DB_MAX_CONNS`)
- nifi.registry.db.sql.debug (env variable `NIFI_REGISTRY_DB_DEBUG_SQL`)
- nifi.registry.security.user.oidc.connect.timeout (env variable `NIFI_REGISTRY_SECURITY_USER_OIDC_CONNECT_TIMEOUT`)
- nifi.registry.security.user.oidc.read.timeout (env variable `NIFI_REGISTRY_SECURITY_USER_OIDC_READ_TIMEOUT`)
- nifi.registry.revisions.enabled
- nifi.registry.kerberos.krb5.file
- nifi.registry.kerberos.spnego.principal
- nifi.registry.kerberos.spnego.keytab.location
- nifi.registry.kerberos.spnego.authentication.expiration.

Some of these properties can also be set via environment variables.
If the same property is set via environment variable and in Consul, then the value from environment variable will take precedence.

The detailed description of all supported NiFi Registry properties is available in the Apache NiFi Registry System [Administrator's Guide](https://nifi.apache.org/docs/nifi-registry-docs/html/administration-guide.html).

## Migration from file storage

To migrate data from file-based storage to PostgreSQL DB one needs to:
1. enable DB usage for storage in environment variables: set NIFI_REG_USE_PGDB = `true`
2. set up DB connection details:
   - either via environment variables (NIFI_REG_DB_URL, NIFI_REG_DB_USERNAME, NIFI_REG_DB_PASSWORD)
   - or by extending Docker image and adding script `GetDBConnectionDetails.sh` to dynamically get connection details during startup.
3. enable data migration: set NIFI_REG_MIGRATE_TO_DB = `true`.

Once qubership-nifi-registry successfully starts, NIFI_REG_MIGRATE_TO_DB may be removed or set to `false`.
After that data that was stored on disk can be removed.

## Cached providers extension

This extension contains libraries for PostgreSQL DB-based access policy (CachedDatabaseAccessPolicyProvider) and user group (CachedDatabaseUserGroupProvider) providers with in-memory caches.

Functionality of these providers is identical to standard DatabaseAccessPolicyProvider and DatabaseUserGroupProvider.
Configuration parameters are the same as for standard DatabaseAccessPolicyProvider and DatabaseUserGroupProvider.
The differences are:
1. only PostgreSQL Database is supported
2. cached providers load in-memory cache for all entities, if bulk method (e.g. getPolicies or getUsers) is called, or only accessed entities (if single gets are used).
3. cache is hold until the next restart. The assumption is that all changes to DB are done via provider, so it can properly update the cache.
4. cached providers rely on PostgreSQL-specific SQL syntax to reduce number of SQL calls, compared with more generic approach in original Apache NiFi Registry providers.

Due to these differences cached providers may have higher performance, especially if lots of users are created in Apache NiFi Registry.

Environment variable `NIFI_REG_DB_FLOW_AUTHORIZERS` can be used to enable/disable usage of cached providers.
