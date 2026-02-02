# GeneXpert Plugin

Cepheid GeneXpert ASTM interface plugin for hematology CBC analysis.

## Identification

- **ASTM Pattern**: H-segment field 4 contains `GeneXpert`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | ASTM LIS2-A2 |
| Transport | RS232 Serial or TCP/IP |
| Category | HEMATOLOGY |
| Manufacturer | Cepheid |

## Test Mappings

Supports 30+ CBC parameters including:

| Analyzer Code | OpenELIS Test | Notes |
|--------------|---------------|-------|
| WBC | White Blood Cell Count | 10^3/µL |
| RBC | Red Blood Cell Count | 10^6/µL |
| HGB | Hemoglobin | g/dL |
| HCT | Hematocrit | % |
| MCV | Mean Corpuscular Volume | fL |
| MCH | Mean Corpuscular Hemoglobin | pg |
| MCHC | Mean Corpuscular Hemoglobin Concentration | g/dL |
| PLT | Platelet Count | 10^3/µL |
| ... | (27 more CBC/differential parameters) | Various units |

## Message Format

- **Protocol**: ASTM LIS2-A2
- **Framing**: ENQ/ACK/NAK establishment, STX+FN+payload+ETB/ETX+checksum+CRLF
- **Identification**: H-segment field 4

## Build

```bash
cd plugins/analyzers/GeneXpert
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

## Notes

This plugin handles ASTM protocol. For file-based GeneXpert molecular testing, see [GeneXpertFile](../GeneXpertFile/). For HL7, see [GeneXpertHL7](../GeneXpertHL7/).

---

**Last Verified:** 2026-02-02
