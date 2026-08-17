# CobasC111 Plugin

Roche Cobas C111 chemistry analyzer plugin.

## Identification

- **File Pattern**: File contains `Instr` column

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
| GLU2 | Glucose | Glucose measurement |
| CREJ2 | Créatinine | Creatinine measurement |
| ALTL | Transaminases GPT (37°C) | ALT/GPT measurement |

## Sample File Format

- **Delimiter**: Semicolon (;)
- **Key Columns**:
  - Instr (instrument ID)
  - date
  - test code (GLU2/CREJ2/ALTL)
  - Column 10: Accession number
  - Column 12: Result value
  - Column 13: Units

## Build

```bash
cd plugins/analyzers/CobasC111
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
