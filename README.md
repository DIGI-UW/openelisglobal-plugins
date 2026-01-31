openelisglobal-plugins
======================

Repository for plugins for openelisglobal

[![Build Status](https://github.com/openelisglobal/openelisglobal-plugins/actions/workflows/ci.yml/badge.svg)](https://github.com/openelisglobal/openelisglobal-plugins/actions/workflows/ci.yml)

## Building

1. Run the Maven Build:

    ```bash
    mvn clean install
    ```

2. Find the built plugin JARs under the `plugins` directory.

To build a single plugin:

```bash
mvn clean package -pl ./analyzers/PluginName
```

## Deployment

Copy plugin JARs to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

## Testing Plugins

### Writing Tests for Analyzer Plugins

Plugin tests that extend `AnalyzerLineInserter` require special handling because the base class attempts to access Spring beans during construction. To handle this, extend `PluginTestBase` from the `test-utilities` module:

```java
import org.junit.Test;
import org.openelisglobal.plugin.test.PluginTestBase;

public class MyAnalyzerLineInserterTest extends PluginTestBase {
  @Test
  public void testParseLine_WithValidData_ReturnsRecord() {
    MyAnalyzerLineInserter inserter = new MyAnalyzerLineInserter();
    // Test parsing logic - SpringContext is automatically mocked
  }
}
```

### Adding Test Dependencies

In your plugin's `pom.xml`:

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

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for a specific plugin
mvn test -pl ./analyzers/PluginName

# Run a specific test class
mvn test -pl ./analyzers/PluginName -Dtest=MyTestClass
```

### Test Infrastructure

The `test-utilities` module provides `PluginTestBase`, which automatically:
- Mocks `SpringContext.getBean()` to prevent NullPointerException
- Sets up and tears down mocks before/after each test
- Allows testing parsing logic without a full Spring application context

For custom service mocking, override `setupMocks()`:

```java
@Override
protected void setupMocks() {
  MyService mockService = mock(MyService.class);
  when(SpringContext.getBean(MyService.class)).thenReturn(mockService);
}
```

## Analyzer Plugins

### ASTM Protocol

| Plugin | Analyzer | Protocol | Region |
| ------ | -------- | -------- | ------ |
| GeneXpert | Cepheid GeneXpert | ASTM LIS2-A2 | PNG |
| SysmexXN-L | Sysmex XN-L Series | ASTM | — |
| SysmexXP | Sysmex XP | ASTM | — |
| pocH-100i | Sysmex pocH-100i | ASTM | Haiti |
| SysmeXT | Sysmex XT | ASTM | — |
| HoribaPentra60 | Horiba ABX Pentra 60 (5-Part Diff) | ASTM/RS232 | Madagascar |
| HoribaMicros60 | Horiba ABX Micros 60 (3-Part Diff) | ASTM/RS232 | Madagascar |

### File-Based

| Plugin | Analyzer | Format |
| ------ | -------- | ------ |
| GeneXpertFile | Cepheid GeneXpert | File |
| QuantStudio3 | QuantStudio 7 Flex | File |

### HL7 Protocol

| Plugin | Analyzer | Protocol |
| ------ | -------- | -------- |
| GeneXpertHL7 | Cepheid GeneXpert | HL7 |
| Mindray | Mindray BC-5380, BS-360E, BC2000, BA-88A | HL7/RS232 |

### Other

| Plugin | Analyzer | Protocol |
| ------ | -------- | -------- |
| CobasC111 | Cobas C111 | — |
| CobasIntegra400 | Cobas Integra 400 | — |
| CobasTaqMan48DBS | Cobas TaqMan 48 DBS | — |
| FacsCalibur | BD FACSCalibur | — |
| FacsCantoII | BD FACSCanto II | — |
| FacsPresto | BD FACSPresto | — |
| Fully | Fully Analyzer | — |
| Sysmex2000i | Sysmex 2000i | — |
| SysmexXT4000i | Sysmex XT-4000i | — |
| weberAnalyzer | Weber/Aquios CL | — |
