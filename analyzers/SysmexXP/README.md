# SysmexXP Plugin

Sysmex XP-100/XP-300 hematology analyzer ASTM interface plugin.

## Identification

- **ASTM Pattern**: H-segment field 4 contains `XP-100` or `XP-300`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | ASTM LIS2-A2 |
| Transport | RS232 Serial or TCP/IP |
| Category | HEMATOLOGY |
| Manufacturer | Sysmex |

## Test Mappings

Supports 30+ CBC parameters with LOINC codes including extended differential:

| Analyzer Code | OpenELIS Test | LOINC | Units |
|--------------|---------------|-------|-------|
| WBC | White Blood Cell Count | 6690-2 | 10^3/µL |
| RBC | Red Blood Cell Count | 789-8 | 10^6/µL |
| HGB | Hemoglobin | 718-7 | g/dL |
| HCT | Hematocrit | 4544-3 | % |
| MCV | Mean Corpuscular Volume | 787-2 | fL |
| MCH | Mean Corpuscular Hemoglobin | 785-6 | pg |
| MCHC | Mean Corpuscular Hemoglobin Concentration | 786-4 | g/dL |
| PLT | Platelet Count | 777-3 | 10^3/µL |
| NEUT_COUNT, LYMPH_COUNT, MONO_COUNT, EO_COUNT, BASO_COUNT | 5-Part Differential Counts | Various | 10^3/µL |
| NEUT_PERCENT, LYMPH_PERCENT, MONO_PERCENT, EO_PERCENT, BASO_PERCENT | 5-Part Differential Percentages | Various | % |
| ... | (Additional advanced parameters) | Various | Various |

## Message Format

- **Protocol**: ASTM LIS2-A2
- **Framing**: ENQ/ACK/NAK establishment
- **Bidirectional**: Supports AnalyzerResponder for two-way communication

## Build

```bash
cd plugins/analyzers/SysmexXP
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
