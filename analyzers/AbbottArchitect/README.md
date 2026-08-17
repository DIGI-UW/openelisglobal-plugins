# Abbott Architect Plugin

External analyzer plugin for Abbott Architect immunoassay systems (HL7 ORU^R01).

## Identification

- HL7 MSH-3 (Sending Application) contains `ARCHITECT` (fallback: `ABBOTT`)

## Test Mapping

Default mappings are registered on startup and can be overridden via the analyzer
test mapping CSV or UI configuration:

- `HIV`
- `HBSAG`
- `VDRL`

## Build

From the repository root:

```bash
cd analyzers/AbbottArchitect
mvn clean package
```

The built JAR is copied to `plugins/` by the module build (see
`analyzers/AbbottArchitect/pom.xml`).
