# Fully Plugin

Fully chemistry analyzer plugin.

## Identification

- **File Pattern**: File contains columns: RESULT, O.D., Well O.D., ID, Patient

## Protocol

| Property | Value |
|----------|-------|
| Protocol | FILE |
| Transport | Filesystem |
| Category | CHEMISTRY |
| Manufacturer | Unknown |

## Test Mappings

| Analyzer Code | OpenELIS Test | Notes |
|--------------|---------------|-------|
| Glucose | Glucose | Glucose measurement |
| Creatinine | Créatinine | Creatinine measurement |
| GPT/ALT | Transaminases GPT (37°C) | ALT/GPT measurement |
| Cholesterol | Cholesterol | Total cholesterol |
| Triglycerides | Triglycerides | Triglycerides |
| GOT/AST | GOT/AST | AST/GOT measurement |

## Sample File Format

- **Delimiter**: Tab-delimited
- **Key Columns**:
  - RESULT
  - O.D.
  - Well O.D.
  - ID
  - Patient

## Build

```bash
cd plugins/analyzers/Fully
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
