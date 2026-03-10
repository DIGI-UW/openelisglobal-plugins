# GenericHL7 Plugin

**Version:** 1.0.0  
**Feature:** 011-madagascar-analyzer-integration (M19)  
**Protocol:** HL7 v2.x (ORU^R01 results)  
**Type:** Generic (database-configured)

---

## Overview

Database-driven HL7 v2.x plugin that enables new analyzers to be added entirely through the Dashboard UI without writing Java code.

Unlike legacy HL7 plugins that hardcode analyzer identification and test mappings, GenericHL7:

- Builds sender identity candidates from `MSH-3` and `MSH-4`
- Matches against `analyzer_configuration.identifier_pattern` (regex)
- Loads test mappings from `analyzer_test_mapping` table

This follows the **Generic-First Architecture** established by GenericASTM (Feature 004).

---

## How It Works

### Message Flow

```
1. HL7 ORU^R01 message arrives at /analyzer/hl7 endpoint
2. HL7AnalyzerReader iterates all plugins (legacy first, then GenericHL7)
3. GenericHL7Analyzer.isTargetAnalyzer():
   - Extracts sender identity candidates from HL7 MSH fields
   - Queries analyzer_configuration for the best pattern match
   - Returns true if match found with is_generic_plugin=true
4. GenericHL7LineInserter processes results:
   - Parses OBX segments for test code/value/units
   - Looks up test mappings from analyzer_test_mapping
   - Creates AnalyzerResults for each observation
5. Results inserted into database for review
```

### Analyzer Identification

**Dynamic Sender Matching:**

```
MSH|^~\&|MINDRAY|BC-5380|OpenELIS|LAB|...|ORU^R01|MSG001|P|2.3.1
         ^^^^^^^ ^^^^^^^
           MSH-3   MSH-4
```

The plugin builds sender identity candidates such as `MINDRAY BC-5380`, `BC-5380`,
and `MINDRAY`, then matches them against:

```sql
SELECT * FROM analyzer_configuration
WHERE is_generic_plugin = true
  AND identifier_pattern ~ 'MINDRAY.*BC.?5380|BC.?5380';
```

### OBX Segment Parsing

**Format:** `OBX|1|NM|WBC||7.5|10^3/uL|4.0-11.0|N|||F`

| Field | Description         | Value      |
|-------|---------------------|------------|
| OBX-1 | Set ID              | 1          |
| OBX-2 | Value Type          | NM         |
| OBX-3 | Test Code           | WBC        |
| OBX-5 | Value               | 7.5        |
| OBX-6 | Units               | 10^3/uL    |

The inserter:
1. Extracts test code from OBX-3 (first component if multi-component)
2. Looks up mapping: `AnalyzerTestNameCache.getInstance().getTest(analyzerName, "WBC")`
3. Creates `AnalyzerResults` with mapped OpenELIS test ID

---

## Configuration

### Database Tables

**analyzer_configuration:**
```sql
INSERT INTO analyzer_configuration (id, analyzer_id, identifier_pattern, is_generic_plugin)
VALUES ('CONFIG-2012', 'ANALYZER-ID', 'MINDRAY.*BC.?2000', true);
```

**analyzer_test_mapping:**
```sql
INSERT INTO analyzer_test_mapping (analyzer_id, analyzer_test_name, test_id)
VALUES ('ANALYZER-ID', 'WBC', 'TEST-ID-WBC');
```

### Default Configurations

Pre-configured templates available in `analyzer-defaults/hl7/`:

- `mindray-bc2000.json` - Mindray BC2000 (hematology)
- `mindray-bc5380.json` - Mindray BC-5380 (hematology)
- `mindray-bs360e.json` - Mindray BS-360E (chemistry)
- `abbott-architect.json` - Abbott Architect (chemistry)
- `genexpert-hl7.json` - Cepheid GeneXpert (molecular)

Load via Dashboard: **Admin > Analyzer Management > Add Analyzer > Load Default Config**

---

## Supported Analyzers (Examples)

| Analyzer           | Identifier Pattern                 | Category   | Default Config           |
|--------------------|------------------------------------|------------|--------------------------|
| Mindray BC2000     | `MINDRAY.*BC.?2000\|BC.?2000`      | Hematology | `mindray-bc2000.json`    |
| Mindray BC-5380    | `MINDRAY.*BC.?5380\|BC.?5380`      | Hematology | `mindray-bc5380.json`    |
| Mindray BS-360E    | `MINDRAY.*BS.?360E\|BS.?360E`      | Chemistry  | `mindray-bs360e.json`    |
| Abbott Architect   | `ABBOTT.*ARCHITECT`                | Chemistry  | `abbott-architect.json`  |
| Cepheid GeneXpert  | `CEPHEID.*GENEXPERT`               | Molecular  | `genexpert-hl7.json`     |

**Note:** Any HL7 analyzer can be configured via Dashboard without code changes.

---

## Testing

### Unit Tests

```bash
cd plugins/analyzers/GenericHL7
mvn test
```

**Test Coverage:**
- `GenericHL7AnalyzerTest` - sender identity matching logic
- `GenericHL7LineInserterTest` - OBX segment parsing

### Integration Testing

```java
// Main repo: src/test/java/.../GenericHL7IntegrationTest.java
@Test
public void testBC2000HL7Import() {
    String hl7Message = 
        "MSH|^~\\&||MINDRAY||||ORU^R01|MSG001|P|2.3.1\r" +
        "PID|1|PAT123||Doe^John||19700101|M\r" +
        "OBR|1||ORDER123|PANEL^CBC|||20260202115900\r" +
        "OBX|1|NM|WBC||7.5|10^3/uL|||F\r";
    
    // Send via HL7AnalyzerReader, verify results inserted
}
```

---

## Build & Deploy

```bash
# Build plugin
cd plugins/analyzers/GenericHL7
mvn clean install

# JAR copied to: plugins/GenericHL7-1.0.jar

# Restart OpenELIS to load plugin
docker-compose restart oe.openelis.org
```

---

## Architecture

### Class Diagram

```
┌─────────────────────────────────────┐
│   AnalyzerImporterPlugin            │
│   (interface)                       │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   GenericHL7Analyzer                │
│   ────────────────────────────────  │
│   + connect(): boolean              │
│   + isTargetAnalyzer(lines): bool   │
│   + isAnalyzerResult(lines): bool   │
│   + getAnalyzerLineInserter(): ...  │
│                                     │
│   - matchedConfiguration:           │
│     ThreadLocal<Config>             │
│   - parseMsh3SendingApp(lines): str │
└─────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   GenericHL7LineInserter            │
│   ────────────────────────────────  │
│   + insert(lines, userId): boolean  │
│   + getError(): String              │
│                                     │
│   - parseObxSegment(line): Result   │
│   - extractTestCode(field): String  │
│   - parseAccessionFromOBR(line): str│
└─────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│   AnalyzerTestNameCache             │
│   (existing infrastructure)         │
│   ────────────────────────────────  │
│   + getTest(analyzer, code): Mapped │
└─────────────────────────────────────┘
```

### Thread Safety

`GenericHL7Analyzer` uses `ThreadLocal<Analyzer>` to safely store the matched analyzer between `isTargetAnalyzer()` and `getAnalyzerLineInserter()` calls, ensuring thread safety for concurrent requests.

---

## Troubleshooting

### No Match Found

**Symptom:** HL7 message not processed, "Unable to identify analyzer" error

**Causes:**
1. MSH-3 value doesn't match any `identifier_pattern`
2. Configuration has `is_generic_plugin=false`
3. Legacy plugin matched first (legacy plugins have priority)

**Solution:**
```sql
-- Check configuration
SELECT analyzer_id, identifier_pattern, is_generic_plugin
FROM analyzer_configuration
WHERE is_generic_plugin = true;

-- Update pattern if needed
UPDATE analyzer_configuration
SET identifier_pattern = 'MINDRAY.*'
WHERE id = 'CONFIG-2012';
```

### Unmapped Test Code

**Symptom:** "No mapping found for test code: XYZ" in logs

**Solution:**
```sql
-- Add mapping via Dashboard or SQL
INSERT INTO analyzer_test_mapping (analyzer_id, analyzer_test_name, test_id)
VALUES ('ANALYZER-ID', 'XYZ', (SELECT id FROM test WHERE name = 'XYZ Test'));
```

---

## Related Documentation

- [GenericASTM Plugin](../GenericASTM/README.md) - ASTM equivalent
- [Feature 011 Spec](../../../specs/011-madagascar-analyzer-integration/spec.md)
- [GenericHL7 Architecture](../../../plugins/analyzers/GenericHL7/ARCHITECTURE.md)
- [Analyzer Defaults](https://github.com/DIGI-UW/OpenELIS-Global-2/tree/develop/projects/analyzer-defaults)

---

**Maintained By:** OpenELIS Global Feature 011 Team  
**Repository:** `DIGI-UW/OpenELIS-Global-2`  
**Last Updated:** 2026-02-02
