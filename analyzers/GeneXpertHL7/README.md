# GeneXpertHL7 Plugin

Cepheid GeneXpert HL7 interface for molecular testing via HTTP servlet.

## Identification

- **Protocol**: HL7 messages via HTTP servlet
- **Note**: Disabled for flat file uploads (HTTP/HL7 only)

## Protocol

| Property | Value |
|----------|-------|
| Protocol | HL7 v2.x |
| Transport | HTTP/TCP/IP |
| Category | MOLECULAR |
| Manufacturer | Cepheid |

## Test Mappings

| Analyzer Code | OpenELIS Test | Notes |
|--------------|---------------|-------|
| HBV | HEPATITIS B VIRAL LOAD | HBV quantification |
| HCV | HEPATITIS C VIRAL LOAD | HCV quantification |
| HIV_QUAL | Xpert HIV-1 Qual | HIV-1 qualitative |
| HIV_VIRAL | HIV VIRAL LOAD | HIV-1 viral load |
| COV_2 | COVID-19 PCR | SARS-CoV-2 detection |

## Message Format

- **Format**: HL7 v2.x messages
- **Transport**: HTTP servlet endpoint
- **Custom Models**: Includes custom HL7v2.5 message types (QBP_Z03, RSP_Z02)

## Build

```bash
cd plugins/analyzers/GeneXpertHL7
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

## Notes

This plugin handles HL7-based molecular testing via HTTP. For file-based testing, see [GeneXpertFile](../GeneXpertFile/). For ASTM CBC, see [GeneXpert](../GeneXpert/).

---

**Last Verified:** 2026-02-02
