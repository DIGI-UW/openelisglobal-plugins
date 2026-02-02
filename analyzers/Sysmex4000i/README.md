# Sysmex4000i Plugin

Sysmex XT-4000i hematology analyzer file-based plugin.

## Identification

- **File Pattern**: File contains `ID Instrument` column AND data contains `XT-4000i`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | FILE |
| Transport | Filesystem |
| Category | HEMATOLOGY |
| Manufacturer | Sysmex |

## Test Mappings

Supports 13 CBC parameters with 5-part differential percentages:

| Analyzer Code | OpenELIS Test | Units |
|--------------|---------------|-------|
| GB(10/uL) | White Blood Cells | 10/µL |
| GR(10^4/uL) | Red Blood Cells | 10^4/µL |
| HBG(g/L) | Hemoglobin | g/L |
| HCT(10^(-1)%) | Hematocrit | 10^(-1)% |
| VGM(10^(-1)fL) | Mean Corpuscular Volume | 10^(-1)fL |
| TCMH(10^(-1)pg) | Mean Corpuscular Hemoglobin | 10^(-1)pg |
| CCMH(g/L) | Mean Corpuscular Hemoglobin Concentration | g/L |
| PLQ(10^3/uL) | Platelets | 10^3/µL |
| NEUT%, LYMPH%, MONO%, EO%, BASO% | Differential Percentages | % |

## Sample File Format

- **Delimiter**: Comma (CSV)
- **Key Columns**:
  - ID Instrument
  - N' Echantillon (specimen number)
  - Ana. Jour (analysis date)
  - Ana. Heure (analysis time)
  - N' Rack (rack number)
  - Pos. Tube (tube position)

## Build

```bash
cd plugins/analyzers/Sysmex4000i
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
