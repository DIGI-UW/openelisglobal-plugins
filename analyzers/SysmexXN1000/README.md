# SysmexXN1000 Plugin

Sysmex XN-1000 hematology analyzer file-based plugin.

## Identification

- **File Pattern**: File contains `XN-10^23865`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | FILE |
| Transport | Filesystem |
| Category | HEMATOLOGY |
| Manufacturer | Sysmex |

## Test Mappings

Supports 12 CBC parameters with 5-part differential percentages:

| Analyzer Code | OpenELIS Test | Units |
|--------------|---------------|-------|
| WBC(10^3/uL) | White Blood Cell Count | 10^3/µL |
| RBC(10^6/uL) | Red Blood Cell Count | 10^6/µL |
| HGB(g/dL) | Hemoglobin | g/dL |
| HCT(%) | Hematocrit | % |
| MCV(fL) | Mean Corpuscular Volume | fL |
| MCH(pg) | Mean Corpuscular Hemoglobin | pg |
| MCHC(g/dL) | Mean Corpuscular Hemoglobin Concentration | g/dL |
| PLT(10^3/uL) | Platelet Count | 10^3/µL |
| NEUT%, LYMPH%, MONO%, EO%, BASO% | 5-Part Differential Percentages | % |

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
cd plugins/analyzers/SysmexXN1000
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

## Notes

For ASTM-based XN series analyzers, see [SysmexXN-L](../SysmexXN-L/).

---

**Last Verified:** 2026-02-02
