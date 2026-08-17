# weberAnalyzer Plugin

Weber flow cytometry analyzer plugin for CD3/CD4 counting.

## Identification

- **File Pattern**: Line 1 contains `MugelSET`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | FILE |
| Transport | Filesystem |
| Category | FLOW_CYTOMETRY |
| Manufacturer | Weber |

## Test Mappings

| Analyzer Code | OpenELIS Test | Notes |
|--------------|---------------|-------|
| CD4_PER | CD4 Compte en % | CD4 percentage |
| CD3_PER | CD4 Compte Absolu | CD4 absolute count |

## Sample File Format

- **Delimiter**: Comma (CSV)
- **Identification**: First line contains `MugelSET`

## Build

```bash
cd plugins/analyzers/weberAnalyzer
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

## Notes

Note: There appears to be a mapping inconsistency in the code where CD3_PER maps to "CD4 Compte Absolu" instead of CD3. This may require review.

---

**Last Verified:** 2026-02-02
