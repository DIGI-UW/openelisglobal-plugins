# GenericASTM Plugin

Generic ASTM analyzer plugin with database-driven configuration.

## Identification

- **ASTM Pattern**: H-segment field 4 matches pattern from `analyzer_configuration.identifier_pattern` in database

## Protocol

| Property | Value |
|----------|-------|
| Protocol | ASTM LIS2-A2 |
| Transport | RS232 Serial or TCP/IP |
| Category | Configurable (database-driven) |
| Manufacturer | Generic (multiple vendors) |

## Test Mappings

Test mappings are **database-driven** via the `analyzer_test_mapping` table. This allows adding new analyzers via the OpenELIS Analyzer Dashboard UI without modifying Java code.

## Configuration

1. Navigate to: **Admin > Analyzer Dashboard**
2. Configure analyzer with:
   - Identifier pattern (regex for H-segment matching)
   - Test code mappings
   - Active/inactive status

## Message Format

- **Protocol**: ASTM LIS2-A2
- **Framing**: ENQ/ACK/NAK establishment, STX+FN+payload+ETB/ETX+checksum+CRLF
- **Identification**: Pattern matching from database configuration

## Build

```bash
cd plugins/analyzers/GenericASTM
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

## Notes

This plugin enables adding ASTM-compatible analyzers via UI configuration without Java code changes. Requires ASTM-HTTP Bridge for RS232 connectivity.

---

**Last Verified:** 2026-02-02
