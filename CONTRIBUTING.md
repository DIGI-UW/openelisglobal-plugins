# Contributing to OpenELIS Plugins

## Overview

This repository contains analyzer plugins for OpenELIS Global. Each plugin integrates a specific laboratory analyzer with the OpenELIS LIMS.

## Plugin Standards

### Required Module Layout

All plugins MUST follow the Maven standard directory layout:

```
analyzers/YourAnalyzer/
├── pom.xml
├── README.md (recommended)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── [your package structure]
│   │   └── resources/
│   │       └── [XML configs, if needed]
│   └── test/
│       └── java/
│           └── [test files]
```

### Required: Java 21

All plugins MUST use Java 21. The parent pom manages compiler configuration.

**CORRECT pom.xml:**
```xml
<parent>
  <groupId>org.openelisglobal</groupId>
  <artifactId>openelisglobal-plugins</artifactId>
  <version>1.0</version>
  <relativePath>../../pom.xml</relativePath>
</parent>
```

**DO NOT** override Java version in child modules:
```xml
<!-- ❌ WRONG - Do not add these -->
<properties>
  <maven.compiler.source>1.8</maven.compiler.source>
  <maven.compiler.target>1.8</maven.compiler.target>
</properties>
```

The parent pom configures Java 21 for all modules.

### Required: Code Formatting

Before committing, ALWAYS run:

```bash
mvn spotless:apply
```

This formats code according to Google Java Format and sorts POMs.

### Prohibited: Debug Code

Never commit debug code:

- ❌ `System.out.println()`
- ❌ `System.err.println()`
- ❌ Commented-out code blocks
- ❌ TODO comments without issue references

Use proper logging instead:

```java
import org.openelisglobal.common.log.LogEvent;

LogEvent.logDebug(this.getClass().getSimpleName(), "methodName", "Debug message");
LogEvent.logWarn(this.getClass().getSimpleName(), "methodName", "Warning message");
LogEvent.logError(this.getClass().getSimpleName(), "methodName", "Error message", exception);
```

### Prohibited: Site-Specific Configurations

Plugins MUST work generically across multiple sites. DO NOT hardcode:

- Site-specific test mappings
- Site-specific analyzer identifiers
- Site-specific result interpretations
- Site-specific units or reference ranges

If customization is needed, use configuration files or database-driven settings.

## Required: Lazy Initialization Pattern

**All plugins MUST use lazy initialization for Spring beans and services.**

This pattern allows unit testing without requiring the full Spring context and follows the framework's `AnalyzerLineInserter` base class design.

### Anti-Pattern (DO NOT USE)

```java
// ❌ WRONG - Static initializers break unit testing
static HashMap<String, Test> testNameMap = new HashMap<>();
static String ANALYZER_ID;

static {
    testNameMap.put("GLU2", SpringContext.getBean(TestService.class).getTestByName("Glucose"));
    ANALYZER_ID = SpringContext.getBean(AnalyzerService.class).getAnalyzerByName("MyAnalyzer").getId();
}
```

### Correct Pattern (USE THIS)

```java
// ✅ CORRECT - Lazy initialization allows unit testing
private TestService testService;
private String analyzerId;
private HashMap<String, Test> testNameMap;

protected TestService getTestService() {
    if (testService == null) {
        testService = SpringContext.getBean(TestService.class);
    }
    return testService;
}

protected String getAnalyzerId() {
    if (analyzerId == null) {
        Analyzer analyzer = SpringContext.getBean(AnalyzerService.class)
            .getAnalyzerByName("MyAnalyzer");
        if (analyzer != null) {
            analyzerId = analyzer.getId();
        }
    }
    return analyzerId;
}

protected HashMap<String, Test> getTestNameMap() {
    if (testNameMap == null) {
        testNameMap = new HashMap<>();
        TestService ts = getTestService();
        testNameMap.put("GLU2", ts.getTestByName("Glucose"));
    }
    return testNameMap;
}
```

**Why this matters:**
- Static initializers run at class load time, requiring Spring context
- Unit tests fail with `ExceptionInInitializerError` without Spring
- Lazy initialization defers bean lookup until runtime
- Tests can instantiate the class and verify error handling

See `analyzers/AB7500Fast` or `analyzers/SysmexKX21` for reference implementations.

---

## Plugin Architecture Patterns

### Pattern A: Legacy File-Based (Discouraged for New Plugins)

```
src/
├── oe/plugin/analyzer/
│   ├── AnalyzerName.java (main plugin)
│   ├── AnalyzerNameImplementation.java (line inserter)
│   ├── AnalyzerNameMenu.java
│   └── AnalyzerNamePermission.java
└── AnalyzerName.xml (metadata)
```

This pattern is retained for backward compatibility but NOT recommended for new plugins.

### Pattern B: Maven Standard (RECOMMENDED)

```
src/main/java/
└── org/openelisglobal/plugins/analyzer/[analyzer]/
    ├── [Analyzer]Analyzer.java
    ├── [Analyzer]AnalyzerLineInserter.java
    └── (other classes as needed)
src/main/resources/
└── [Analyzer].xml (if needed)
src/test/java/
└── (test files)
```

**Example**: See `HoribaPentra60` or `GenericASTM` for reference implementations.

### Pattern C: Generic/Dynamic (Advanced)

Use `GenericASTM` for analyzers that can be configured entirely through the OpenELIS dashboard (Feature 004/011). This avoids writing Java code for each new analyzer.

**When to use GenericASTM:**
- Analyzer follows ASTM LIS2-A2 protocol
- Test mappings can be configured via dashboard
- No custom parsing logic required

**When to write a dedicated plugin:**
- Proprietary protocol (not ASTM/HL7)
- Complex parsing rules
- Custom result transformation logic

## Writing Tests

ALL new plugins MUST include tests for parsing logic.

### Test Structure

```java
import org.junit.Test;
import org.openelisglobal.plugin.test.PluginTestBase;
import static org.junit.Assert.*;

public class MyAnalyzerLineInserterTest extends PluginTestBase {
  
  @Test
  public void testParseLine_WithValidData_ReturnsRecord() {
    MyAnalyzerLineInserter inserter = new MyAnalyzerLineInserter();
    
    MyAnalyzerLineInserter.Record record = 
        inserter.parseLine("SAMPLE-001\tHGB\t15.2\tg/dL");
    
    assertNotNull(record);
    assertEquals("SAMPLE-001", record.getSampleId());
    assertEquals("HGB", record.getTestCode());
    assertEquals("15.2", record.getValue());
  }
  
  @Test
  public void testParseLine_WithMissingData_ReturnsNull() {
    MyAnalyzerLineInserter inserter = new MyAnalyzerLineInserter();
    
    MyAnalyzerLineInserter.Record record = 
        inserter.parseLine("\t\t\t");
    
    assertNull(record);
  }
}
```

### Test Dependencies

Add to your plugin's `pom.xml`:

```xml
<dependencies>
  <dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
  </dependency>
  <dependency>
    <groupId>org.openelisglobal.plugins</groupId>
    <artifactId>test-utilities</artifactId>
    <version>1.0</version>
    <scope>test</scope>
  </dependency>
</dependencies>
```

See [README.md Testing Section](README.md#testing-plugins) for more details.

## GenericASTM Plugin

### Dependencies

The GenericASTM plugin requires OpenELIS features that currently exist only on the `demo/madagascar` branch:

- `org.openelisglobal.analyzer.service.AnalyzerConfigurationService`
- `org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration`
- `analyzer_configuration` database table
- `analyzer_test_mapping` database table

**Current Status**: CI builds against `demo/madagascar` to support GenericASTM.

**Exit Criteria**: Once Feature 004 (analyzer-management) or Feature 011 (madagascar-analyzer-integration) merges to OpenELIS develop, CI will revert to building against `develop`.

**For New Plugins**: If your plugin doesn't require these features, it will work with both `develop` and `demo/madagascar`.

## Contribution Workflow

### 1. Check Existing Plugins

Before creating a new plugin, check if your analyzer is already supported or if GenericASTM can handle it.

### 2. Create Feature Branch

```bash
git checkout develop
git pull origin develop
git checkout -b feat/analyzer-your-analyzer-name
```

### 3. Implement Plugin

Follow the [Pattern B (Maven Standard)](#pattern-b-maven-standard-recommended) structure.

### 4. Add Tests

Write tests for your parsing logic (see [Writing Tests](#writing-tests)).

### 5. Format Code

```bash
mvn spotless:apply
```

### 6. Test Build

```bash
# Build and test your plugin
mvn clean install -pl ./analyzers/YourAnalyzer

# Verify all plugins still build
mvn clean install
```

### 7. Update Parent POM

Add your module to `pom.xml`:

```xml
<modules>
  <!-- ... existing modules ... -->
  <module>./analyzers/YourAnalyzer</module>
</modules>
```

### 8. Create Pull Request

Target branch: `develop`

PR Title: `feat(analyzer): add [Analyzer Name] plugin`

PR Description should include:
- Analyzer model and manufacturer
- Protocol used (ASTM, HL7, File, etc.)
- Test results from CI
- Example input/output files (if applicable)
- Any site/region-specific context (e.g., "Used in Haiti PEPFAR sites")

## Code Quality Checklist

Before submitting PR, verify:

- [ ] Java 21 (no version override in child pom)
- [ ] Maven standard layout (`src/main/java`, `src/main/resources`)
- [ ] **Lazy initialization pattern used** (no static SpringContext calls)
- [ ] Tests included and passing (`mvn test`)
- [ ] **Tests run WITHOUT @Ignore** (lazy init enables testability)
- [ ] Code formatted (`mvn spotless:apply`)
- [ ] No debug print statements
- [ ] No site-specific hardcoded configurations
- [ ] README.md created (recommended)
- [ ] Parent pom updated with new module
- [ ] Full build passes (`mvn clean install`)

## Getting Help

- **Architecture Questions**: See [existing plugins](analyzers/) for examples
- **Testing Issues**: See [README.md Testing Section](README.md#testing-plugins)
- **CI Failures**: Check [CI workflow](.github/workflows/ci.yml) for build requirements
- **PR #23 Salvage**: See [PR23-TRIAGE.md](PR23-TRIAGE.md) for lessons learned

## Additional Resources

- [OpenELIS Global Main Repo](https://github.com/DIGI-UW/OpenELIS-Global-2)
- [Feature 004: Analyzer Management](https://github.com/DIGI-UW/OpenELIS-Global-2/tree/develop/specs/004-astm-analyzer-mapping)
- [Feature 011: Madagascar Analyzer Integration](https://github.com/DIGI-UW/OpenELIS-Global-2/tree/develop/specs/011-madagascar-analyzer-integration)
