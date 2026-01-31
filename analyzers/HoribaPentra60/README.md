# Horiba ABX Pentra 60 Analyzer Plugin

External plugin JAR for the **Horiba ABX Pentra 60 C+** hematology analyzer.

## Analyzer

- **Type**: 5-Part Differential Hematology
- **Protocol**: ASTM LIS2-A2 over RS232 serial (9600 8N1)
- **Parameters**: 26 (CBC + 5-part differential)
- **ASTM Header ID**: `ABX^PENTRA60`

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

### 5-Part Differential (10 parameters)

| ASTM Code | OpenELIS Test    | LOINC  |
| --------- | ---------------- | ------ |
| LYM%      | Lymphocyte %     | 736-9  |
| LYM#      | Lymphocyte Count | 731-0  |
| MON%      | Monocyte %       | 5905-5 |
| MON#      | Monocyte Count   | 742-7  |
| NEU%      | Neutrophil %     | 770-8  |
| NEU#      | Neutrophil Count | 751-8  |
| EOS%      | Eosinophil %     | 713-8  |
| EOS#      | Eosinophil Count | 711-2  |
| BAS%      | Basophil %       | 706-2  |
| BAS#      | Basophil Count   | 704-7  |

## Build

```bash
mvn clean package -pl ./analyzers/HoribaPentra60
```

## Deployment

Copy `target/HoribaPentra60-1.0.jar` to `/var/lib/openelis-global/plugins/`.

## Feature

011-madagascar-analyzer-integration, Milestone M9
