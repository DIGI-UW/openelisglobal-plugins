# OpenELIS Global Analyzer Plugins

This directory contains external analyzer plugins for OpenELIS Global. Currently **35 analyzers** are supported across 30+ countries.

## Quick Links

- **[INVENTORY.md](INVENTORY.md)** - Complete structured inventory with protocol details
- **[../docs/analyzer.md](../docs/analyzer.md)** - Plugin development guide
- **[../docs/astm.md](../docs/astm.md)** - ASTM bidirectional setup guide
- **[template/](template/)** - Plugin template for new analyzers

---

## Supported Analyzers by Category

### Hematology (12 analyzers)

Complete blood count (CBC) and differential analysis.

| Analyzer | Manufacturer | Protocol | Transport |
|----------|--------------|----------|-----------|
| [HoribaMicros60](HoribaMicros60/) | Horiba ABX | ASTM | RS232 |
| [HoribaPentra60](HoribaPentra60/) | Horiba ABX | ASTM | RS232 |
| [Mindray](Mindray/) | Mindray | HL7 | TCP/IP |
| [pocH-100i](pocH-100i/) | Horiba ABX | ASTM | RS232/TCP |
| [Sysmex2000i](Sysmex2000i/) | Sysmex | FILE | Filesystem |
| [Sysmex4000i](Sysmex4000i/) | Sysmex | FILE | Filesystem |
| [SysmexKX21](SysmexKX21/) | Sysmex | FILE | Filesystem |
| [SysmeXT](SysmeXT/) | Sysmex | FILE | Filesystem |
| [SysmexXN-L](SysmexXN-L/) | Sysmex | ASTM | RS232/TCP |
| [SysmexXN1000](SysmexXN1000/) | Sysmex | FILE | Filesystem |
| [SysmexXP](SysmexXP/) | Sysmex | ASTM | RS232/TCP |
| [SysmexXT4000i](SysmexXT4000i/) | Sysmex | FILE | Filesystem |

### Molecular (9 analyzers)

PCR, viral load, and nucleic acid testing.

| Analyzer | Manufacturer | Protocol | Transport |
|----------|--------------|----------|-----------|
| [AB7500Fast](AB7500Fast/) | Applied Biosystems | FILE | Filesystem |
| [Cobas4800](Cobas4800/) | Roche | FILE | Filesystem |
| [Cobas6800VL](Cobas6800VL/) | Roche | HL7 | TCP/IP |
| [CobasTaqMan48DBS](CobasTaqMan48DBS/) | Roche | FILE | Filesystem |
| [CobasTaqMan48VL](CobasTaqMan48VL/) | Roche | FILE | Filesystem |
| [CobasTaqman96VL](CobasTaqman96VL/) | Roche | FILE | Filesystem |
| [CobasTaqMan96DBS](CobasTaqMan96DBS/) | Roche | FILE | Filesystem |
| [FluoroCyclerXT](FluoroCyclerXT/) | Hain Lifescience | FILE | Filesystem |
| [GeneXpertFile](GeneXpertFile/) | Cepheid | FILE | Filesystem |
| [GeneXpertHL7](GeneXpertHL7/) | Cepheid | HL7 | HTTP/TCP |

### Chemistry (5 analyzers)

Clinical chemistry panels (glucose, creatinine, liver enzymes, etc.)

| Analyzer | Manufacturer | Protocol | Transport |
|----------|--------------|----------|-----------|
| [CobasC111](CobasC111/) | Roche | FILE | Filesystem |
| [CobasIntegra400](CobasIntegra400/) | Roche | FILE | Filesystem |
| [Fully](Fully/) | Unknown | FILE | Filesystem |
| [Mindray](Mindray/) | Mindray | HL7 | TCP/IP |

### Flow Cytometry (4 analyzers)

CD4/CD3 counting for HIV monitoring.

| Analyzer | Manufacturer | Protocol | Transport |
|----------|--------------|----------|-----------|
| [FacsCalibur](FacsCalibur/) | BD Biosciences | FILE | Filesystem |
| [FacsCantoII](FacsCantoII/) | BD Biosciences | FILE | Filesystem |
| [FacsPresto](FacsPresto/) | BD Biosciences | FILE | Filesystem |
| [weberAnalyzer](weberAnalyzer/) | Weber | FILE | Filesystem |

### Immunology (1 analyzer)

Immunoassay testing (HIV, HBsAg, VDRL).

| Analyzer | Manufacturer | Protocol | Transport |
|----------|--------------|----------|-----------|
| [AbbottArchitect](AbbottArchitect/) | Abbott | HL7 | TCP/IP |

### Coagulation (1 analyzer)

Coagulation testing (PT, INR, APTT, Fibrinogen).

| Analyzer | Manufacturer | Protocol | Transport |
|----------|--------------|----------|-----------|
| [StagoSTart4](StagoSTart4/) | Stago | ASTM/HL7 | RS232/TCP |

### Generic/Template (2 items)

| Item | Description |
|------|-------------|
| [GenericASTM](GenericASTM/) | Database-driven ASTM plugin for dashboard-configured analyzers |
| [template](template/) | Plugin development template for creating new analyzer plugins |

---

## Supported Analyzers by Protocol

### HL7 v2.x (5 analyzers)

HL7 messages over TCP/IP with MLLP framing (0x0B start, 0x1C+0x0D end).

| Analyzer | MSH-3 Sending Application | Category |
|----------|--------------------------|----------|
| [AbbottArchitect](AbbottArchitect/) | ARCHITECT or ABBOTT | Immunology |
| [Cobas6800VL](Cobas6800VL/) | (Contains "Load Viral") | Molecular |
| [GeneXpertHL7](GeneXpertHL7/) | (HTTP servlet) | Molecular |
| [Mindray](Mindray/) | MINDRAY | Hematology/Chemistry |
| [StagoSTart4](StagoSTart4/) | STAGO | Coagulation |

### ASTM LIS2-A2 (8 analyzers)

Bidirectional laboratory instrument communication (ENQ/ACK/NAK framing).

| Analyzer | Transport | H-Segment Identification |
|----------|-----------|-------------------------|
| [GeneXpert](GeneXpert/) | RS232/TCP | Contains "GeneXpert" |
| [GenericASTM](GenericASTM/) | RS232/TCP | Database pattern matching |
| [HoribaMicros60](HoribaMicros60/) | RS232 | Contains "ABX^MICROS60" |
| [HoribaPentra60](HoribaPentra60/) | RS232 | Contains "ABX^PENTRA60" |
| [pocH-100i](pocH-100i/) | RS232/TCP | Contains "pocH-100i" |
| [StagoSTart4](StagoSTart4/) | RS232 | Contains "START4" or "STAGO" |
| [SysmexXN-L](SysmexXN-L/) | RS232/TCP | Contains "XN-L" |
| [SysmexXP](SysmexXP/) | RS232/TCP | Contains "XP-100" |

### FILE (23 analyzers)

CSV/TXT file exports monitored via filesystem watcher.

**Molecular (7):** AB7500Fast, Cobas4800, CobasTaqMan series (4), FluoroCyclerXT, GeneXpertFile

**Hematology (7):** Sysmex2000i, Sysmex4000i, SysmexKX21, SysmeXT, SysmexXN1000, SysmexXT4000i

**Chemistry (3):** CobasC111, CobasIntegra400, Fully

**Flow Cytometry (4):** FacsCalibur, FacsCantoII, FacsPresto, weberAnalyzer

**Molecular (2):** QuantStudio3

See [INVENTORY.md](INVENTORY.md) for complete file format details.

---

## Installation

### Prerequisites

- Java 21 LTS
- Maven 3.8+
- OpenELIS Global 2.x

### Building a Plugin

```bash
cd plugins/analyzers/{AnalyzerName}
mvn clean package
```

### Deploying to OpenELIS

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

1. Install and configure [ASTM-HTTP Bridge](https://github.com/DIGI-UW/astm-http-bridge)
2. Connect analyzer via RS232 or TCP/IP
3. Configure analyzer in OpenELIS Dashboard
4. Results flow bidirectionally

### HL7 Analyzers

1. Configure network settings on analyzer
2. Point analyzer to OpenELIS IP/port
3. Configure analyzer in OpenELIS Dashboard
4. Results received via HL7 ORU^R01 messages

---

## Creating New Analyzer Plugins

### Quick Start

1. Copy [template/](template/) as starting point:
   ```bash
   cp -r template/ MyAnalyzer/
   ```

2. Update plugin descriptor XML
3. Implement analyzer identification logic
4. Add test mappings
5. Build and test

### Development Guide

See comprehensive guide: [../docs/analyzer.md](../docs/analyzer.md)

### Plugin Requirements

- Analyzer interface implementation
- File/message identification logic
- Test code to OpenELIS test mappings
- Build configuration (pom.xml)
- README.md documentation

---

## Contributing

### Modifying Existing Plugins

1. **Contact Original Author**: Check plugin's `contact.txt` if present
2. **Coordinate with Core Team**: Ensure changes don't break existing deployments
3. **Backward Compatibility**: Maintain compatibility with existing configurations
4. **Update Documentation**: Update plugin's README.md

### Adding New Plugins

1. Create plugin from [template/](template/)
2. Follow [plugin development guide](../docs/analyzer.md)
3. Add comprehensive README.md
4. Submit pull request to [openelisglobal-plugins](https://github.com/openelisglobal/openelisglobal-plugins)

---

## Related Documentation

- **[INVENTORY.md](INVENTORY.md)** - Complete analyzer inventory with technical details
- **[Plugin Development Guide](../docs/analyzer.md)** - How to create analyzer plugins
- **[ASTM Setup Guide](../docs/astm.md)** - Bidirectional ASTM configuration
- **[Feature 011 Madagascar Analyzers](../specs/011-madagascar-analyzer-integration/)** - Madagascar deployment specifics

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
