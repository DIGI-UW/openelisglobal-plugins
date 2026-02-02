# SysmexXT4000i Plugin

Sysmex XT-4000i hematology analyzer file-based plugin.

## Identification

- **File Pattern**: File contains `ID Instrument` column AND data contains `XT`

## Protocol

| Property | Value |
|----------|-------|
| Protocol | FILE |
| Transport | Filesystem |
| Category | HEMATOLOGY |
| Manufacturer | Sysmex |

## Test Mappings

Supports CBC with 5-part differential (mappings similar to Sysmex4000i).

| Analyzer Code | OpenELIS Test | Notes |
|--------------|---------------|-------|
| Various CBC parameters | Complete Blood Count | 5-part differential |

## Sample File Format

- **Delimiter**: Comma (CSV)
- **Key Columns**:
  - ID Instrument
  - N' Echantillon
  - Ana. Jour
  - Ana. Heure
  - N' Rack
  - Pos. Tube

## Build

```bash
cd plugins/analyzers/SysmexXT4000i
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

## Notes

Test mapping details are similar to [Sysmex4000i](../Sysmex4000i/).

---

**Last Verified:** 2026-02-02
