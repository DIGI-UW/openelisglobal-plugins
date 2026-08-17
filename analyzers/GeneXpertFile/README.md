# GeneXpertFile Plugin

Cepheid GeneXpert file-based interface for molecular testing.

## Identification

- **File Pattern**: File contains `GeneXpert Dx System` (always matches)

## Protocol

| Property | Value |
|----------|-------|
| Protocol | FILE |
| Transport | Filesystem |
| Category | MOLECULAR |
| Manufacturer | Cepheid |

## Test Mappings

| Analyzer Code | OpenELIS Test | Notes |
|--------------|---------------|-------|
| HBV | HEPATITIS B VIRAL LOAD | HBV quantification |
| HCV | HEPATITIS C VIRAL LOAD | HCV quantification |
| HIV_VIRAL | HIV VIRAL LOAD | HIV-1 viral load |
| COV_2 | COVID-19 PCR | SARS-CoV-2 detection |

## Sample File Format

- **Format**: CSV export from GeneXpert Dx System
- **Identification**: System-specific export format

## Build

```bash
cd plugins/analyzers/GeneXpertFile
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

## Notes

This plugin handles file-based molecular testing. For ASTM CBC testing, see [GeneXpert](../GeneXpert/). For HL7, see [GeneXpertHL7](../GeneXpertHL7/).

---

**Last Verified:** 2026-02-02
