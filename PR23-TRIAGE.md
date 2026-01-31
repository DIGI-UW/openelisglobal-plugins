# PR #23 Triage Analysis

**PR**: #23 - Add/update analyzers plugins  
**Status**: Open (unmerged since 2023-03-28)  
**Mergeable**: NO (conflicts exist)  
**Files**: 68 files changed

## Executive Summary

PR #23 contains a mix of useful new analyzers, problematic site-specific changes, and outdated configurations. This document categorizes all changes and documents salvage decisions.

## Triage Categories

### SALVAGE - New Analyzers (9 analyzers)

These analyzers add new functionality and should be ported after modernization:

| Analyzer | Status | Action Required |
|----------|--------|-----------------|
| AB7500Fast | Salvage | Update to Java 21, remove Java 8 config |
| Cobas4800 | Salvage | Update to Java 21, remove Java 8 config |
| Cobas6800VL | Salvage | Update to Java 21, remove Java 8 config |
| CobasTaqMan48VL | Salvage | Update to Java 21, remove Java 8 config |
| CobasTaqMan96DBS | Salvage | Update to Java 21, remove Java 8 config |
| CobasTaqman96VL | Salvage | Update to Java 21, remove Java 8 config |
| Sysmex4000i | Evaluate | May overlap with existing SysmexXT4000i |
| SysmexKX21 | Salvage | Update to Java 21, remove Java 8 config |
| SysmexXN1000 | Salvage | Update to Java 21, remove Java 8 config |

**Modernization Checklist for Each Analyzer:**
- [ ] Update pom.xml: Remove `<source>1.8</source>` and `<target>1.8</target>`
- [ ] Use parent-managed compiler settings (Java 21)
- [ ] Remove system-scoped JAR dependencies
- [ ] Ensure Maven standard layout (`src/main/java`, `src/main/resources`)
- [ ] Run `mvn spotless:apply` for formatting
- [ ] Test build: `mvn clean install`

### SALVAGE - Bug Fixes (1 file)

| File | Issue | Fix |
|------|-------|-----|
| `analyzers/template/HttpHl7TemplateAnalyzer/src/main/java/oe/plugin/analyzer/HttpHl7TemplateAnalyzer.java` | Class name typo | `HttpHl7TemplateAnalyzerAnalyzerImplementation` → `HttpHl7TemplateAnalyzerImplementation` |

**Action**: Cherry-pick this fix immediately.

### REJECT - Site-Specific Changes (1 file)

| File | Issue | Why Reject |
|------|-------|-----------|
| `analyzers/FacsCantoII/src/oe/plugin/analyzer/FacsCantoIIImplementation.java` | Changed test names `CD3 %Parent` → `Q3 %Parent`, `CD4 %Parent` → `Q4 %Parent` | Site-specific FACSCanto II configuration. "Q3" and "Q4" are quadrant names from a specific instrument setup, not universal. |

**Action**: DO NOT merge this change.

### REJECT - Debug Code (1 file)

| File | Issue | Why Reject |
|------|-------|-----------|
| `analyzers/FacsCantoII/src/oe/plugin/analyzer/FacsCantoIIImplementation.java` | Added `System.out.println()` statements | Debug code should never be in production. |

**Action**: DO NOT merge these print statements.

### VERIFY - Permission API Changes (17 files)

All `*Permission.java` files were updated to add `SystemModuleUrl` support:

**Old Pattern:**
```java
return service.bindRoleToModule(role, module);
```

**PR #23 Pattern:**
```java
SystemModuleUrl moduleUrl = service.getOrCreateSystemModuleUrl(module, "/AnalyzerResults");
return service.bindRoleToModule(role, module, moduleUrl);
```

**Status**: VERIFY  
**Action Required**: Check if current OpenELIS-Global-2 `PluginPermissionService` has the 3-argument `bindRoleToModule(role, module, moduleUrl)` signature.

**Files Affected:**
- CobasC111Permission.java
- CobasIntegra400Permission.java
- CobasTaqMan48DBSPermission.java
- FacsCaliburPermission.java
- FacsCantoIIPermission.java
- FacsPrestoPermission.java
- FullyPermission.java
- GeneXpertAnalyzerPermission.java
- GeneXpertPermission.java (GeneXpertHL7)
- MindrayPermission.java
- QuantStudio3Permission.java
- SysmexPermission.java (Sysmex2000i)
- SysmexXTPermission.java (SysmexXT4000i)
- WeberPermission.java

**Decision**: If API exists and is recommended, port in a separate focused PR with compatibility notes. Otherwise, document as future work.

### ALREADY FIXED - OE Dependency (1 file)

| File | PR #23 Change | Current Status |
|------|---------------|----------------|
| `pom.xml` | Changed from system-scoped JAR to managed dependency | Already fixed in develop with CI workflow changes |

**Action**: No action needed.

## Implementation Plan

### Phase 1: Port Bug Fix (Immediate)
1. Cherry-pick HttpHl7TemplateAnalyzer class name fix
2. Test build
3. Commit to develop

### Phase 2: Salvage New Analyzers (High Priority)
For each analyzer in salvage list:
1. Create feature branch: `feat/pr23-salvage-{analyzer-name}`
2. Copy analyzer directory from PR #23
3. Apply modernization checklist
4. Test build
5. Commit with clear message referencing PR #23
6. Merge to develop

### Phase 3: Verify Permission Changes (Medium Priority)
1. Check `PluginPermissionService` API in OpenELIS-Global-2 develop
2. If supported: Create PR with permission updates
3. If not supported: Document in plugins/docs/future-work.md

### Phase 4: Document Rejections (Low Priority)
1. Add note to PR #23 explaining what was salvaged and what was rejected
2. Suggest closing PR #23 as "superseded by individual PRs"

## Salvage Progress Tracking

- [ ] HttpHl7TemplateAnalyzer fix ported
- [ ] AB7500Fast modernized and integrated
- [ ] Cobas4800 modernized and integrated
- [ ] Cobas6800VL modernized and integrated
- [ ] CobasTaqMan48VL modernized and integrated
- [ ] CobasTaqMan96DBS modernized and integrated
- [ ] CobasTaqman96VL modernized and integrated
- [ ] SysmexKX21 modernized and integrated
- [ ] SysmexXN1000 modernized and integrated
- [ ] Sysmex4000i evaluated (overlap check)
- [ ] Permission API changes verified and decided

## Notes

- Original PR author: Pkom17
- PR created: 2023-03-28
- Last updated: 2026-01-29
- Total commits in PR: 4
- All new analyzers use Java 8 configuration (outdated)
- No tests included for new analyzers
