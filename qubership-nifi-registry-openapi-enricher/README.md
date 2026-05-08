# qubership-nifi-registry-openapi-enricher

Command-line tool for enriching OpenAPI specification from Apache NiFi Registry to pass API Hub validation rules.

It loads Apache NiFi OpenAPI specification and makes the following changes:

1. adds servers, if they are missing
2. adds default descriptions (`successful operation`) for 200-202 and `default` responses
3. adds default descriptions = summary for all operations without description
4. adds tags with descriptions to specification root, if not present.

Modified OpenAPI specification is saved into a local file `openapi.json` located in the specified output directory.

## Prerequisites

- JDK 21
- Maven 3.x

## Usage

Run the tool from the repository root via the exec-maven-plugin:

```shell
mvn exec:java \
  -pl qubership-nifi-registry-openapi-enricher \
  -Dexec.args="--output-dir ./docs/openapi"
```

## Parameters

| Parameter      | Default          | Description                                   |
|----------------|------------------|-----------------------------------------------|
| `--output-dir` | `./docs/openapi` | Directory where JSON output files are written |

Parameters are optional; omit any to use the default value.

## Running tests

Unit tests:

```shell
mvn test -pl qubership-nifi-registry-openapi-enricher
```

Integration tests:

```shell
mvn verify -P tools-integration-tests -Dgpg.skip=true -DskipUnitTests=true \
    -pl qubership-nifi-registry-openapi-enricher
```
