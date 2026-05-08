---
name: nifi-registry-upgrade-skill
description: Upgrades Apache NiFi-Registry to a target version. Updates scripts, configs, Dockerfile.
---

# NiFi-Registry Upgrade Skill

## 1. Get versions
- Ask the user for **target NiFi-Registry version** and **SHA256 hash** if not provided. Don't proceed without both.
- Get current version from Dockerfile:
```bash
sed -n "s/.*NIFI_REGISTRY_VERSION='\([0-9.]*\)'.*/\1/p" ./Dockerfile | head -1
```

## 2. Fetch reference files
```bash
bash .claude/skills/nifi-registry-upgrade-skill/scripts/getFilesFromDocker.sh <CURRENT> <TARGET>
```

## 3. Sync scripts
For each file in `nifi-scripts/` (`start.sh`, `common.sh`, `update_bundle_provider.sh`, `update_flow_provider.sh`):
- Diff `upgrade-temp-data/nifi-registry-files-to-compare/scripts/<CURRENT>/<FILE>` vs `<TARGET>/<FILE>`
- Apply differences directly. If file missing in target, keep current and note it.
- Summarize changes at the end (files touched, lines +/-).

## 4. Sync logback-template.xml
Diff `logback.xml` between versions. Apply changes to `qubership-nifi-registry-consul-templates/src/main/resources/logback-template.xml`:
- **New loggers** that suppress extra log noise (level WARN/ERROR/OFF): add them to our template.
- All other changes (level tweaks on loggers not present in our template): note in the final report only, do not apply.

## 5. Sync nifi-registry.properties
Diff `nifi-registry.properties` between versions. Apply relevant changes to these files in `qubership-nifi-registry-consul-templates`:
- `nifi_registry_default.properties`
- `nifi_registry_internal.properties`
- `nifi_registry_internal_comments.properties`

## 6. Update Dockerfile
Replace `NIFI_REGISTRY_VERSION` and `NIFI_REGISTRY_VERSION_SHA256` with target values.

## 7. Update pom.xml versions
Set `<nifi.version>` to target.

## 8. Build
```bash
mvn clean install -DskipUnitTests=true -Dgpg.skip=true -q
mvn clean install 2>&1 | grep -E "BUILD|ERROR|FAIL|Tests run" | tail -20
```

## 9. Update OpenAPI specification
```bash
mvn exec:java \
  -pl qubership-nifi-registry-openapi-enricher
```
