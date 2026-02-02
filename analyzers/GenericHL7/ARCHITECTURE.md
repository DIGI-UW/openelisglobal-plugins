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
│         ├──► Legacy Plugins iterate first (backward compat) │
│         │                                                    │
│         └──► GenericHL7Plugin.isTargetAnalyzer()            │
│              - Query analyzer_configuration                  │
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

## Related Documentation

- [GenericASTM Plugin](../GenericASTM/README.md) - Similar pattern for ASTM
- [Feature 011 Spec](../../../specs/011-madagascar-analyzer-integration/spec.md)
- [Default Config Templates](../../../analyzer-defaults/README.md)

---

**Maintained By:** OpenELIS Global Feature 011 Team  
**Repository:** `DIGI-UW/OpenELIS-Global-2`  
**Status:** 🚧 Awaiting implementation (M19)
