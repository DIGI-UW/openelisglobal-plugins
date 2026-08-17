# CobasTaqMan96DBS Plugin

Roche Cobas TaqMan 96 Dried Blood Spot (DBS) DNA PCR plugin.

## Identification

- **File Pattern**: File contains `Test` column AND next line contains `HI2QLD96`

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
| Result | DNA PCR | HIV-1 DNA PCR for early infant diagnosis |

## Sample File Format

- **Delimiter**: Tab-delimited (detected from column 14)
- **Key Columns**:
  - Order Number
  - Detection Start Date/Time
  - Sample Type
  - Result

## Notes

Processes Dried Blood Spot (DBS) samples for HIV-1 DNA PCR testing. Result mapping uses GUID lookup from database.

## Build

```bash
cd plugins/analyzers/CobasTaqMan96DBS
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
