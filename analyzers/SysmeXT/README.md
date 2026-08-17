# SysmeXT Plugin

Generic Sysmex XT series hematology analyzer file-based plugin.

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

Test mappings are configurable per installation site. No default mappings provided.

## Sample File Format

- **Delimiter**: Comma (CSV)
- **Key Columns**:
  - ID Instrument

## Notes

This is a generic plugin for Sysmex XT series analyzers. Test mappings should be configured based on specific analyzer model and site requirements.

## Build

```bash
cd plugins/analyzers/SysmeXT
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-02-02
