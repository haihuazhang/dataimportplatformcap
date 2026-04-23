package customer.batchimportcat.batch.dynamic.types;

import com.sap.cds.reflect.CdsBaseType;
import com.sap.cds.reflect.CdsSimpleType;

public enum DynamicFieldType {
    STRING("STRING", "String"),
    INTEGER("INTEGER", "Integer"),
    DECIMAL("DECIMAL", "Decimal"),
    BOOLEAN("BOOLEAN", "Boolean"),
    DATE("DATE", "Date"),
    TIME("TIME", "Time"),
    DATETIME("DATETIME", "Date Time"),
    TIMESTAMP("TIMESTAMP", "Timestamp"),
    UUID("UUID", "UUID"),
    BINARY("BINARY", "Binary");

    private final String code;
    private final String description;

    DynamicFieldType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DynamicFieldType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return STRING;
        }
        String normalized = code.trim().toUpperCase();
        return switch (normalized) {
            case "STRING", "CHAR", "VARCHAR", "S" -> STRING;
            case "INTEGER", "INT", "I", "INT16", "INT32", "INT64" -> INTEGER;
            case "DECIMAL", "DEC", "NUMERIC", "P" -> DECIMAL;
            case "BOOLEAN", "BOOL" -> BOOLEAN;
            case "DATE" -> DATE;
            case "TIME" -> TIME;
            case "DATETIME" -> DATETIME;
            case "TIMESTAMP" -> TIMESTAMP;
            case "UUID" -> UUID;
            case "BINARY", "RAW" -> BINARY;
            default -> STRING;
        };
    }

    public static DynamicFieldType fromSimpleType(CdsSimpleType simpleType) {
        CdsBaseType baseType = simpleType.getType();
        return switch (baseType) {
            case UUID -> UUID;
            case BOOLEAN -> BOOLEAN;
            case INTEGER, UINT8, INT16, INT32, INT64, INTEGER64 -> INTEGER;
            case DECIMAL, DECIMAL_FLOAT, DOUBLE, HANA_SMALLDECIMAL, HANA_REAL -> DECIMAL;
            case DATE -> DATE;
            case TIME -> TIME;
            case DATETIME -> DATETIME;
            case TIMESTAMP -> TIMESTAMP;
            case BINARY, LARGE_BINARY, HANA_BINARY -> BINARY;
            default -> STRING;
        };
    }
}
