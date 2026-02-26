# OpenELIS Global Analyzer Plugins

Repository for external analyzer plugins for OpenELIS Global. Currently **35 analyzers** are supported across 30+ countries.

[![Build Status](https://github.com/openelisglobal/openelisglobal-plugins/actions/workflows/ci.yml/badge.svg)](https://github.com/openelisglobal/openelisglobal-plugins/actions/workflows/ci.yml)

---

## Quick Links

- **[INVENTORY.md](analyzers/INVENTORY.md)** - Complete structured inventory with protocol details
- **[Template](analyzers/template/)** - Plugin template for new analyzers
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Architecture standards and contribution guidelines

---

## Supported Analyzers

### By Category

#### Hematology (12 analyzers)

Complete blood count (CBC) and differential analysis.

| Analyzer | Manufacturer | Protocol | Transport |
|----------|--------------|----------|-----------|
| [HoribaMicros60](analyzers/HoribaMicros60/) | Horiba ABX | ASTM | RS232 |
| [HoribaPentra60](analyzers/HoribaPentra60/) | Horiba ABX | ASTM | RS232 |
| [Mindray](analyzers/Mindray/) | Mindray | HL7 | TCP/IP |
| [pocH-100i](analyzers/pocH-100i/) | Horiba ABX | ASTM | RS232/TCP |
| [Sysmex2000i](analyzers/Sysmex2000i/) | Sysmex | FILE | Filesystem |
| [Sysmex4000i](analyzers/Sysmex4000i/) | Sysmex | FILE | Filesystem |
| [SysmexKX21](analyzers/SysmexKX21/) | Sysmex | FILE | Filesystem |
| [SysmeXT](analyzers/SysmeXT/) | Sysmex | FILE | Filesystem |
| [SysmexXN-L](analyzers/SysmexXN-L/) | Sysmex | ASTM | RS232/TCP |
| [SysmexXN1000](analyzers/SysmexXN1000/) | Sysmex | FILE | Filesystem |
| [SysmexXP](analyzers/SysmexXP/) | Sysmex | ASTM | RS232/TCP |
| [SysmexXT4000i](analyzers/SysmexXT4000i/) | Sysmex | FILE | Filesystem |

#### Molecular (9 analyzers)

PCR, viral load, and nucleic acid testing.

| Analyzer | Manufacturer | Protocol | Transport |
|----------|--------------|----------|-----------|
| [AB7500Fast](analyzers/AB7500Fast/) | Applied Biosystems | FILE | Filesystem |
| [Cobas4800](analyzers/Cobas4800/) | Roche | FILE | Filesystem |
| [Cobas6800VL](analyzers/Cobas6800VL/) | Roche | HL7 | TCP/IP |
| [CobasTaqMan48DBS](analyzers/CobasTaqMan48DBS/) | Roche | FILE | Filesystem |
| [CobasTaqMan48VL](analyzers/CobasTaqMan48VL/) | Roche | FILE | Filesystem |
| [CobasTaqman96VL](analyzers/CobasTaqman96VL/) | Roche | FILE | Filesystem |
| [CobasTaqMan96DBS](analyzers/CobasTaqMan96DBS/) | Roche | FILE | Filesystem |
| [FluoroCyclerXT](analyzers/FluoroCyclerXT/) | Hain Lifescience | FILE | Filesystem |
| [GeneXpertFile](analyzers/GeneXpertFile/) | Cepheid | FILE | Filesystem |
| [GeneXpertHL7](analyzers/GeneXpertHL7/) | Cepheid | HL7 | HTTP/TCP |

#### Chemistry (5 analyzers)

Clinical chemistry panels (glucose, creatinine, liver enzymes, etc.)

| Analyzer | Manufacturer | Protocol | Transport |
|----------|--------------|----------|-----------|
| [CobasC111](analyzers/CobasC111/) | Roche | FILE | Filesystem |
| [CobasIntegra400](analyzers/CobasIntegra400/) | Roche | FILE | Filesystem |
| [Fully](analyzers/Fully/) | Unknown | FILE | Filesystem |
| [Mindray](analyzers/Mindray/) | Mindray | HL7 | TCP/IP |

#### Flow Cytometry (4 analyzers)

CD4/CD3 counting for HIV monitoring.

| Analyzer | Manufacturer | Protocol | Transport |
|----------|--------------|----------|-----------|
| [FacsCalibur](analyzers/FacsCalibur/) | BD Biosciences | FILE | Filesystem |
| [FacsCantoII](analyzers/FacsCantoII/) | BD Biosciences | FILE | Filesystem |
| [FacsPresto](analyzers/FacsPresto/) | BD Biosciences | FILE | Filesystem |
| [weberAnalyzer](analyzers/weberAnalyzer/) | Weber | FILE | Filesystem |

#### Immunology (1 analyzer)

Immunoassay testing (HIV, HBsAg, VDRL).

| Analyzer | Manufacturer | Protocol | Transport |
|----------|--------------|----------|-----------|
| [AbbottArchitect](analyzers/AbbottArchitect/) | Abbott | HL7 | TCP/IP |

#### Coagulation (1 analyzer)

Coagulation testing (PT, INR, APTT, Fibrinogen).

| Analyzer | Manufacturer | Protocol | Transport |
|----------|--------------|----------|-----------|
| [StagoSTart4](analyzers/StagoSTart4/) | Stago | ASTM/HL7 | RS232/TCP |

#### Generic/Template (2 items)

| Item | Description |
|------|-------------|
| [GenericASTM](analyzers/GenericASTM/) | Database-driven ASTM plugin for dashboard-configured analyzers |
| [template](analyzers/template/) | Plugin development template for creating new analyzer plugins |

---

### By Protocol

#### HL7 v2.x (5 analyzers)

HL7 messages over TCP/IP with MLLP framing (0x0B start, 0x1C+0x0D end).

| Analyzer | MSH-3 Sending Application | Category |
|----------|--------------------------|----------|
| [AbbottArchitect](analyzers/AbbottArchitect/) | ARCHITECT or ABBOTT | Immunology |
| [Cobas6800VL](analyzers/Cobas6800VL/) | (Contains "Load Viral") | Molecular |
| [GeneXpertHL7](analyzers/GeneXpertHL7/) | (HTTP servlet) | Molecular |
| [Mindray](analyzers/Mindray/) | MINDRAY | Hematology/Chemistry |
| [StagoSTart4](analyzers/StagoSTart4/) | STAGO | Coagulation |

#### ASTM LIS2-A2 (8 analyzers)

Bidirectional laboratory instrument communication (ENQ/ACK/NAK framing).

| Analyzer | Transport | H-Segment Identification |
|----------|-----------|-------------------------|
| [GeneXpert](analyzers/GeneXpert/) | RS232/TCP | Contains "GeneXpert" |
| [GenericASTM](analyzers/GenericASTM/) | RS232/TCP | Database pattern matching |
| [HoribaMicros60](analyzers/HoribaMicros60/) | RS232 | Contains "ABX^MICROS60" |
| [HoribaPentra60](analyzers/HoribaPentra60/) | RS232 | Contains "ABX^PENTRA60" |
| [pocH-100i](analyzers/pocH-100i/) | RS232/TCP | Contains "pocH-100i" |
| [StagoSTart4](analyzers/StagoSTart4/) | RS232 | Contains "START4" or "STAGO" |
| [SysmexXN-L](analyzers/SysmexXN-L/) | RS232/TCP | Contains "XN-L" |
| [SysmexXP](analyzers/SysmexXP/) | RS232/TCP | Contains "XP-100" |

#### FILE (23 analyzers)

CSV/TXT file exports monitored via filesystem watcher.

**Molecular (7):** AB7500Fast, Cobas4800, CobasTaqMan series (4), FluoroCyclerXT, GeneXpertFile

**Hematology (7):** Sysmex2000i, Sysmex4000i, SysmexKX21, SysmeXT, SysmexXN1000, SysmexXT4000i

**Chemistry (3):** CobasC111, CobasIntegra400, Fully

**Flow Cytometry (4):** FacsCalibur, FacsCantoII, FacsPresto, weberAnalyzer

**Molecular (2):** QuantStudio3

See [INVENTORY.md](analyzers/INVENTORY.md) for complete file format details.

---

## Building

### Prerequisite (required once per OpenELIS version)

This repository depends on `org.openelisglobal:openelisglobal:3.2.1.2` (classifier `classes`), which is not published to Maven Central.
Build and install OpenELIS Global first:

```bash
git clone --recurse-submodules https://github.com/DIGI-UW/OpenELIS-Global-2.git
cd OpenELIS-Global-2/dataexport && mvn clean install -DskipTests -Dmaven.test.skip=true
cd .. && mvn clean install -DskipTests -Dspotless.check.skip=true
```

Then return to this repository and build plugins.

### Build All Plugins

```bash
mvn clean install
```

### Build Single Plugin

```bash
mvn clean package -pl ./analyzers/PluginName -am
```

Use this from the repository root (recommended). `-am` also builds required local modules (for example `test-utilities`).

If you are already inside an analyzer directory, run the build from the parent aggregator:

```bash
mvn clean package -f ../../pom.xml -pl ./analyzers/PluginName -am
```

---

## Deployment

### Prerequisites

- Java 21 LTS
- Maven 3.8+
- OpenELIS Global 2.x

### Installation

1. Copy the built JAR to the plugins directory:
   ```bash
   cp target/*.jar /var/lib/openelis-global/plugins/
   ```

2. Restart OpenELIS:
   ```bash
   docker compose restart oe.openelis.org
   ```

3. Verify plugin loaded:
   - Navigate to: **Results > Analyzer**
   - Plugin should appear in the dropdown menu

---

## Using Analyzer Plugins

### File-Based Analyzers

1. Configure file import directory (if applicable)
2. Analyzer exports results to configured directory
3. OpenELIS monitors directory and imports automatically

### ASTM Analyzers

1. Install and configure [OpenELIS Analyzer Bridge](https://github.com/DIGI-UW/openelis-analyzer-bridge)
2. Connect analyzer via RS232 or TCP/IP
3. Configure analyzer in OpenELIS Dashboard
4. Results flow bidirectionally

### HL7 Analyzers

1. Configure network settings on analyzer
2. Point analyzer to OpenELIS IP/port
3. Configure analyzer in OpenELIS Dashboard
4. Results received via HL7 ORU^R01 messages

---

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

---

## Creating New Analyzer Plugins

### Quick Start

1. Copy [template/](analyzers/template/) as starting point:
   ```bash
   cp -r analyzers/template/ analyzers/MyAnalyzer/
   ```

2. Update plugin descriptor XML
3. Implement analyzer identification logic
4. Add test mappings
5. Build and test

### Plugin Requirements

- Analyzer interface implementation
- File/message identification logic
- Test code to OpenELIS test mappings
- Build configuration (pom.xml)
- README.md documentation

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Plugin architecture standards (Java 21, Maven layout, code formatting)
- **Lazy initialization pattern** (required for all Spring bean access)
- How to write and test new plugins
- Quality requirements and prohibited patterns
- Contribution workflow

### Key Architecture Requirement

All plugins **MUST use lazy initialization** for Spring beans. Static initializers that call `SpringContext.getBean()` are prohibited as they prevent unit testing. See CONTRIBUTING.md for the required pattern.

### Dynamic Configuration (GenericASTM)

GenericASTM allows analyzers to be configured entirely through the OpenELIS dashboard without writing Java code. See Feature 004 (analyzer-management) for details.

**Note**: GenericASTM requires OpenELIS features currently on `demo/madagascar` branch. The CI workflow builds against `demo/madagascar` until these features merge to `develop`.

### Modifying Existing Plugins

1. **Contact Original Author**: Check plugin's `contact.txt` if present
2. **Coordinate with Core Team**: Ensure changes don't break existing deployments
3. **Backward Compatibility**: Maintain compatibility with existing configurations
4. **Update Documentation**: Update plugin's README.md

### Adding New Plugins

1. Create plugin from [template/](analyzers/template/)
2. Follow plugin requirements above
3. Add comprehensive README.md
4. Submit pull request to [openelisglobal-plugins](https://github.com/openelisglobal/openelisglobal-plugins)

---

## Related Documentation

- **[INVENTORY.md](analyzers/INVENTORY.md)** - Complete analyzer inventory with technical details
- **[Feature 011 Madagascar Analyzers](https://github.com/DIGI-UW/OpenELIS-Global-2/tree/develop/specs/011-madagascar-analyzer-integration)** - Madagascar deployment specifics

---

## Support

- **Issues**: Report bugs via [GitHub Issues](https://github.com/DIGI-UW/OpenELIS-Global-2/issues)
- **Discussions**: Join [GitHub Discussions](https://github.com/DIGI-UW/OpenELIS-Global-2/discussions)
- **Documentation**: [OpenELIS Global Wiki](https://uwdigi.atlassian.net/wiki/spaces/OG/)

---

**Maintained By:** OpenELIS Global Community  
**Plugin Repository:** [openelisglobal-plugins](https://github.com/openelisglobal/openelisglobal-plugins)  
**Main Repository:** [OpenELIS-Global-2](https://github.com/DIGI-UW/OpenELIS-Global-2)

**Last Updated:** 2026-02-02
