# Mindray Analyzer Plugin

Supports Mindray hematology and chemistry analyzers.

## HL7 (M5 validation)

- **BC-5380**: Hematology (CBC) over HL7. Results received via HL7 ORU^R01; core HL7 adapter and identification live in OpenELIS-Global (MSH sending application `MINDRAY`).
- **BS-360E**: Chemistry over HL7. Same HL7 path; test codes (e.g. CREA, ALT, AST) in OBX map via field mappings or plugin defaults.

Plugin registration and line-insertion logic remain here; HL7 parsing and MappingAware wrapper are in the main repo. See feature branch `feat/011-madagascar-analyzer-integration-m5-mindray-hl7` in OpenELIS-Global for integration tests and fixtures.
