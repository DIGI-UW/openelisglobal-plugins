# Erba Hematology Plugin

Plugin for **Erba Mannheim hematology analyzers** (H360, H560, Elite 580).

---

## Identification

Messages are identified based on HL7 MSH segment fields:

- **MSH-4 (Sending Facility):** `Erba`
- **MSH-3 (Sending Application):** `H360`, `H560`, or `ELite 580`

---

## Protocol

| Property     | Value              |
|--------------|--------------------|
| Protocol     | HL7 v2.3.1         |
| Transport    | TCP/IP (MLLP)      |
| Category     | Hematology         |
| Manufacturer | Erba Mannheim      |

---

## Test Mappings

### WBC

| Analyzer Code | OBX-3 ID | Test Name             | LOINC   | Encoding | Models          |
|---------------|----------|-----------------------|---------|----------|-----------------|
| WBC           | 6690-2   | White Blood Cells     | 6690-2  | LN       | All             |

### WBC Differential — 5-part % (CBC+DIFF mode)

| Analyzer Code | OBX-3 ID | Test Name          | LOINC  | Encoding | Models     |
|---------------|----------|--------------------|--------|----------|------------|
| NEU%          | 770-8    | Neutrophils %      | 770-8  | LN       | ELITE 580  |
| LYM%          | 736-9    | Lymphocytes %      | 736-9  | LN       | ELITE 580  |
| MON%          | 5905-5   | Monocytes %        | 5905-5 | LN       | ELITE 580  |
| EOS%          | 713-8    | Eosinophils %      | 713-8  | LN       | ELITE 580  |
| BAS%          | 706-2    | Basophils %        | 706-2  | LN       | ELITE 580  |

### WBC Differential — 5-part # (CBC+DIFF mode)

| Analyzer Code | OBX-3 ID | Test Name             | LOINC  | Encoding | Models     |
|---------------|----------|-----------------------|--------|----------|------------|
| NEU#          | 751-8    | Neutrophils Absolute  | 751-8  | LN       | ELITE 580  |
| LYM#          | 731-0    | Lymphocytes Absolute  | 731-0  | LN       | ELITE 580  |
| MON#          | 742-7    | Monocytes Absolute    | 742-7  | LN       | ELITE 580  |
| EOS#          | 711-2    | Eosinophils Absolute  | 711-2  | LN       | ELITE 580  |
| BAS#          | 704-7    | Basophils Absolute    | 704-7  | LN       | ELITE 580  |

### WBC Differential — 3-part (CBC+3DIFF mode)

GRAN = granulocytes (NEU + EOS + BAS combined); MID = mid-range cells (MON + atypical lymphocytes combined).

| Analyzer Code | OBX-3 ID | Test Name                  | LOINC   | Encoding | Models |
|---------------|----------|----------------------------|---------|----------|--------|
| GRAN%         | 20482-6  | Granulocytes %             | 20482-6 | LN       | H360   |
| MID%          | 32155-4  | Mid-range Cells %          | 32155-4 | LN       | H360   |
| GRAN#         | 19023-1  | Granulocytes Absolute      | 19023-1 | LN       | H360   |
| MID#          | 32154-7  | Mid-range Cells Absolute   | 32154-7 | LN       | H360   |

### RBC Series

| Analyzer Code | OBX-3 ID | Test Name                                 | LOINC   | Encoding | Models |
|---------------|----------|-------------------------------------------|---------|----------|--------|
| RBC           | 789-8    | Red Blood Cells                           | 789-8   | LN       | All    |
| HGB           | 718-7    | Hemoglobin                                | 718-7   | LN       | All    |
| HCT           | 4544-3   | Hematocrit                                | 4544-3  | LN       | All    |
| MCV           | 787-2    | Mean Corpuscular Volume                   | 787-2   | LN       | All    |
| MCH           | 785-6    | Mean Corpuscular Hemoglobin               | 785-6   | LN       | All    |
| MCHC          | 786-4    | Mean Corpuscular Hemoglobin Concentration | 786-4   | LN       | All    |
| RDW-CV        | 788-0    | RDW-CV                                    | 788-0   | LN       | All    |
| RDW-SD        | 21000-5  | RDW-SD                                    | 21000-5 | LN       | All    |

### RBC Derived Indices

| Analyzer Code | OBX-3 ID | Test Name          | LOINC | Encoding | Models |
|---------------|----------|--------------------|-------|----------|--------|
| *Mentzr       | 11092    | Mentzer Index      | 11092 | 99MRC    | All    |
| *RDWI         | 11093    | RDW Index          | 11093 | 99MRC    | All    |

### Platelet Series

| Analyzer Code | OBX-3 ID | Test Name                       | LOINC   | Encoding | Models |
|---------------|----------|---------------------------------|---------|----------|--------|
| PLT           | 777-3    | Platelets                       | 777-3   | LN       | All    |
| MPV           | 32623-1  | Mean Platelet Volume            | 32623-1 | LN       | All    |
| PDW           | 32207-3  | Platelet Distribution Width     | 32207-3 | LN       | All    |
| PDW-SD        | 32207-3  | Platelet Distribution Width SD  | 32207-3 | LN       | All    |
| PDW-CV        | 11090    | Platelet Distribution Width CV  | 11090   | 99MRC    | All    |
| PCT           | 11003    | Plateletcrit                    | 11003   | 99MRC    | All    |
| P-LCR         | 48386-7  | Platelet Large Cell Ratio       | 48386-7 | LN       | All    |
| P-LCC         | 34167-7  | Platelet Large Cell Count       | 34167-7 | LN       | All    |

### Extended Parameters — Atypical / Large Immature Cells

| Analyzer Code | OBX-3 ID | Test Name                       | LOINC   | Encoding | Models     |
|---------------|----------|---------------------------------|---------|----------|------------|
| *ALY#         | 26477-0  | Atypical Lymphocytes Absolute   | 26477-0 | LN       | ELITE 580  |
| *ALY%         | 13046-8  | Atypical Lymphocytes %          | 13046-8 | LN       | ELITE 580  |
| *LIC#         | 11001    | Large Immature Cells Absolute   | 11001   | 99MRC    | ELITE 580  |
| *LIC%         | 11002    | Large Immature Cells %          | 11002   | 99MRC    | ELITE 580  |

### Silently Ignored Segments

These OBX segments are transmitted by the instrument but carry no clinical result
value and are not stored in OpenELIS. They route to `notMatchedResults` harmlessly.

| OBX-3 ID | Name                     | Type |
|----------|--------------------------|------|
| 02001    | Take Mode                | IS   |
| 02002    | Blood Mode               | IS   |
| 02003    | Test Mode                | IS   |
| 03001    | Reference Group          | IS   |
| 09001    | Remark                   | IS   |
| 30525-0  | Age                      | NM   |
| 13xxx    | Alarm / flag codes       | IS   |
| ED type  | Histograms / scattergrams| ED   |

---

## Message Format

- **Format**: HL7 v2.3.1
- **Transport Framing**: MLLP
  - Start Block: `<VT>` (`0x0B`)
  - End Block: `<FS>` (`0x1C`)
  - Segment Terminator: `<CR>` (`0x0D`)
- **Encoding**: UTF-8 (`MSH-18 = UNICODE`)
- **Accession / Sample ID**: `OBR-3` (Filler Order Number)
- **QC detection**: `OBR-13` (Relevant Clinical Info) non-empty → flagged as control
- **Results**: `OBX` segments
  - Test Code: `OBX-3` (ID component, format `ID^Name^EncodeSys`)
  - Value Type: `OBX-2` (`NM`, `IS`, `ST`; `ED` segments are skipped)
  - Value: `OBX-5`
  - Units: `OBX-6`
  - Abnormal Flag: `OBX-8` (e.g. `H~A` = High + Abnormal)

### Model Differences

| Feature           | H360                   | H560                          | ELite 580                     |
|-------------------|------------------------|-------------------------------|-------------------------------|
| Differential      | 3-part                 | 5-part                        | 5-part + extended params.     |
| Reportable params | ~22                    | ~26 (varies)                  | ~26+ (varies)                 |
| WBC diff          | GRAN/MID/LYM (% and #) | NEU/LYM/MON/EOS/BAS (% and #) | NEU/LYM/MON/EOS/BAS (% and #) |

---

## Build

```bash
cd plugins/analyzers/ErbaHematology
mvn clean package
```

## Installation

Copy the built JAR to `/var/lib/openelis-global/plugins/` and restart OpenELIS.

---

**Last Verified:** 2026-03-22