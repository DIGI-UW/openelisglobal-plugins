# {AnalyzerName} Plugin

{Brief description - 1-2 sentences}

## Identification

- **Pattern**: {File pattern or ASTM H-segment or HL7 MSH-3}

## Protocol

| Property | Value |
|----------|-------|
| Protocol | {FILE / ASTM / HL7} |
| Transport | {Filesystem / RS232 / TCP/IP} |
| Category | {HEMATOLOGY / MOLECULAR / CHEMISTRY / FLOW_CYTOMETRY / IMMUNOLOGY / COAGULATION} |
| Manufacturer | {Manufacturer name} |

## Test Mappings

| Analyzer Code | OpenELIS Test | LOINC | Units |
|--------------|---------------|-------|-------|
| {code} | {test} | {loinc} | {units} |

## Build

```bash
cd plugins/analyzers/{AnalyzerName}
mvn clean package
```

## Installation

Copy `target/{AnalyzerName}-*.jar` to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

## Notes

{Cross-references, special considerations, related plugins}

---

**Feature:** {011-madagascar-analyzer-integration, Milestone MX} (if applicable)  
**Last Verified:** {YYYY-MM-DD}
