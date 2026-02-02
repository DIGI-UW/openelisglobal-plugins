# Cobas4800 Plugin

Roche Cobas 4800 molecular system plugin for viral load and EID testing.

## Identification

- **File Pattern**: File content contains `cobas 4800`

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
| Result | Viral Load | HIV-1 viral load (VL mode) |
| Result | DNA PCR | HIV-1 qualitative DBS (EID mode) |

## Sample File Format

- **Delimiter**: Comma (CSV)
- **Key Columns**:
  - SpecimenId
  - TestType
  - Result Name
  - Value
  - AcceptedDateTime
  - SpecimenType

## Notes

Supports both Viral Load (HIV-1) and Early Infant Diagnosis (HIV-1-qual-DBS) testing modes.

## Build

```bash
cd plugins/analyzers/Cobas4800
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
