# Plugin Submodule PR Instructions

## Stago STart 4 Plugin (M11)

**Main Repository Branch**: `feat/011-madagascar-analyzer-integration-m11-stago`  
**Plugin Repository Branch**: `feat/011-madagascar-analyzer-integration-m11-stago`  
**Plugin Repository**: https://github.com/DIGI-UW/openelisglobal-plugins.git

## Changes Made

### New Plugin Module
- `analyzers/StagoSTart4/` - Complete Stago STart 4 coagulation analyzer plugin
  - Dual-protocol support (ASTM + HL7)
  - Test mappings for PT, INR, APTT, FIB, TT
  - Comprehensive unit tests

### Updated Files
- `pom.xml` - Added StagoSTart4 module to parent POM

## Creating the Plugin PR

### Step 1: Commit Changes in Plugin Repository

```bash
cd plugins
git checkout feat/011-madagascar-analyzer-integration-m11-stago
git add analyzers/StagoSTart4/ pom.xml
git commit -m "feat(011-m11): Add Stago STart 4 analyzer plugin

- Add StagoSTart4Analyzer plugin class with dual-protocol support
- Add StagoSTart4AnalyzerLineInserter with ASTM/HL7 parsing
- Add comprehensive unit tests (23 test methods)
- Add test fixtures for ASTM and HL7 messages
- Update parent POM to include StagoSTart4 module

Feature: 011-madagascar-analyzer-integration
Milestone: M11"
```

### Step 2: Push Branch

```bash
git push origin feat/011-madagascar-analyzer-integration-m11-stago
```

### Step 3: Create PR in Plugin Repository

1. Go to: https://github.com/DIGI-UW/openelisglobal-plugins
2. Create PR: `feat/011-madagascar-analyzer-integration-m11-stago` → `develop`
3. Title: `feat(011-m11): Add Stago STart 4 analyzer plugin`
4. Description: See PR template below

## PR Description Template

```markdown
## Overview

Adds Stago STart 4 coagulation analyzer plugin with dual-protocol support (ASTM LIS2-A2 and HL7 v2.5).

**Feature**: 011-madagascar-analyzer-integration  
**Milestone**: M11  
**Related PR**: [Main repo PR link]

## Changes

### New Plugin Module
- `analyzers/StagoSTart4/` - Complete plugin implementation
  - `StagoSTart4Analyzer.java` - Plugin class with dual-protocol identification
  - `StagoSTart4AnalyzerLineInserter.java` - Result parser supporting ASTM and HL7
  - Comprehensive unit tests (23 test methods)
  - Test fixtures for both protocols

### Updated Files
- `pom.xml` - Added StagoSTart4 module

## Protocol Support

- **ASTM LIS2-A2**: RS232 serial communication
- **HL7 v2.5**: Network (TCP/IP) communication
- Automatic protocol detection

## Test Mappings

| Analyzer Code | OpenELIS Test Name | LOINC Code |
|--------------|-------------------|------------|
| PT | Prothrombin Time | 5902-2 |
| INR | International Normalized Ratio | 6301-6 |
| APTT | Activated Partial Thromboplastin Time | 3173-2 |
| FIB | Fibrinogen | 3255-7 |
| TT | Thrombin Time | 3174-0 |

## Testing

- ✅ Unit tests: 23 test methods covering identification, parsing, and error handling
- ✅ Build verification: Compiles and packages successfully
- ⚠️ Integration tests: To be added in main OpenELIS repository

## Build Verification

```bash
cd analyzers/StagoSTart4
mvn clean package
# SUCCESS: JAR created (9.6KB)
```

## References

- Feature Spec: `specs/011-madagascar-analyzer-integration/spec.md`
- Tasks: `specs/011-madagascar-analyzer-integration/tasks.md` (T185-T193)
- Plugin README: `analyzers/StagoSTart4/README.md`
```

## Important Notes

1. **Parallel PRs Required**: 
   - Main repo PR: OpenELIS-Global-2 changes (test fixtures, documentation)
   - Plugin repo PR: Plugin implementation (this PR)

2. **Branch Naming**: Matches main repo branch name for consistency

3. **Target Branch**: Both PRs target `develop` branch

4. **Dependencies**: Plugin PR can be merged independently, but main repo PR references plugin changes
