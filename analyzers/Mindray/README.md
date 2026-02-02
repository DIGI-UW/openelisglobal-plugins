# Mindray Analyzer Plugin

Supports Mindray hematology and chemistry analyzers.

## Identification

- **HL7 MSH-3**: `MINDRAY` (sending application identifier)

## Protocol

| Property | Value |
|----------|-------|
| Protocol | HL7 v2.x |
| Transport | TCP/IP (MLLP) |
| Category | HEMATOLOGY/CHEMISTRY |
| Manufacturer | Mindray |

## Supported Models

- **BC-5380**: Hematology (CBC) over HL7
- **BS-360E**: Chemistry over HL7
- **BC-2000**: Hematology over HL7
- **BA-88A**: Chemistry over HL7/RS232

## Test Mappings

Results received via HL7 ORU^R01 messages. Test codes in OBX segments (e.g., CREA, ALT, AST) map via field mappings or plugin defaults.

Plugin registration and line-insertion logic remain here; HL7 parsing and MappingAware wrapper are in the main OpenELIS-Global repository.

## Build

```bash
cd plugins/analyzers/Mindray
mvn clean package
```

## Installation

Copy `target/Mindray-*.jar` to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

## Notes

See feature branch `feat/011-madagascar-analyzer-integration-m5-mindray-hl7` in OpenELIS-Global for integration tests and fixtures.

---

**Feature:** 011-madagascar-analyzer-integration, Milestone M5  
**Last Verified:** 2026-02-02
