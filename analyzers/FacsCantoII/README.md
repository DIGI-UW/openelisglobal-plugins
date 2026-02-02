# FacsCantoII Plugin

BD Biosciences FACSCanto II flow cytometry analyzer plugin for CD3/CD4 percentage counting.

## Identification

- **File Pattern**: Line 1 contains `TRITEST`

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
| CD3_PER | CD3 percentage count | CD3 percentage |
| CD4_PER | CD4 percentage count | CD4 percentage |

## Sample File Format

- **Delimiter**: Comma (CSV)
- **Key Columns**:
  - CD3 %Parent
  - CD4 %Parent

## Build

```bash
cd plugins/analyzers/FacsCantoII
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
