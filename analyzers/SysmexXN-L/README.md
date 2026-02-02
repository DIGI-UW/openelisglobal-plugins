# SysmexXN-L Plugin

Sysmex XN-L hematology analyzer ASTM interface plugin.

## Identification

- **ASTM Pattern**: H-segment field 4 contains `XN-L`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | ASTM LIS2-A2 or ASTM E1381-02 |
| Transport | RS232 Serial or TCP/IP |
| Category | HEMATOLOGY |
| Manufacturer | Sysmex |

## Test Mappings

Supports 30+ CBC parameters including extended differential and reticulocyte counts:

| Analyzer Code | OpenELIS Test | Units |
|--------------|---------------|-------|
| WBC | White Blood Cell Count | 10^3/µL |
| RBC | Red Blood Cell Count | 10^6/µL |
| HGB | Hemoglobin | g/dL |
| HCT | Hematocrit | % |
| MCV | Mean Corpuscular Volume | fL |
| MCH | Mean Corpuscular Hemoglobin | pg |
| MCHC | Mean Corpuscular Hemoglobin Concentration | g/dL |
| PLT | Platelet Count | 10^3/µL |
| NEUT_COUNT, LYMPH_COUNT, MONO_COUNT, EO_COUNT, BASO_COUNT | 5-Part Differential Counts | 10^3/µL |
| NEUT_PERCENT, LYMPH_PERCENT, MONO_PERCENT, EO_PERCENT, BASO_PERCENT | 5-Part Differential Percentages | % |
| RET_COUNT, RET_PERCENT | Reticulocyte Counts | Various |
| ... | (Additional advanced parameters) | Various |

## Message Format

- **Protocol**: ASTM LIS2-A2 or ASTM E1381-02
- **Framing**: ENQ/ACK/NAK establishment
- **Transport**: RS232 (9600 baud, 8N1) or TCP/IP

## Build

```bash
cd plugins/analyzers/SysmexXN-L
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

## Notes

Supports both RS232 and TCP/IP connectivity per vendor specifications. For file-based XN series, see [SysmexXN1000](../SysmexXN1000/).

---

**Last Verified:** 2026-02-02
