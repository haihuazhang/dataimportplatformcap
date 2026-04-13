package customer.batchimportcat.batch.dynamic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.reflect.CdsSimpleType;
import com.sap.cds.reflect.CdsStructuredType;
import com.sap.cds.reflect.CdsType;

import cds.gen.dataimportservice.BatchImportConfig;
import cds.gen.dataimportservice.BatchImportField;
import cds.gen.dataimportservice.BatchImportStructure;
import customer.batchimportcat.batch.exceptions.BatchExceptionsUtil;

@Component
public class DynamicConfigurationBuilder {
    private final CdsModel cdsModel;

    public DynamicConfigurationBuilder(CdsModel cdsModel) {
        this.cdsModel = cdsModel;
    }

    public DynamicImportConfiguration fromServiceConfig(BatchImportConfig config) {
        List<DynamicStructureDefinition> structures = Optional.ofNullable(config.getToStructures())
                .orElse(List.of())
                .stream()
                .map(this::toStructureDefinition)
                .toList();
        DynamicImportConfiguration dynamicConfig = new DynamicImportConfiguration(
                config.getId(),
                config.getObject(),
                config.getObjectName(),
                config.getProcessKey(),
                config.getImplementedByClass(),
                config.getStructName(),
                config.getSheetName(),
                toInt(config.getStartLine(), 1),
                defaultString(config.getStartColumn(), "A"),
                structures);
        return ensureLegacyCompatibility(dynamicConfig);
    }

    public DynamicImportConfiguration fromExecutionContext(Map<String, Serializable> configData,
            List<Map<String, Serializable>> structures, List<Map<String, Serializable>> fields) {
        Map<String, List<DynamicFieldDefinition>> fieldsByStructure = fields.stream()
                .map(this::toFieldDefinition)
                .collect(Collectors.groupingBy(DynamicFieldDefinition::structureUUID, LinkedHashMap::new, Collectors.toList()));

        List<DynamicStructureDefinition> structureDefinitions = structures.stream()
                .map(row -> toStructureDefinition(row, fieldsByStructure.getOrDefault(asString(row.get("ID")), List.of())))
                .sorted(Comparator.comparing(DynamicStructureDefinition::rootNode).reversed()
                        .thenComparing(DynamicStructureDefinition::sheetName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        DynamicImportConfiguration dynamicConfig = new DynamicImportConfiguration(
                asString(configData.get("ID")),
                asString(configData.get("Object")),
                asString(configData.get("ObjectName")),
                asString(configData.get("ProcessKey")),
                asString(configData.get("ImplementedByClass")),
                asString(configData.get("StructName")),
                asString(configData.get("SheetName")),
                toInt(configData.get("StartLine"), 1),
                defaultString(asString(configData.get("StartColumn")), "A"),
                structureDefinitions);
        return ensureLegacyCompatibility(dynamicConfig);
    }

    private DynamicImportConfiguration ensureLegacyCompatibility(DynamicImportConfiguration config) {
        if (!config.structures().isEmpty()) {
            return config;
        }
        if (config.legacyStructName() == null || config.legacyStructName().isBlank()) {
            return config;
        }

        List<DynamicFieldDefinition> legacyFields = buildLegacyFields(config.id(), config.legacyStructName());
        DynamicStructureDefinition legacyStructure = new DynamicStructureDefinition(
                "LEGACY::" + config.id(),
                config.id(),
                true,
                defaultString(config.legacySheetName(), config.legacyStructName()),
                null,
                Math.max(config.legacyStartLine(), 1),
                defaultString(config.legacyStartColumn(), "A"),
                true,
                false,
                legacyFields);

        return new DynamicImportConfiguration(
                config.id(),
                config.object(),
                config.objectName(),
                config.processKey(),
                config.implementedByClass(),
                config.legacyStructName(),
                config.legacySheetName(),
                config.legacyStartLine(),
                config.legacyStartColumn(),
                List.of(legacyStructure));
    }

    private List<DynamicFieldDefinition> buildLegacyFields(String configUUID, String structName) {
        CdsStructuredType structuredType = cdsModel.findStructuredType(structName)
                .orElseThrow(() -> BatchExceptionsUtil.getBatchConfigNotFoundBecauseNoStruct(structName));

        AtomicLong sequence = new AtomicLong(10);
        List<DynamicFieldDefinition> legacyFields = structuredType.concreteNonAssociationElements()
                .filter(element -> element.getType().isSimple())
                .map(element -> toLegacyField(configUUID, element, sequence.getAndAdd(10)))
                .toList();
        if (!legacyFields.isEmpty() && legacyFields.stream().noneMatch(DynamicFieldDefinition::keyField)) {
            DynamicFieldDefinition firstField = legacyFields.get(0);
            List<DynamicFieldDefinition> adjustedFields = new ArrayList<>(legacyFields);
            adjustedFields.set(0, new DynamicFieldDefinition(
                    firstField.id(),
                    firstField.configUUID(),
                    firstField.structureUUID(),
                    firstField.fieldName(),
                    firstField.fieldDescription(),
                    true,
                    firstField.sequence(),
                    firstField.foreignField(),
                    firstField.foreignFieldName(),
                    firstField.fieldLength(),
                    firstField.fieldType(),
                    firstField.fieldDecimal()));
            return adjustedFields;
        }
        return legacyFields;
    }

    private DynamicFieldDefinition toLegacyField(String configUUID, CdsElement element, long sequence) {
        CdsType type = element.getType();
        CdsSimpleType simpleType = type.as(CdsSimpleType.class);
        Integer length = firstPositiveInteger(
                simpleType.get("length"),
                simpleType.get("precision"),
                defaultLength(DynamicFieldType.fromSimpleType(simpleType)));
        Integer decimal = firstPositiveInteger(simpleType.get("scale"), 0);
        return new DynamicFieldDefinition(
                "LEGACY::" + configUUID + "::" + element.getName(),
                configUUID,
                "LEGACY::" + configUUID,
                element.getName(),
                Optional.ofNullable(element.getAnnotationValue("title", (String) null)).orElse(element.getName()),
                element.isKey(),
                sequence,
                false,
                null,
                length,
                DynamicFieldType.fromSimpleType(simpleType).getCode(),
                decimal);
    }

    private DynamicStructureDefinition toStructureDefinition(BatchImportStructure structure) {
        List<DynamicFieldDefinition> fieldDefinitions = Optional.ofNullable(structure.getToFields())
                .orElse(List.of())
                .stream()
                .map(this::toFieldDefinition)
                .toList();
        return new DynamicStructureDefinition(
                structure.getId(),
                structure.getConfigUUID(),
                Boolean.TRUE.equals(structure.getRootNode()),
                structure.getSheetName(),
                structure.getSheetNameUp(),
                toInt(structure.getStartLine(), 1),
                defaultString(structure.getStartColumn(), "A"),
                Boolean.TRUE.equals(structure.getHasFieldnameLine()),
                Boolean.TRUE.equals(structure.getHasDescLine()),
                fieldDefinitions);
    }

    private DynamicStructureDefinition toStructureDefinition(Map<String, ? extends Object> row,
            List<DynamicFieldDefinition> fields) {
        return new DynamicStructureDefinition(
                asString(row.get("ID")),
                asString(row.get("ConfigUUID")),
                toBoolean(row.get("RootNode")),
                asString(row.get("SheetName")),
                asString(row.get("SheetNameUp")),
                toInt(row.get("StartLine"), 1),
                defaultString(asString(row.get("StartColumn")), "A"),
                toBoolean(row.get("HasFieldnameLine"), true),
                toBoolean(row.get("HasDescLine"), true),
                fields);
    }

    private DynamicFieldDefinition toFieldDefinition(BatchImportField field) {
        return new DynamicFieldDefinition(
                field.getId(),
                field.getConfigUUID(),
                field.getStructureUUID(),
                field.getFieldName(),
                field.getFieldDescription(),
                Boolean.TRUE.equals(field.getIsKeyField()),
                toLong(field.getSequence(), 0L),
                Boolean.TRUE.equals(field.getIsForeignField()),
                field.getForeignField(),
                toInteger(field.getFieldLength()),
                defaultString(field.getFieldType(), DynamicFieldType.STRING.getCode()),
                toInteger(field.getFieldDecimal()));
    }

    private DynamicFieldDefinition toFieldDefinition(Map<String, ? extends Object> row) {
        return new DynamicFieldDefinition(
                asString(row.get("ID")),
                asString(row.get("ConfigUUID")),
                asString(row.get("StructureUUID")),
                asString(row.get("FieldName")),
                asString(row.get("FieldDescription")),
                toBoolean(row.get("IsKeyField")),
                toLong(row.get("Sequence"), 0L),
                toBoolean(row.get("IsForeignField")),
                asString(row.get("ForeignField")),
                toInteger(row.get("FieldLength")),
                defaultString(asString(row.get("FieldType")), DynamicFieldType.STRING.getCode()),
                toInteger(row.get("FieldDecimal")));
    }

    private int defaultLength(DynamicFieldType fieldType) {
        return switch (fieldType) {
            case INTEGER -> 18;
            case DECIMAL -> 18;
            case DATE -> 10;
            case TIME -> 8;
            case DATETIME, TIMESTAMP -> 27;
            case UUID -> 36;
            default -> 255;
        };
    }

    private Integer firstPositiveInteger(Object value, Object fallback, int defaultValue) {
        Integer first = toInteger(value);
        if (first != null && first > 0) {
            return first;
        }
        Integer second = toInteger(fallback);
        if (second != null && second >= 0) {
            return second;
        }
        return defaultValue;
    }

    private Integer firstPositiveInteger(Object value, int defaultValue) {
        Integer first = toInteger(value);
        return first != null ? first : defaultValue;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private boolean toBoolean(Object value) {
        return toBoolean(value, false);
    }

    private boolean toBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private long toLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String stringValue = String.valueOf(value);
        return stringValue.isBlank() ? null : Integer.valueOf(stringValue);
    }
}
