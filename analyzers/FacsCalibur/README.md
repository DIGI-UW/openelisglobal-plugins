# FacsCalibur Plugin

BD Biosciences FACSCalibur flow cytometry analyzer plugin for CD4 counting.

## Identification

- **File Pattern**: Line 1 contains `MultiSET`

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
| CD4_ABS | Dénombrement des lymphocytes CD4 (mm3) | CD4 absolute count |
| CD4_PER | Dénombrement des lymphocytes CD4 (%) | CD4 percentage |

## Sample File Format

- **Delimiter**: Comma (CSV)
- **Key Columns**:
  - CD4_ABS
  - CD4_PER

## Build

```bash
cd plugins/analyzers/FacsCalibur
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
