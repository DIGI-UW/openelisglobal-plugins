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

## Source Files Location

PR #23 diff files are available at:
```
/home/ubuntu/.cursor/projects/home-ubuntu-OpenELIS-Global-2/pull-requests/pr-23/
├── all.diff (complete diff - 5350 lines)
├── summary.json (metadata about files)
└── diffs/ (68 individual file diffs)
```

## How to Extract Files from Diffs

**Understanding the Diff Format:**

Each diff file shows changes with these prefixes:
- `@@ NEW FILE @@` - File is completely new (entire content after this is the file)
- Lines starting with `+` - Lines to add
- Lines starting with `-` - Lines to remove (from existing files)
- Lines with no prefix - Context lines

**For NEW files (all 9 analyzers are new):**

1. Read the diff file from `/home/ubuntu/.cursor/projects/home-ubuntu-OpenELIS-Global-2/pull-requests/pr-23/diffs/`
2. Look for `@@ NEW FILE @@` marker
3. Extract all lines starting with `+` (remove the `+` prefix)
4. Those lines are the complete file content
5. Write to the target file path

**Example: Reading AB7500Fast pom.xml**

```bash
# Read the diff
cat /home/ubuntu/.cursor/projects/home-ubuntu-OpenELIS-Global-2/pull-requests/pr-23/diffs/analyzers__AB7500Fast__pom.xml.diff

# Extract content (remove + prefix from lines after @@ NEW FILE @@)
grep '^+' file.diff | sed 's/^+//'
```

**Tool-Based Approach (Recommended for Agent):**

Use the Read tool to read each diff file, then:
1. Find the `@@ NEW FILE @@` marker
2. Extract all subsequent lines starting with `+`
3. Remove the `+` prefix
4. Write to target file using Write tool

**Example Code Pattern for Agent:**

```python
# Pseudocode for extracting file content from diff
diff_content = read_file("diffs/analyzers__AB7500Fast__pom.xml.diff")
lines = diff_content.split('\n')

# Find NEW FILE marker
in_new_file = False
content_lines = []

for line in lines:
    if '@@ NEW FILE @@' in line:
        in_new_file = True
        continue
    if in_new_file and line.startswith('+'):
        # Remove + prefix and collect content
        content_lines.append(line[1:])

# Join and write
file_content = '\n'.join(content_lines)
write_file("analyzers/AB7500Fast/pom.xml", file_content)
```

## Implementation Plan

### Phase 1: Port Bug Fix ✅ COMPLETED
1. ✅ Cherry-pick HttpHl7TemplateAnalyzer class name fix
2. ✅ Test build
3. ✅ Commit to develop

### Phase 2: Salvage New Analyzers (HIGH PRIORITY - IN PROGRESS)

**For each analyzer, follow this exact workflow:**

#### Step-by-Step Process

**1. Extract Analyzer Files from PR #23**

Each analyzer in PR #23 has this structure in the diff files:
```
analyzers/{AnalyzerName}/pom.xml
analyzers/{AnalyzerName}/src/oe/plugin/analyzer/{AnalyzerName}Analyzer.java
analyzers/{AnalyzerName}/src/oe/plugin/analyzer/{AnalyzerName}AnalyzerImplementation.java
analyzers/{AnalyzerName}/src/oe/plugin/analyzer/{AnalyzerName}Menu.java
analyzers/{AnalyzerName}/src/oe/plugin/analyzer/{AnalyzerName}Permission.java
analyzers/{AnalyzerName}/src/{AnalyzerName}.xml (sometimes)
```

Read the corresponding diff files from:
```
/home/ubuntu/.cursor/projects/home-ubuntu-OpenELIS-Global-2/pull-requests/pr-23/diffs/analyzers__{AnalyzerName}__*
```

**2. Create Directory Structure**

```bash
cd /home/ubuntu/OpenELIS-Global-2/plugins
mkdir -p analyzers/{AnalyzerName}/src/oe/plugin/analyzer
# Or if Maven standard layout:
mkdir -p analyzers/{AnalyzerName}/src/main/java/oe/plugin/analyzer
mkdir -p analyzers/{AnalyzerName}/src/main/resources
```

**3. Create pom.xml (CRITICAL - Must Use Java 21)**

Create `analyzers/{AnalyzerName}/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  
  <parent>
    <groupId>org.openelisglobal</groupId>
    <artifactId>openelisglobal-plugins</artifactId>
    <version>1.0</version>
    <relativePath>../../pom.xml</relativePath>
  </parent>
  
  <groupId>org.openelisglobal.plugins</groupId>
  <artifactId>{AnalyzerName}</artifactId>
  <version>1.0</version>
  <packaging>jar</packaging>
  
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
  
  <!-- NO Java version properties - inherited from parent -->
  
  <dependencies>
    <dependency>
      <groupId>commons-logging</groupId>
      <artifactId>commons-logging</artifactId>
      <version>1.2</version>
    </dependency>
    <dependency>
      <groupId>commons-validator</groupId>
      <artifactId>commons-validator</artifactId>
      <version>1.6</version>
    </dependency>
  </dependencies>
  
  <build>
    <sourceDirectory>src</sourceDirectory>
    <!-- OR for Maven standard: -->
    <!-- <sourceDirectory>src/main/java</sourceDirectory> -->
    
    <resources>
      <resource>
        <directory>src</directory>
        <excludes>
          <exclude>**/*.java</exclude>
        </excludes>
      </resource>
    </resources>
    
    <plugins>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.11.0</version>
        <!-- NO configuration - uses parent Java 21 -->
      </plugin>
      <plugin>
        <artifactId>maven-resources-plugin</artifactId>
        <version>2.4.3</version>
        <executions>
          <execution>
            <id>copy-resources</id>
            <phase>install</phase>
            <goals>
              <goal>copy-resources</goal>
            </goals>
            <configuration>
              <outputDirectory>${project.basedir}/../../plugins</outputDirectory>
              <resources>
                <resource>
                  <directory>target/</directory>
                  <includes>
                    <include>${project.build.finalName}.jar</include>
                  </includes>
                  <filtering>false</filtering>
                </resource>
              </resources>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
  </build>
</project>
```

**CRITICAL**: DO NOT include these (they force Java 8):
```xml
<!-- ❌ WRONG - DO NOT ADD -->
<properties>
  <maven.compiler.source>1.8</maven.compiler.source>
  <maven.compiler.target>1.8</maven.compiler.target>
</properties>
<plugin>
  <configuration>
    <source>1.8</source>
    <target>1.8</target>
  </configuration>
</plugin>
```

**4. Copy Java Source Files**

Extract the Java files from PR #23 diffs and create them. The diffs show lines starting with `+` that need to be added.

**5. Update Parent pom.xml**

Edit `/home/ubuntu/OpenELIS-Global-2/plugins/pom.xml`:

Current module list ends with:
```xml
<module>./analyzers/HoribaPentra60</module>
<module>./analyzers/HoribaMicros60</module>
<module>./analyzers/GenericASTM</module>
</modules>
```

**Where to add new analyzers:**
- Insert in logical grouping (keep Cobas together, Sysmex together)
- Maintain alphabetical order within families
- Add BEFORE `GenericASTM` (keep Generic last)

**Example after adding all 9 analyzers:**
```xml
<module>./analyzers/HoribaPentra60</module>
<module>./analyzers/HoribaMicros60</module>
<!-- PR #23 salvaged analyzers (in logical groups) -->
<module>./analyzers/AB7500Fast</module>
<module>./analyzers/Cobas4800</module>
<module>./analyzers/Cobas6800VL</module>
<module>./analyzers/CobasTaqMan48VL</module>
<module>./analyzers/CobasTaqMan96DBS</module>
<module>./analyzers/CobasTaqman96VL</module>
<module>./analyzers/Sysmex4000i</module>
<module>./analyzers/SysmexKX21</module>
<module>./analyzers/SysmexXN1000</module>
<module>./analyzers/GenericASTM</module>
</modules>
```

**6. Format Code**

```bash
cd /home/ubuntu/OpenELIS-Global-2/plugins
mvn spotless:apply -pl ./analyzers/{AnalyzerName}
```

**7. Test Build**

```bash
# Build just this analyzer
mvn clean install -pl ./analyzers/{AnalyzerName} -am

# Verify full build still works
mvn clean install -DskipTests -Dmaven.test.skip=true
```

**8. Commit**

```bash
cd /home/ubuntu/OpenELIS-Global-2/plugins
git add analyzers/{AnalyzerName} pom.xml
git commit -m "feat(analyzer): add {AnalyzerName} plugin from PR #23

Ported {AnalyzerName} analyzer plugin from PR #23.

Modernization changes:
- Updated to Java 21 (removed Java 8 configuration)
- Uses parent-managed compiler settings
- Formatted with spotless
- Verified build passes

Original PR: #23 (https://github.com/DIGI-UW/openelisglobal-plugins/pull/23)
See PR23-TRIAGE.md for complete salvage analysis."
```

**9. Verify Integration**

```bash
# Check that the JAR was created
ls -lh plugins/{AnalyzerName}*.jar

# Verify no compilation errors
mvn clean compile
```

### Phase 3: Verify Permission Changes (Medium Priority)

**Context**: PR #23 updated all Permission.java files to add `SystemModuleUrl` support.

**Verification Steps:**

1. Check OpenELIS-Global-2 `PluginPermissionService` interface:
```bash
# From main OpenELIS repo
grep -n "bindRoleToModule" src/main/java/org/openelisglobal/common/services/PluginPermissionService.java
```

2. Look for the 3-argument signature:
```java
boolean bindRoleToModule(Role role, SystemModule module, SystemModuleUrl url);
```

3. Decision tree:
   - **If signature exists**: Create branch `feat/pr23-salvage-permissions`, port all permission updates, test thoroughly
   - **If signature doesn't exist**: Skip permission changes, document in CONTRIBUTING.md as future work when API is available

**Files requiring permission updates (if we proceed):**
All `*Permission.java` files in existing analyzers (17 total) - see list above in VERIFY section.

### Phase 4: Document Rejections (Low Priority)
1. Add note to PR #23 explaining what was salvaged and what was rejected
2. Suggest closing PR #23 as "superseded by individual PRs"

## Analyzer-Specific Details

### AB7500Fast
**PR #23 Files:**
```
analyzers__AB7500Fast__pom.xml.diff
analyzers__AB7500Fast__src__oe__plugin__analyzer__AB7500VLAnalyzer.java.diff
analyzers__AB7500Fast__src__oe__plugin__analyzer__AB7500VLAnalyzerImplementation.java.diff
analyzers__AB7500Fast__src__oe__plugin__analyzer__AB7500VLMenu.java.diff
analyzers__AB7500Fast__src__oe__plugin__analyzer__AB7500VLPermission.java.diff
```
**Purpose:** Applied Biosystems 7500 Fast viral load analyzer
**Protocol:** File-based

### Cobas4800
**PR #23 Files:**
```
analyzers__Cobas4800__pom.xml.diff
analyzers__Cobas4800__src__Cobas4800.xml.diff
analyzers__Cobas4800__src__oe__plugin__analyzer__Cobas4800Analyzer.java.diff
analyzers__Cobas4800__src__oe__plugin__analyzer__Cobas4800AnalyzerImplementation.java.diff
analyzers__Cobas4800__src__oe__plugin__analyzer__Cobas4800Menu.java.diff
analyzers__Cobas4800__src__oe__plugin__analyzer__Cobas4800Permission.java.diff
```
**Purpose:** Cobas 4800 VL/EID analyzer
**Protocol:** File-based
**Note:** Includes XML metadata file

### Cobas6800VL
**PR #23 Files:**
```
analyzers__Cobas6800VL__pom.xml.diff
analyzers__Cobas6800VL__src__Cobas6800.xml.diff
analyzers__Cobas6800VL__src__oe__plugin__analyzer__Cobas6800VLAnalyzer.java.diff
analyzers__Cobas6800VL__src__oe__plugin__analyzer__Cobas6800VLAnalyzerImplementation.java.diff
analyzers__Cobas6800VL__src__oe__plugin__analyzer__Cobas6800VLMenu.java.diff
analyzers__Cobas6800VL__src__oe__plugin__analyzer__Cobas6800VLPermission.java.diff
```
**Purpose:** Cobas 6800 viral load analyzer
**Protocol:** File-based
**Note:** Includes XML metadata file

### CobasTaqMan48VL
**PR #23 Files:**
```
analyzers__CobasTaqMan48VL__pom.xml.diff
analyzers__CobasTaqMan48VL__src__oe__plugin__analyzer__TaqMan48VLAnalyzer.java.diff
analyzers__CobasTaqMan48VL__src__oe__plugin__analyzer__TaqMan48VLAnalyzerImplementation.java.diff
analyzers__CobasTaqMan48VL__src__oe__plugin__analyzer__TaqMan48VLMenu.java.diff
analyzers__CobasTaqMan48VL__src__oe__plugin__analyzer__TaqMan48VLPermission.java.diff
```
**Purpose:** Cobas TaqMan 48 viral load analyzer
**Protocol:** File-based
**Note:** Extends existing CobasTaqMan48DBS functionality

### CobasTaqMan96DBS
**PR #23 Files:**
```
analyzers__CobasTaqMan96DBS__pom.xml.diff
analyzers__CobasTaqMan96DBS__src__oe__plugin__analyzer__TaqMan96DBSAnalyzer.java.diff
analyzers__CobasTaqMan96DBS__src__oe__plugin__analyzer__TaqMan96DBSAnalyzerImplementation.java.diff
analyzers__CobasTaqMan96DBS__src__oe__plugin__analyzer__TaqMan96DBSMenu.java.diff
analyzers__CobasTaqMan96DBS__src__oe__plugin__analyzer__TaqMan96DBSPermission.java.diff
```
**Purpose:** Cobas TaqMan 96 DBS (dried blood spot) analyzer
**Protocol:** File-based
**Note:** New capacity variant

### CobasTaqman96VL
**PR #23 Files:**
```
analyzers__CobasTaqman96VL__pom.xml.diff
analyzers__CobasTaqman96VL__src__oe__plugin__analyzer__TaqMan96VLAnalyzer.java.diff
analyzers__CobasTaqman96VL__src__oe__plugin__analyzer__TaqMan96VLAnalyzerImplementation.java.diff
analyzers__CobasTaqman96VL__src__oe__plugin__analyzer__TaqMan96VLMenu.java.diff
analyzers__CobasTaqman96VL__src__oe__plugin__analyzer__TaqMan96VLPermission.java.diff
```
**Purpose:** Cobas TaqMan 96 viral load analyzer
**Protocol:** File-based
**Note:** Higher throughput version

### SysmexKX21
**PR #23 Files:**
```
analyzers__SysmexKX21__pom.xml.diff
analyzers__SysmexKX21__src__oe__plugin__analyzer__SysmexKX21Analyzer.java.diff
analyzers__SysmexKX21__src__oe__plugin__analyzer__SysmexKX21AnalyzerImplementation.java.diff
analyzers__SysmexKX21__src__oe__plugin__analyzer__SysmexKX21Menu.java.diff
analyzers__SysmexKX21__src__oe__plugin__analyzer__SysmexKX21Permission.java.diff
```
**Purpose:** Sysmex KX-21 hematology analyzer
**Protocol:** File-based

### SysmexXN1000
**PR #23 Files:**
```
analyzers__SysmexXN1000__pom.xml.diff
analyzers__SysmexXN1000__src__SysmexXN1000.xml.diff
analyzers__SysmexXN1000__src__oe__plugin__analyzer__SysmexXN1000Analyzer.java.diff
analyzers__SysmexXN1000__src__oe__plugin__analyzer__SysmexXN1000AnalyzerImplementation.java.diff
analyzers__SysmexXN1000__src__oe__plugin__analyzer__SysmexXN1000Menu.java.diff
analyzers__SysmexXN1000__src__oe__plugin__analyzer__SysmexXN1000Permission.java.diff
```
**Purpose:** Sysmex XN-1000 hematology analyzer
**Protocol:** File-based
**Note:** Includes XML metadata file

### Sysmex4000i
**PR #23 Files:**
```
analyzers__Sysmex4000i__pom.xml.diff
analyzers__Sysmex4000i__src__oe__plugin__analyzer__SysmexXTAnalyzer.java.diff
analyzers__Sysmex4000i__src__oe__plugin__analyzer__SysmexXTAnalyzerImplementation.java.diff
analyzers__Sysmex4000i__src__oe__plugin__analyzer__SysmexXTMenu.java.diff
analyzers__Sysmex4000i__src__oe__plugin__analyzer__SysmexXTPermission.java.diff
```
**Purpose:** Sysmex XT-4000i hematology analyzer
**Protocol:** File-based
**⚠️ CAUTION:** Check for overlap with existing `SysmexXT4000i` module before porting

## Salvage Progress Tracking

- [x] HttpHl7TemplateAnalyzer fix ported ✅
- [ ] SysmexKX21 modernized and integrated
- [ ] SysmexXN1000 modernized and integrated
- [ ] AB7500Fast modernized and integrated
- [ ] CobasTaqMan48VL modernized and integrated
- [ ] CobasTaqMan96DBS modernized and integrated
- [ ] CobasTaqman96VL modernized and integrated
- [ ] Cobas4800 modernized and integrated
- [ ] Cobas6800VL modernized and integrated
- [ ] Sysmex4000i evaluated (overlap check with existing SysmexXT4000i)
- [ ] Permission API changes verified and decided

## Acceptance Criteria

**After completing all analyzer ports, verify:**

1. **All 9 analyzers build successfully:**
   ```bash
   mvn clean install -DskipTests -Dmaven.test.skip=true
   ```
   Should show SUCCESS for all modules

2. **All JARs created:**
   ```bash
   ls -1 plugins/*.jar | wc -l
   ```
   Should show 31 JARs total (22 existing + 9 new)

3. **No Java 8 references:**
   ```bash
   grep -r "source>1.8\|target>1.8" analyzers/
   ```
   Should return no results

4. **Parent pom updated:**
   ```bash
   grep -c "module>./analyzers/" pom.xml
   ```
   Should show 32 (23 existing + 1 test-utilities + 9 new - 1 for GenericASTM counted separately)

5. **Git status clean:**
   ```bash
   git status
   ```
   Should show commits but no uncommitted changes

6. **All commits follow convention:**
   ```bash
   git log --oneline -9
   ```
   Each should start with `feat(analyzer): add {AnalyzerName}`

**Final Report Format:**

```markdown
## PR #23 Salvage Complete

✅ 9 analyzers ported successfully
✅ 1 bug fix applied
❌ 2 changes rejected (site-specific + debug code)
⏸️ 17 permission changes deferred pending API verification

All new plugins build successfully and produce expected JAR artifacts.
Ready for CI validation.
```

## Verification Commands

**After porting each analyzer:**

```bash
# 1. Verify the analyzer module was added to parent pom
grep -n "module>./analyzers/{AnalyzerName}" pom.xml

# 2. Check Java version is NOT overridden (should return nothing)
grep -r "maven.compiler.source>1.8" analyzers/{AnalyzerName}/

# 3. Test build
mvn clean install -pl ./analyzers/{AnalyzerName} -am

# 4. Verify JAR was created
ls -lh plugins/{AnalyzerName}*.jar

# 5. Test full build
mvn clean install -DskipTests -Dmaven.test.skip=true
```

**Expected Outcomes:**
- ✅ No compilation errors
- ✅ JAR file created in `plugins/` directory
- ✅ No Java 8 references in pom.xml
- ✅ Code formatted (Spotless should show no changes)

## Common Issues and Solutions

### Issue: "package org.openelisglobal.X does not exist"
**Cause:** Missing dependency on OpenELIS classes  
**Solution:** Ensure parent pom dependency on `openelisglobal` artifact is present and OE_REF is set to `demo/madagascar` in CI

### Issue: "source value 1.8 is obsolete"
**Cause:** Java 8 configuration in child pom  
**Solution:** Remove `<maven.compiler.source>1.8</maven.compiler.source>` and `<maven.compiler.target>1.8</maven.compiler.target>` from child pom

### Issue: Spotless formatting changes files
**Cause:** PR #23 files are not formatted according to current standards  
**Solution:** This is expected - run `mvn spotless:apply` and commit the formatted version

### Issue: "Child module X does not exist"
**Cause:** Module added to parent pom.xml but directory not created  
**Solution:** Ensure analyzer directory exists before adding to parent pom

## Complete Example: Porting SysmexKX21

This is a complete walkthrough for porting one analyzer. Repeat for each analyzer.

### Step 1: Read Source Files

```bash
# Read all SysmexKX21 diffs from PR #23
read /home/ubuntu/.cursor/projects/home-ubuntu-OpenELIS-Global-2/pull-requests/pr-23/diffs/analyzers__SysmexKX21__pom.xml.diff
read /home/ubuntu/.cursor/projects/home-ubuntu-OpenELIS-Global-2/pull-requests/pr-23/diffs/analyzers__SysmexKX21__src__oe__plugin__analyzer__SysmexKX21Analyzer.java.diff
read /home/ubuntu/.cursor/projects/home-ubuntu-OpenELIS-Global-2/pull-requests/pr-23/diffs/analyzers__SysmexKX21__src__oe__plugin__analyzer__SysmexKX21AnalyzerImplementation.java.diff
read /home/ubuntu/.cursor/projects/home-ubuntu-OpenELIS-Global-2/pull-requests/pr-23/diffs/analyzers__SysmexKX21__src__oe__plugin__analyzer__SysmexKX21Menu.java.diff
read /home/ubuntu/.cursor/projects/home-ubuntu-OpenELIS-Global-2/pull-requests/pr-23/diffs/analyzers__SysmexKX21__src__oe__plugin__analyzer__SysmexKX21Permission.java.diff
```

### Step 2: Create Directory Structure

```bash
cd /home/ubuntu/OpenELIS-Global-2/plugins
mkdir -p analyzers/SysmexKX21/src/oe/plugin/analyzer
```

### Step 3: Create Modernized pom.xml

Write to `analyzers/SysmexKX21/pom.xml` using the template above.

**CRITICAL CHANGES from PR #23 pom:**
- ❌ Remove: `<source>1.8</source>` and `<target>1.8</target>`
- ✅ Keep: Parent reference, dependencies, resource plugin

### Step 4: Extract and Write Java Files

For each `.java.diff` file:
1. Extract content after `@@ NEW FILE @@`
2. Remove `+` prefix from each line
3. Write to corresponding path:
   - `SysmexKX21Analyzer.java` → `analyzers/SysmexKX21/src/oe/plugin/analyzer/SysmexKX21Analyzer.java`
   - `SysmexKX21AnalyzerImplementation.java` → `analyzers/SysmexKX21/src/oe/plugin/analyzer/SysmexKX21AnalyzerImplementation.java`
   - `SysmexKX21Menu.java` → `analyzers/SysmexKX21/src/oe/plugin/analyzer/SysmexKX21Menu.java`
   - `SysmexKX21Permission.java` → `analyzers/SysmexKX21/src/oe/plugin/analyzer/SysmexKX21Permission.java`

### Step 5: Update Parent pom.xml

Edit `/home/ubuntu/OpenELIS-Global-2/plugins/pom.xml`:

Add after existing Sysmex modules:
```xml
<module>./analyzers/SysmexKX21</module>
```

### Step 6: Format

```bash
mvn spotless:apply -pl ./analyzers/SysmexKX21
```

### Step 7: Test Build

```bash
mvn clean install -pl ./analyzers/SysmexKX21 -am
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: ~5s
```

### Step 8: Verify JAR Created

```bash
ls -lh plugins/SysmexKX21*.jar
```

Should show: `plugins/SysmexKX21-1.0.jar`

### Step 9: Commit

```bash
git add analyzers/SysmexKX21 pom.xml
git commit -m "feat(analyzer): add SysmexKX21 plugin from PR #23

Ported Sysmex KX-21 hematology analyzer plugin from PR #23.

Modernization changes:
- Updated to Java 21 (removed Java 8 configuration)
- Uses parent-managed compiler settings
- Formatted with spotless
- Verified build passes

Original PR: #23 (https://github.com/DIGI-UW/openelisglobal-plugins/pull/23)
See PR23-TRIAGE.md for complete salvage analysis."
```

## Workflow Summary for Background Agent

**Recommended Order (from simplest to most complex):**

1. **SysmexKX21** (simplest - straightforward hematology analyzer)
2. **SysmexXN1000** (similar to KX21, includes XML)
3. **AB7500Fast** (viral load, simple structure)
4. **CobasTaqMan48VL** (extends existing TaqMan family)
5. **CobasTaqMan96DBS** (similar to 48VL)
6. **CobasTaqman96VL** (similar to 96DBS)
7. **Cobas4800** (includes XML metadata)
8. **Cobas6800VL** (similar to 4800)
9. **Sysmex4000i** (LAST - requires overlap evaluation with existing SysmexXT4000i)

**For each analyzer:**
1. Read corresponding diff files from PR #23 cache
2. Extract file content (remove `+` prefix from NEW FILE sections)
3. Create directory structure
4. Write files with modernized pom.xml (Java 21, no version override)
5. Update parent pom.xml
6. Run `mvn spotless:apply`
7. Test build
8. Commit
9. Move to next analyzer

**Time Estimate:** 10-15 minutes per analyzer × 9 analyzers = ~2 hours total

## Critical: What NOT to Copy from PR #23

When extracting files, DO NOT include these from PR #23:

### 1. Java 8 Compiler Configuration
```xml
<!-- ❌ DO NOT COPY from PR #23 pom.xml -->
<properties>
  <maven.compiler.source>1.8</maven.compiler.source>
  <maven.compiler.target>1.8</maven.compiler.target>
</properties>

<!-- OR in plugin configuration -->
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.8.0</version>
  <configuration>
    <source>1.8</source>
    <target>1.8</target>
  </configuration>
</plugin>
```

### 2. System-Scoped Dependencies
```xml
<!-- ❌ DO NOT COPY - Already handled by parent -->
<dependency>
  <groupId>org.openelisglobal</groupId>
  <artifactId>openelisglobal</artifactId>
  <version>2.7.3.9</version>
  <scope>system</scope>
  <systemPath>/path/to/jar</systemPath>
</dependency>
```

### 3. Outdated Version References
- PR #23 references `openelisglobal-2.7.3.9`
- Current version is `3.2.1.2`
- Parent pom handles this - don't override in child

## Branch Strategy

**Current Branch:** `develop`  
**Target:** Commit each analyzer directly to `develop` branch

**Alternative (if you want to batch):**
```bash
# Create a salvage branch for all analyzers
git checkout -b feat/pr23-salvage-all-analyzers
# Port all 9 analyzers
# Create single PR to develop
```

**Per-analyzer branches (more granular review):**
```bash
# For each analyzer
git checkout develop
git checkout -b feat/pr23-salvage-{analyzer-name}
# Port one analyzer
# Create PR to develop
# After merge, repeat for next
```

**Recommendation:** Commit directly to `develop` for faster integration, since:
- Each analyzer is independent
- Triage already completed
- Changes are pre-approved via this document
- Build validation happens per-analyzer

## Notes

- Original PR author: Pkom17
- PR created: 2023-03-28
- Last updated: 2026-01-29
- Total commits in PR: 4
- All new analyzers use Java 8 configuration (outdated)
- No tests included for new analyzers
- PR #23 changes reference OpenELIS version 2.7.3.9 (current is 3.2.1.2)
