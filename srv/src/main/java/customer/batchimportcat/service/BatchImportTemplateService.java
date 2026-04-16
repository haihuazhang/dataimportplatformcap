package customer.batchimportcat.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import customer.batchimportcat.batch.dynamic.types.DynamicFieldDefinition;
import customer.batchimportcat.batch.dynamic.types.DynamicImportConfiguration;
import customer.batchimportcat.batch.dynamic.types.DynamicStructureDefinition;

@Service
public class BatchImportTemplateService {
    public byte[] generateTemplate(DynamicImportConfiguration configuration) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (configuration.sortedStructures().isEmpty()) {
                workbook.createSheet("Instructions")
                        .createRow(0)
                        .createCell(0)
                        .setCellValue("Configure structures and fields before downloading the template.");
            } else {
                for (DynamicStructureDefinition structure : configuration.sortedStructures()) {
                    String sheetName = sanitizeSheetName(structure.sheetName());
                    var sheet = workbook.createSheet(sheetName);
                    int rowIndex = Math.max(structure.startLine() - 1, 0);
                    int columnIndexStart = CellReference.convertColStringToIndex(
                            structure.startColumn() == null || structure.startColumn().isBlank() ? "A" : structure.startColumn());
                    if (structure.hasFieldnameLine()) {
                        Row fieldRow = sheet.createRow(rowIndex++);
                        int columnIndex = columnIndexStart;
                        for (DynamicFieldDefinition field : structure.sortedFields()) {
                            writeCell(fieldRow, columnIndex++, field.fieldName());
                        }
                    }
                    if (structure.hasDescLine()) {
                        Row descriptionRow = sheet.createRow(rowIndex++);
                        int columnIndex = columnIndexStart;
                        for (DynamicFieldDefinition field : structure.sortedFields()) {
                            writeCell(descriptionRow, columnIndex++,
                                    field.fieldDescription() == null ? field.fieldName() : field.fieldDescription());
                        }
                    }
                }
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to generate batch import template.", exception);
        }
    }

    private String sanitizeSheetName(String sheetName) {
        String candidate = (sheetName == null || sheetName.isBlank()) ? "Sheet1" : sheetName;
        return candidate.length() > 31 ? candidate.substring(0, 31) : candidate;
    }

    private void writeCell(Row row, int columnIndex, String value) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setCellValue(value);
    }
}
