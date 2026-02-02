# QuantStudio3 Plugin

Applied Biosystems QuantStudio 3 Real-Time PCR System plugin for COVID-19 testing.

## Identification

- **File Pattern**: File contains `Instrument Type` AND matches regex `QuantStudio.?\s+3\s+System`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | FILE |
| Transport | Filesystem |
| Category | MOLECULAR |
| Manufacturer | Applied Biosystems |

## Test Mappings

| Analyzer Code | OpenELIS Test | LOINC | Notes |
|--------------|---------------|-------|-------|
| SARS-CoV-2 (COVID-19) RNA | SARS-CoV-2 (COVID-19) RNA [Presence] in Respiratory specimen by qRT-PCR | 94500-6 | COVID-19 detection |

## Sample File Format

- **Delimiter**: Comma (CSV)
- **Key Columns**:
  - Sample Name
  - CT (cycle threshold)
  - Ct Mean
  - Ct SD

## Build

```bash
cd plugins/analyzers/QuantStudio3
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

## Notes

Adapted from QuantStudio 3 but compatible with QuantStudio 7 Flex systems.

---

**Last Verified:** 2026-02-02
