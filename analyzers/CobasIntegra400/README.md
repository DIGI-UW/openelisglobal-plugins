# CobasIntegra400 Plugin

Roche Cobas Integra 400 chemistry analyzer plugin.

## Identification

- **File Pattern**: First line contains `COBAS INTEGRA400`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | FILE |
| Transport | Filesystem |
| Category | CHEMISTRY |
| Manufacturer | Roche |

## Test Mappings

| Analyzer Code | OpenELIS Test | Notes |
|--------------|---------------|-------|
| ALTL | Transaminases ALTL | ALT/GPT measurement |
| ASTL | Transaminases ASTL | AST/GOT measurement |
| CREJ2 | Créatininémie | Creatinine measurement |
| GLU3 / GLU2 | Glycémie | Glucose measurement |

## Sample File Format

- **Delimiter**: Space-delimited
- **Key Columns**:
  - date
  - test code
  - Column 5: Accession number
  - Column 13: Result value

## Build

```bash
cd plugins/analyzers/CobasIntegra400
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
