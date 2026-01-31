# Hain FluoroCycler XT Analyzer Plugin

External plugin JAR for the **Hain FluoroCycler XT** PCR analyzer.

## Analyzer

- **Type**: PCR Thermocycler
- **Protocol**: File-based CSV import
- **Format**: Semicolon-delimited CSV
- **Columns**: Position, Sample ID, Result, Interpretation

## Test Mappings

| Analyzer Test Name | Default OpenELIS Test Name |
| ------------------ | -------------------------- |
| RESULT             | FluoroCycler XT Result     |
| INTERPRETATION     | FluoroCycler XT Interpretation |

> Use analyzer-test-map.csv to override mappings per site.

## Build

```bash
mvn clean package -pl ./analyzers/FluoroCyclerXT
```

## Deployment

Copy `target/FluoroCyclerXT-1.0.jar` to `/var/lib/openelis-global/plugins/`.

## Feature

011-madagascar-analyzer-integration, Milestone M13
