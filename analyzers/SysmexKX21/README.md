# SysmexKX21 Plugin

Sysmex KX-21 hematology analyzer file-based plugin.

## Identification

- **File Pattern**: File contains `KX21-NERG`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | FILE |
| Transport | Filesystem |
| Category | HEMATOLOGY |
| Manufacturer | Sysmex |

## Test Mappings

Supports 13 CBC parameters with 3-part differential:

| Analyzer Code | OpenELIS Test | Units |
|--------------|---------------|-------|
| GB | White Blood Cells | 10^3/µL |
| GR | Red Blood Cells | 10^6/µL |
| HB | Hemoglobin | g/dL |
| Hct | Hematocrit | % |
| VGM | Mean Corpuscular Volume | fL |
| TCMH | Mean Corpuscular Hemoglobin | pg |
| CCMH | Mean Corpuscular Hemoglobin Concentration | g/dL |
| PLT | Platelets | 10^3/µL |
| GRAN%, LYM%, MONO% | 3-Part Differential Percentages | % |
| GRAN#, LYM#, MONO# | 3-Part Differential Counts | 10^3/µL |

## Sample File Format

- **Delimiter**: Semicolon (;)
- **Key Columns**:
  - KX21
  - LYM%
  - GRAN%

## Build

```bash
cd plugins/analyzers/SysmexKX21
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
