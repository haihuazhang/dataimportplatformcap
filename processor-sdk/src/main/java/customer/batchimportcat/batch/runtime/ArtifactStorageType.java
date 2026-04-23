package customer.batchimportcat.batch.runtime;

public enum ArtifactStorageType {
    DB_MEDIA,
    S3,
    OBJECT_STORAGE;

    public static ArtifactStorageType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return DB_MEDIA;
        }
        return ArtifactStorageType.valueOf(value.trim().toUpperCase());
    }
}
