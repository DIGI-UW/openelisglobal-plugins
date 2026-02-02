# pocH-100i Plugin

Horiba ABX pocH-100i hematology analyzer plugin with ASTM interface.

## Identification

- **ASTM Pattern**: H-segment field 4 contains `pocH-100i`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | ASTM LIS2-A2 |
| Transport | RS232 Serial or TCP/IP |
| Category | HEMATOLOGY |
| Manufacturer | Horiba ABX |

## Test Mappings

Supports 20+ CBC parameters with LOINC codes:

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
| NEUT_COUNT | Neutrophil Count | 751-8 | 10^3/µL |
| NEUT_PERCENT | Neutrophil Percentage | 770-8 | % |
| ... | (13 more differential parameters) | Various | Various |

## Message Format

- **Protocol**: ASTM LIS2-A2
- **Framing**: ENQ/ACK/NAK establishment
- **Bidirectional**: Supports AnalyzerResponder for two-way communication

## Build

```bash
cd plugins/analyzers/pocH-100i
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
