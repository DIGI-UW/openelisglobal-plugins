# Stago STart 4 Analyzer Plugin

**Feature**: 011-madagascar-analyzer-integration  
**Milestone**: M11 (Stago STart 4)  
**Status**: ✅ Complete

## Overview

External plugin JAR for Stago STart 4 coagulation/hemostasis analyzer. Supports **dual-protocol communication**:

- **ASTM LIS2-A2** over RS232 serial connection
- **HL7 v2.5** over Network (TCP/IP)

## Analyzer Specifications

- **Manufacturer**: Stago
- **Model**: STart 4
- **Type**: Coagulation/Hemostasis Analyzer
- **Protocols**: ASTM LIS2-A2 (RS232), HL7 v2.5 (Network)
- **Tests**: PT, INR, APTT, Fibrinogen, TT

## Protocol Support

### ASTM Format

The plugin automatically detects ASTM messages by checking for H-segment headers containing "START4" or "STAGO".

**Message Structure**:
- H (Header) - Analyzer identification
- P (Patient) - Patient demographics
- O (Order) - Test order information
- R (Result) - Coagulation test results
- L (Terminator) - Message end

**Example R-segment**: `R|1|^^^PT|12.5|sec|11.0-13.5|N||F|20260128080100`

### HL7 Format

The plugin automatically detects HL7 messages by checking for MSH segments with sending application "STAGO".

**Message Structure**:
- MSH (Message Header) - Message metadata
- PID (Patient Identification) - Patient demographics
- ORC (Common Order) - Order information
- OBR (Observation Request) - Test order details
- OBX (Observation/Result) - Coagulation test results

**Example OBX segment**: `OBX|1|NM|^^^PT^PROTHROMBIN TIME||12.5|sec|||||F||||||`

## Test Mappings

The plugin maps the following coagulation tests to OpenELIS via LOINC codes:

| Analyzer Code | OpenELIS Test Name | LOINC Code |
|--------------|-------------------|------------|
| PT | Prothrombin Time | 5902-2 |
| INR | International Normalized Ratio | 6301-6 |
| APTT | Activated Partial Thromboplastin Time | 3173-2 |
| FIB | Fibrinogen | 3255-7 |
| TT | Thrombin Time | 3174-0 |

## Integration with MappingAware

The plugin automatically integrates with the MappingAware wrapper pattern (Feature 004). When field mappings are configured for the Stago STart 4 analyzer in the OpenELIS dashboard, the system automatically wraps the plugin inserter with `MappingAwareAnalyzerLineInserter` to apply field mappings before processing results.

**No plugin code changes required** - integration is handled automatically by `ASTMAnalyzerReader` and `HL7AnalyzerReader`.

## Installation

1. Build the plugin:
   ```bash
   cd plugins/analyzers/StagoSTart4
   mvn clean install
   ```

2. Deploy the JAR:
   ```bash
   cp target/StagoSTart4-1.0.jar /var/lib/openelis-global/plugins/
   ```

3. Restart OpenELIS to load the plugin.

## Testing

Unit tests are located in:
- `src/test/java/uw/edu/itech/StagoSTart4/StagoSTart4AnalyzerLineInserterTest.java`

Test fixtures are located in the main OpenELIS project:
- `src/test/resources/testdata/stago/stago-start4-coagulation.astm`
- `src/test/resources/testdata/stago/stago-start4-coagulation.hl7`

## References

- Feature Specification: `specs/011-madagascar-analyzer-integration/spec.md`
- Research Documentation: `specs/011-madagascar-analyzer-integration/research.md`
- Task Breakdown: `specs/011-madagascar-analyzer-integration/tasks.md`

## License

Mozilla Public License Version 1.1 (MPL 1.1)

Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
