# Horiba ABX Micros 60 Analyzer Plugin

External plugin JAR for the **Horiba ABX Micros 60** hematology analyzer.

## Analyzer

- **Type**: 3-Part Differential Hematology
- **Protocol**: ASTM LIS2-A2 over RS232 serial (9600 8N1)
- **Parameters**: 18 (CBC + 3-part differential)
- **ASTM Header ID**: `ABX^MICROS60`

## Test Mappings

### CBC (10 parameters)

| ASTM Code | OpenELIS Test     | LOINC   |
| --------- | ----------------- | ------- |
| WBC       | White Blood Cells | 6690-2  |
| RBC       | Red Blood Cells   | 789-8   |
| HGB       | Hemoglobin        | 718-7   |
| HCT       | Hematocrit        | 4544-3  |
| MCV       | MCV               | 787-2   |
| MCH       | MCH               | 785-6   |
| MCHC      | MCHC              | 786-4   |
| PLT       | Platelet Count    | 777-3   |
| RDW       | RDW               | 788-0   |
| MPV       | MPV               | 32623-1 |

### 3-Part Differential (6 parameters)

| ASTM Code | OpenELIS Test    | LOINC  |
| --------- | ---------------- | ------ |
| LYM%      | Lymphocyte %     | 736-9  |
| LYM#      | Lymphocyte Count | 731-0  |
| MXD%      | Mixed Cell %     | —      |
| MXD#      | Mixed Cell Count | —      |
| NEU%      | Neutrophil %     | 770-8  |
| NEU#      | Neutrophil Count | 751-8  |

> **Note**: MXD (Mixed cells = monocytes + eosinophils + basophils) has no
> standard LOINC code. These tests require manual mapping in the OpenELIS admin UI.

## Build

```bash
mvn clean package -pl ./analyzers/HoribaMicros60
```

## Deployment

Copy `target/HoribaMicros60-1.0.jar` to `/var/lib/openelis-global/plugins/`.

## Feature

011-madagascar-analyzer-integration, Milestone M10
