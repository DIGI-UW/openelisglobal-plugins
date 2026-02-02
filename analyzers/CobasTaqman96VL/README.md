# CobasTaqman96VL Plugin

Roche Cobas TaqMan 96 viral load testing plugin.

## Identification

- **File Pattern**: File contains `Test` column AND next line contains `HI2CAP96` or `IFS96CDC`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | FILE |
| Transport | Filesystem |
| Category | MOLECULAR |
| Manufacturer | Roche |

## Test Mappings

| Analyzer Code | OpenELIS Test | Notes |
|--------------|---------------|-------|
| Result | Viral Load | HIV-1 viral load quantification |

## Sample File Format

- **Delimiter**: Tab-delimited (detected from column 14)
- **Key Columns**:
  - Patient Name
  - Patient ID
  - Order Number
  - Sample ID
  - Test
  - Result

## Build

```bash
cd plugins/analyzers/CobasTaqman96VL
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
