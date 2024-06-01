namespace zzdt;

using {
    cuid,
    managed
} from '@sap/cds/common';

entity BatchImportConfig : cuid, managed {
    Object             : String(50)  @Common.Label:'{i18n>zzdt_Object}';
    ObjectName         : String(100) @Common.Label:'{i18n>zzdt_ObjectName}';
    ImplementedByClass : String @Common.Label:'{i18n>zzdt_ImplementedByClass}';
    MimeType           : String       @Core.IsMediaType;
    FileName           : String @Common.Label:'{i18n>zzdt_FileName}';
    Template           : LargeBinary  @Core.MediaType: MimeType  @Core.ContentDisposition.Filename: FileName;
    SheetName          : String @Common.Label:'{i18n>zzdt_SheetName}';
    StructName         : String @Common.Label:'{i18n>zzdt_StructName}';
    StartLine          : Int64 @Common.Label:'{i18n>zzdt_StartLine}';
    StartColumn        : String(5) @Common.Label:'{i18n>zzdt_StartColumn}';

}


entity BatchImportFile : cuid, managed {
    ConfigUUID : UUID @Common.Label:'{i18n>zzdt_ConfigUUID}';
    MimeType   : String       @Core.IsMediaType;
    FileName   : String(100);
    Attachment : LargeBinary  @Core.MediaType: MimeType  @Core.ContentDisposition.Filename: FileName;
    JobName    : String(150) @Common.Label:'{i18n>zzdt_JobName}' @readonly;
    to_Config  : Association to one BatchImportConfig
                     on ConfigUUID = to_Config.ID;
}

@cds.persistence.skip
entity ImportStructure {
    key Name : String(100);
    Description : String(255);
}

@cds.persistence.skip
entity ImplementedByClass {
    key Name: String;
    Description : String;
}