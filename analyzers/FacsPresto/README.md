# FacsPresto Plugin

BD Biosciences FACSPresto flow cytometry analyzer plugin for CD4 counting.

## Identification

- **File Pattern**: File contains `BD FACSPresto`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | FILE |
| Transport | Filesystem |
| Category | FLOW_CYTOMETRY |
| Manufacturer | BD Biosciences |

## Test Mappings

| Analyzer Code | OpenELIS Test | Notes |
|--------------|---------------|-------|
| CD4 | CD4 absolute count | CD4 cells/µL |
| %CD4 | CD4 percentage | CD4 % of lymphocytes |

## Sample File Format

- **Delimiter**: CSV
- **Format**: BD FACSPresto export format

## Build

```bash
cd plugins/analyzers/FacsPresto
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
