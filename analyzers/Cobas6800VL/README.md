# Cobas6800VL Plugin

Roche Cobas 6800 viral load testing plugin using HL7.

## Identification

- **HL7 Pattern**: Message contains `Load Viral` AND first line contains `^MPL`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | HL7 |
| Transport | TCP/IP |
| Category | MOLECULAR |
| Manufacturer | Roche |

## Test Mappings

| Analyzer Code | OpenELIS Test | Notes |
|--------------|---------------|-------|
| Result | Viral Load | HIV viral load quantification |

## Message Format

- **Format**: HL7
- **Separator**: `L\|1\|N`
- **Accession**: OBR\|1\| segment
- **Result**: OBX\|1\| segment

## Build

```bash
cd plugins/analyzers/Cobas6800VL
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
