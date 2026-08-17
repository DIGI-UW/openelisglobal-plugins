# AB7500Fast Plugin

Applied Biosystems 7500 Fast Real-Time PCR System plugin for viral load testing.

## Identification

- **File Pattern**: File content contains `sds7500fast`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | FILE |
| Transport | Filesystem |
| Category | MOLECULAR |
| Manufacturer | Applied Biosystems |

## Test Mappings

| Analyzer Code | OpenELIS Test | Notes |
|--------------|---------------|-------|
| Quantity | Viral Load | Viral load quantification |

## Sample File Format

- **Delimiter**: Comma (CSV)
- **Key Columns**:
  - Sample Name
  - Target Name
  - C? (value)
  - Quantity

## Build

```bash
cd plugins/analyzers/AB7500Fast
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
