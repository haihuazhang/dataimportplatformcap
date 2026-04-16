package customer.batchimportcat.batch.itemreaders;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;

import customer.batchimportcat.batch.dynamic.dto.DynamicNode;
import customer.batchimportcat.batch.dynamic.types.DynamicFieldDefinition;
import customer.batchimportcat.batch.dynamic.types.DynamicFieldType;
import customer.batchimportcat.batch.dynamic.types.DynamicImportConfiguration;
import customer.batchimportcat.batch.dynamic.types.DynamicStructureDefinition;

public class DynamicHierarchyItemReader implements ItemStreamReader<DynamicNode> {
    private static final String INDEX_KEY = "dynamicHierarchyReader.index";
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.BASIC_ISO_DATE,
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.M.d"));

    private final byte[] fileContent;
    private final DynamicImportConfiguration configuration;
    private final List<DynamicNode> rootNodes = new ArrayList<>();
    private final DataFormatter dataFormatter = new DataFormatter();

    private int currentIndex;
    private boolean initialized;

    public DynamicHierarchyItemReader(byte[] fileContent, DynamicImportConfiguration configuration) {
        this.fileContent = fileContent;
        this.configuration = configuration;
    }

    @Override
    public DynamicNode read() {
        if (currentIndex >= rootNodes.size()) {
            return null;
        }
        return rootNodes.get(currentIndex++);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        currentIndex = executionContext.containsKey(INDEX_KEY) ? executionContext.getInt(INDEX_KEY) : 0;
        if (!initialized) {
            initialized = true;
            try {
                buildHierarchy();
            } catch (IOException exception) {
                throw new ItemStreamException("Failed to initialize dynamic hierarchy reader.", exception);
            }
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putInt(INDEX_KEY, currentIndex);
    }

    @Override
    public void close() {
        rootNodes.clear();
        currentIndex = 0;
        initialized = false;
    }

    private void buildHierarchy() throws IOException {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileContent))) {
            Map<String, List<DynamicNode>> nodesByStructure = new HashMap<>();
            Map<String, List<DynamicStructureDefinition>> childStructuresByParentSheet = new HashMap<>();

            for (DynamicStructureDefinition structure : configuration.sortedStructures()) {
                nodesByStructure.put(structure.id(), readStructureRows(workbook, structure));
                if (structure.sheetNameUp() != null && !structure.sheetNameUp().isBlank()) {
                    childStructuresByParentSheet.computeIfAbsent(structure.sheetNameUp(), ignored -> new ArrayList<>())
                            .add(structure);
                }
            }

            for (DynamicStructureDefinition rootStructure : configuration.rootStructures()) {
                List<DynamicNode> nodes = nodesByStructure.getOrDefault(rootStructure.id(), List.of());
                for (DynamicNode node : nodes) {
                    rootNodes.add(buildTree(node, rootStructure, nodesByStructure, childStructuresByParentSheet,
                            new HashSet<>()));
                }
            }
        }
    }

    private List<DynamicNode> readStructureRows(Workbook workbook, DynamicStructureDefinition structure) {
        Sheet sheet = workbook.getSheet(structure.sheetName());
        if (sheet == null) {
            return List.of();
        }

        List<DynamicFieldDefinition> fields = structure.sortedFields();
        int startRow = Math.max(structure.startLine() - 1, 0);
        if (structure.hasFieldnameLine()) {
            startRow += 1;
        }
        if (structure.hasDescLine()) {
            startRow += 1;
        }
        int startColumnIndex = CellReference.convertColStringToIndex(
                structure.startColumn() == null || structure.startColumn().isBlank() ? "A" : structure.startColumn());
        List<DynamicNode> nodes = new ArrayList<>();

        for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            DynamicNode node = new DynamicNode(structure.id(), structure.sheetName(), rowIndex + 1);
            boolean hasData = false;
            for (int fieldIndex = 0; fieldIndex < fields.size(); fieldIndex++) {
                DynamicFieldDefinition field = fields.get(fieldIndex);
                Cell cell = row.getCell(startColumnIndex + fieldIndex);
                Object value = readCellValue(cell, field);
                if (value != null && !String.valueOf(value).isBlank()) {
                    hasData = true;
                }
                node.putField(field.fieldName(), value);
            }
            if (hasData) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private DynamicNode buildTree(DynamicNode currentNode, DynamicStructureDefinition currentStructure,
            Map<String, List<DynamicNode>> nodesByStructure,
            Map<String, List<DynamicStructureDefinition>> childStructuresByParentSheet, Set<String> visited) {
        DynamicNode copy = currentNode.copyShallow();
        if (!visited.add(currentStructure.id())) {
            return copy;
        }
        List<DynamicStructureDefinition> childStructures = childStructuresByParentSheet
                .getOrDefault(currentStructure.sheetName(), List.of());
        for (DynamicStructureDefinition childStructure : childStructures) {
            List<DynamicFieldDefinition> foreignFields = childStructure.sortedFields().stream()
                    .filter(DynamicFieldDefinition::foreignField)
                    .toList();
            if (foreignFields.isEmpty()) {
                continue;
            }
            for (DynamicNode childNode : nodesByStructure.getOrDefault(childStructure.id(), List.of())) {
                if (matchesByForeignFields(copy, childNode, foreignFields)) {
                    copy.addChild(childStructure.sheetName(),
                            buildTree(childNode, childStructure, nodesByStructure, childStructuresByParentSheet,
                                    new HashSet<>(visited)));
                }
            }
        }
        return copy;
    }

    private boolean matchesByForeignFields(DynamicNode parentNode, DynamicNode childNode,
            List<DynamicFieldDefinition> foreignFields) {
        for (DynamicFieldDefinition foreignField : foreignFields) {
            Object childValue = childNode.getFields().get(foreignField.fieldName());
            Object parentValue = parentNode.getFields().get(foreignField.foreignFieldName());
            if (childValue == null || parentValue == null || !String.valueOf(childValue).equals(String.valueOf(parentValue))) {
                return false;
            }
        }
        return true;
    }

    private Object readCellValue(Cell cell, DynamicFieldDefinition fieldDefinition) {
        if (cell == null) {
            return null;
        }
        CellType cellType = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        String formattedValue = dataFormatter.formatCellValue(cell);
        if (formattedValue == null || formattedValue.isBlank()) {
            return null;
        }

        return switch (DynamicFieldType.fromCode(fieldDefinition.fieldType())) {
            case INTEGER -> readInteger(cell, cellType, formattedValue);
            case DECIMAL -> readDecimal(cell, cellType, formattedValue);
            case BOOLEAN -> readBoolean(cell, cellType, formattedValue);
            case DATE -> readDate(cell, formattedValue);
            case TIME -> readTime(cell, formattedValue);
            case DATETIME, TIMESTAMP -> readTimestamp(cell, formattedValue);
            default -> formattedValue.trim();
        };
    }

    private Object readInteger(Cell cell, CellType cellType, String formattedValue) {
        if (cellType == CellType.NUMERIC) {
            return Math.round(cell.getNumericCellValue());
        }
        return Long.parseLong(formattedValue.trim());
    }

    private Object readDecimal(Cell cell, CellType cellType, String formattedValue) {
        if (cellType == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        return new BigDecimal(formattedValue.trim());
    }

    private Object readBoolean(Cell cell, CellType cellType, String formattedValue) {
        if (cellType == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        }
        return Boolean.parseBoolean(formattedValue.trim());
    }

    private Object readDate(Cell cell, String formattedValue) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate().toString();
        }
        LocalDate parsedValue = parseLocalDate(formattedValue);
        // LocalDate parsedValue = LocalDate.parse(formattedValue);
        return parsedValue.toString();
    }

    private Object readTime(Cell cell, String formattedValue) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalTime().toString();
        }
        return LocalTime.parse(formattedValue.trim()).toString();
    }

    private Object readTimestamp(Cell cell, String formattedValue) {
        if (DateUtil.isCellDateFormatted(cell)) {
            LocalDateTime localDateTime = cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            return localDateTime.toString();
        }
        return formattedValue.trim();
    }

    private LocalDate parseLocalDate(String formattedValue) {
        String normalized = formattedValue.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException exception) {
                // Try the next accepted date format.
            }
        }
        return LocalDate.parse(normalized);
    }
}
