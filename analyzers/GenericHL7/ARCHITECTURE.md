# GenericHL7 Plugin Architecture

**Status:** 🚧 **PLANNED** - Implementation pending (Milestone M19)
**Version:** 1.0.0 (spec)
**Date:** 2026-02-02
**Feature:** specs/011-madagascar-analyzer-integration

---

## Purpose

GenericHL7 is a database-driven HL7 analyzer plugin that enables adding new HL7 analyzers without Java code changes. It follows the Generic-First Architecture pattern established by GenericASTM.

**Key Benefit**: Configure new HL7 analyzers via Dashboard using loadable default configuration templates.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│              GenericHL7 with Default Config Loading          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Dashboard: "Add Analyzer"                                   │
│         │                                                    │
│         ├──► "Load Default Config" dropdown                 │
│         │    - Mindray BC2000                                │
│         │    - Mindray BC-5380                               │
│         │    - Abbott Architect                              │
│         │    - (any HL7 analyzer)                            │
│         │                                                    │
│         ▼                                                    │
│  Populates: identifier_pattern, test_mappings, etc.         │
│         │                                                    │
│         ▼                                                    │
│  User can customize before saving                            │
│                                                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  HL7 ORU^R01 Message arrives                                │
│         │                                                    │
│         ▼                                                    │
│  HL7AnalyzerReader (Core Adapter)                           │
│         │                                                    │
│         ├──► Single code path: iterate ALL registered       │
│         │    plugins (legacy and generic in same list)      │
│         │                                                    │
│         └──► First match wins: plugin.isTargetAnalyzer()     │
│              - GenericHL7: query analyzer_configuration     │
│              - WHERE identifier_pattern MATCHES MSH-3       │
│              - AND is_generic_plugin = true                 │
│              │                                               │
│              ▼                                               │
│         GenericHL7LineInserter                              │
│         - Load field mappings from DB                        │
│         - Parse OBX segments                                 │
│         - Insert results                                     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Database Schema Additions

### analyzer_configuration Table (Liquibase 3.5.x.x/019-generic-hl7-schema.xml)

Add HL7-specific columns:

```sql
ALTER TABLE analyzer_configuration ADD COLUMN IF NOT EXISTS hl7_version VARCHAR(10);
ALTER TABLE analyzer_configuration ADD COLUMN IF NOT EXISTS msh3_pattern VARCHAR(255);
ALTER TABLE analyzer_configuration ADD COLUMN IF NOT EXISTS identifier_pattern VARCHAR(255);
ALTER TABLE analyzer_configuration ADD COLUMN IF NOT EXISTS is_generic_plugin BOOLEAN DEFAULT FALSE;
```

### analyzer_field Table (HL7-specific metadata)

Add HL7 segment/field mapping columns:

```sql
ALTER TABLE analyzer_field ADD COLUMN IF NOT EXISTS hl7_segment VARCHAR(10);
ALTER TABLE analyzer_field ADD COLUMN IF NOT EXISTS hl7_field_index INTEGER;
ALTER TABLE analyzer_field ADD COLUMN IF NOT EXISTS hl7_component INTEGER;
ALTER TABLE analyzer_field ADD COLUMN IF NOT EXISTS hl7_subcomponent INTEGER;
```

**Example:**
- `hl7_segment`: "OBX"
- `hl7_field_index`: 5 (for OBX-5 result value)
- `hl7_component`: 1 (first component if multi-part)

---

## Implementation Files

### 1. GenericHL7Analyzer.java

**Package:** `oe.plugin.analyzer`
**Extends:** N/A (implements `AnalyzerImporterPlugin`)

```java
package oe.plugin.analyzer;

import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

public class GenericHL7Analyzer implements AnalyzerImporterPlugin {

    @Override
    public boolean connect() {
        // Register plugin with PluginAnalyzerService
        // Note: Test mappings loaded from database, not hardcoded
        return true;
    }

    @Override
    public boolean isTargetAnalyzer(List<String> lines) {
        // Parse HL7 message to extract MSH-3 sending application
        String msh3 = extractMSH3(lines);

        // Query analyzer_configuration for matching pattern
        // WHERE is_generic_plugin = true
        // AND identifier_pattern MATCHES msh3

        return matchFound;
    }

    @Override
    public AnalyzerLineInserter getAnalyzerLineInserter() {
        return new GenericHL7LineInserter();
    }

    private String extractMSH3(List<String> lines) {
        // Parse MSH segment
        // Extract MSH-3 (Sending Application)
        // Handle MLLP framing (0x0B, 0x1C, 0x0D)
    }
}
```

**Key Methods:**

1. **`isTargetAnalyzer()`**:
   - Parse MSH-3 from HL7 message
   - Query `analyzer_configuration` for matching `identifier_pattern`
   - Return true if `is_generic_plugin=true` AND pattern matches

2. **`connect()`**:
   - Register plugin (no hardcoded test mappings)
   - Mappings loaded from database

3. **`getAnalyzerLineInserter()`**:
   - Return GenericHL7LineInserter instance

---

### 2. GenericHL7LineInserter.java

**Package:** `oe.plugin.analyzer`
**Extends:** `AnalyzerLineInserter`

```java
package oe.plugin.analyzer;

import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;

public class GenericHL7LineInserter extends AnalyzerLineInserter {

    @Override
    public boolean insert(List<String> lines, String currentUserId) {
        // 1. Parse HL7 ORU^R01 message
        // 2. Extract MSH-3 to identify analyzer config
        // 3. Load analyzer_configuration from database
        // 4. Load analyzer_field mappings (HL7 segment positions)
        // 5. Parse OBX segments for results
        // 6. Apply test mappings from analyzer_test_mapping
        // 7. Create AnalyzerResults objects
        // 8. Persist via persistImport()
    }

    private Map<String, Object> parseOBXSegment(String obxLine, AnalyzerField field) {
        // Parse OBX segment fields
        // Extract: OBX-3 (test identifier), OBX-5 (result value), OBX-6 (units)
        // Apply field.hl7_field_index, field.hl7_component
    }

    @Override
    public String getError() {
        return "GenericHL7 analyzer unable to write to database";
    }
}
```

**Key Responsibilities:**

1. **HL7 Message Parsing**:
   - Handle MLLP framing (0x0B start, 0x1C+0x0D end)
   - Parse MSH, PID, OBR, OBX segments
   - Extract patient ID, test identifiers, results

2. **Database-Driven Configuration**:
   - Query `analyzer_configuration` for analyzer settings
   - Load `analyzer_field` for OBX field positions
   - Apply `analyzer_test_mapping` for test code mapping

3. **Result Insertion**:
   - Create `AnalyzerResults` objects
   - Map analyzer codes to OpenELIS tests via LOINC
   - Call `persistImport()` to save results

---

### 3. pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.openelisglobal</groupId>
        <artifactId>openelisglobal-plugins</artifactId>
        <version>1.0</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <groupId>org.openelisglobal.plugins</groupId>
    <artifactId>GenericHL7</artifactId>
    <version>1.0</version>
    <packaging>jar</packaging>

    <dependencies>
        <!-- HAPI FHIR for HL7 parsing (optional, or use custom parser) -->
        <dependency>
            <groupId>ca.uhn.hapi</groupId>
            <artifactId>hapi-structures-v231</artifactId>
            <version>2.3</version>
        </dependency>

        <!-- JUnit 4 for tests -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

---

### 4. plugin.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<openElisGlobalPlugin>
    <version>1.0</version>
    <analyzerImporter>
        <extension_point path="org.openelisglobal.plugin.AnalyzerImporterPlugin">
            <extension path="oe.plugin.analyzer.GenericHL7Analyzer" />
            <description value="GenericHL7" />
        </extension_point>
    </analyzerImporter>
    <menu>
        <extension_point path="org.openelisglobal.plugin.MenuPlugin">
            <extension path="oe.plugin.analyzer.GenericHL7Menu" />
            <description value="Menu for GenericHL7" />
        </extension_point>
    </menu>
    <permission>
        <extension_point path="org.openelisglobal.plugin.PermissionPlugin">
            <extension path="oe.plugin.analyzer.GenericHL7Permission" />
            <description value="GenericHL7 Analyzer permission" />
        </extension_point>
    </permission>
</openElisGlobalPlugin>
```

---

### 5. Unit Tests

**GenericHL7AnalyzerTest.java**:

```java
@RunWith(MockitoJUnitRunner.class)
public class GenericHL7AnalyzerTest {

    @Test
    public void testIsTargetAnalyzer_WithMatchingMSH3_ReturnsTrue() {
        // Arrange: HL7 message with MSH-3 = "MINDRAY"
        // Act: Call isTargetAnalyzer()
        // Assert: Returns true (matches identifier_pattern in config)
    }

    @Test
    public void testIsTargetAnalyzer_WithNonMatchingMSH3_ReturnsFalse() {
        // Arrange: HL7 message with MSH-3 = "UNKNOWN"
        // Act: Call isTargetAnalyzer()
        // Assert: Returns false
    }
}
```

**GenericHL7LineInserterTest.java**:

```java
public class GenericHL7LineInserterTest extends PluginTestBase {

    @Test
    public void testInsert_WithValidORU_InsertsResults() {
        // Arrange: Valid HL7 ORU^R01 message
        // Act: Call insert()
        // Assert: AnalyzerResults created with correct mappings
    }
}
```

---

## HL7 Message Example

```
MSH|^~\&|MINDRAY|BC-5380|OpenELIS||20260202120000||ORU^R01|MSG00001|P|2.3.1
PID|1||12345^^^OpenELIS^PI||Doe^John||19800101|M
OBR|1||LAB-2025-001|CBC^Complete Blood Count||20260202115500
OBX|1|NM|WBC^White Blood Cells||7.5|10^3/uL|4.0-11.0|N|||F
OBX|2|NM|RBC^Red Blood Cells||4.8|10^6/uL|4.2-6.1|N|||F
OBX|3|NM|HGB^Hemoglobin||14.5|g/dL|12.0-16.0|N|||F
```

**Parsing Strategy:**

1. **MSH-3**: Extract "MINDRAY" → Match against `identifier_pattern`
2. **OBX-3**: Extract "WBC", "RBC", "HGB" → Map to test codes
3. **OBX-5**: Extract "7.5", "4.8", "14.5" → Result values
4. **OBX-6**: Extract "10^3/uL", "10^6/uL", "g/dL" → Units

---

## Configuration Example

### Database Fixture (madagascar-analyzer-test-data.xml)

```xml
<!-- CONFIG-2012: Mindray BC2000 (HL7 TCP) - USES GenericHL7 -->
<analyzer_configuration id="CONFIG-2012"
    analyzer_id="2012"
    protocol_version="HL7 v2.3.1"
    identifier_pattern="MINDRAY.*BC.?2000"
    msh3_pattern="MINDRAY"
    is_generic_plugin="true"
    status="ACTIVE"
    fhir_uuid="b0c1d2e3-f4a5-4b4c-7d8e-9f0a1b2c3d4e"
    sys_user_id="1"
    last_updated="2026-02-02 00:00:00" />
```

### Default Config Template (analyzer-defaults/hl7/mindray-bc2000.json)

```json
{
  "schema_version": "1.0",
  "analyzer_name": "Mindray BC2000",
  "identifier_pattern": "MINDRAY.*BC.?2000",
  "protocol": "HL7",
  "hl7_version": "2.3.1",
  "category": "HEMATOLOGY",
  "msh3_pattern": "MINDRAY",
  "default_test_mappings": [
    {"obx_identifier": "WBC", "test_name": "White Blood Cells", "loinc": "6690-2"}
  ]
}
```

---

## Implementation Checklist

### Phase 4.1: Core Plugin Implementation

- [ ] Create `GenericHL7Analyzer.java` with MSH-3 pattern matching
- [ ] Create `GenericHL7LineInserter.java` with OBX parsing
- [ ] Implement `isTargetAnalyzer()` with database query
- [ ] Add HL7 message parser (MLLP framing support)
- [ ] Create `pom.xml` and `plugin.xml`
- [ ] Add unit tests (JUnit 4)

### Phase 4.2: Database Schema

- [ ] Create Liquibase changeset `019-generic-hl7-schema.xml`
- [ ] Add `hl7_version`, `msh3_pattern` to `analyzer_configuration`
- [ ] Add `hl7_segment`, `hl7_field_index`, `hl7_component` to `analyzer_field`
- [ ] Test schema migration on empty database
- [ ] Test schema migration on database with existing data

### Phase 4.3: Default Config Templates

- [x] Create `analyzer-defaults/hl7/mindray-bc2000.json` ✅
- [x] Create `analyzer-defaults/hl7/mindray-bc5380.json` ✅
- [x] Create `analyzer-defaults/hl7/mindray-bs360e.json` ✅
- [x] Create `analyzer-defaults/hl7/abbott-architect.json` ✅
- [x] Create `analyzer-defaults/hl7/genexpert-hl7.json` ✅

### Phase 4.4: Dashboard Integration (M20)

- [ ] Add "Load Default Config" dropdown to `AnalyzerConfigForm.jsx`
- [ ] Create REST API endpoint: `GET /rest/analyzer/defaults`
- [ ] Create REST API endpoint: `GET /rest/analyzer/defaults/{protocol}/{name}`
- [ ] Implement form population from JSON template
- [ ] Add validation for `identifier_pattern` regex

### Phase 4.5: Testing

- [ ] Unit tests pass (JUnit 4)
- [ ] Integration test with mock HL7 server
- [ ] E2E test: Send HL7 ORU^R01 → Verify result insertion
- [ ] Test with multiple analyzers (BC2000, BC-5380, BS-360E)
- [ ] Test MSH-3 pattern matching edge cases

---

## Dependencies

### Existing Components

- **HL7AnalyzerReader** (`org.openelisglobal.analyzerimport.analyzerreaders.HL7AnalyzerReader`)
  - Core adapter that iterates through ALL registered HL7 plugins (single list)
  - No separate legacy vs generic code path; iteration order depends on registration order

- **AnalyzerLineInserter** (`org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter`)
  - Base class with `persistImport()` method

### External Libraries

- **HAPI FHIR** (optional): For HL7 message parsing
  - Alternative: Custom parser for ORU^R01 messages

---

## Testing Strategy

### 1. Unit Tests (JUnit 4)

- Test MSH-3 extraction from various HL7 formats
- Test identifier pattern matching (regex)
- Test OBX segment parsing
- Test database query for analyzer configuration

### 2. Integration Tests

- Test with real HL7 messages from Mindray BC2000
- Test with BC-5380 messages (slightly different format)
- Test that unknown MSH-3 does not match (no plugin selected); legacy and generic plugins share the same list

### 3. E2E Tests (Cypress)

- Load default config via Dashboard
- Send HL7 message from mock server
- Verify result appears in OpenELIS

---

## Migration Path

### For Existing Mindray Deployments

1. **No immediate action required** - Legacy Mindray plugin still works
2. **Optional migration**: Configure via GenericHL7 for easier test mapping
3. **Benefit**: Dashboard-configurable test codes (no plugin recompilation)

### For New Deployments

1. Use GenericHL7 with default config template
2. Load `analyzer-defaults/hl7/mindray-bc2000.json`
3. Customize test mappings via Dashboard
4. No Java code required

---

## Future Enhancements

### Phase 4+: Advanced Features

- **HL7 v2.5.1 Support**: Abbott Architect uses newer HL7 version
- **Multiple OBR Support**: Handle multi-test panels in single message
- **QC Flags**: Parse OBX-8 (Abnormal Flags) for quality control
- **Instrument Status**: Parse OBX with instrument status codes
- **Bidirectional**: Support HL7 query messages (QBP^Q11)

---

## Related Documentation

- [GenericASTM Plugin](../GenericASTM/README.md) - Similar pattern for ASTM
- [Feature 011 Spec](../../../specs/011-madagascar-analyzer-integration/spec.md)
- [Default Config Templates](../../../projects/analyzer-defaults/README.md)
- [HL7 v2.3.1 Specification](http://www.hl7.org/)

---

**Maintained By:** OpenELIS Global Feature 011 Team
**Repository:** `DIGI-UW/OpenELIS-Global-2`
**Status:** 🚧 Awaiting implementation (M19)
