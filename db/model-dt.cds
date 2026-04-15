namespace zzdt;

using {
    cuid,
    managed
} from '@sap/cds/common';

entity BatchImportConfig : cuid, managed {
    Object             : String(50)  @Common.Label:'{i18n>zzdt_Object}';
    ObjectName         : String(100) @Common.Label:'{i18n>zzdt_ObjectName}';
    ProcessKey         : String(100) @Common.Label:'{i18n>zzdt_ProcessKey}';
    ImplementedByClass : String(255) @Common.Label:'{i18n>zzdt_ImplementedByClass}';
    MimeType           : String       @Core.IsMediaType;
    FileName           : String(255)  @Common.Label:'{i18n>zzdt_FileName}';
    Template           : LargeBinary  @Core.MediaType: MimeType  @Core.ContentDisposition.Filename: FileName;
    SheetName          : String(100)  @Common.Label:'{i18n>zzdt_SheetName}';
    StructName         : String(100)  @Common.Label:'{i18n>zzdt_StructName}';
    StartLine          : Int64        @Common.Label:'{i18n>zzdt_StartLine}';
    StartColumn        : String(5)    @Common.Label:'{i18n>zzdt_StartColumn}';
    to_Structures      : Composition of many BatchImportStructure
                             on to_Structures.ConfigUUID = ID;
}

entity BatchImportStructure : cuid, managed {
    ConfigUUID        : UUID        @Common.Label:'{i18n>zzdt_ConfigUUID}';
    RootNode          : Boolean     @Common.Label:'{i18n>zzdt_RootNode}' default false;
    SheetName         : String(100) @Common.Label:'{i18n>zzdt_SheetName}';
    SheetNameUp       : String(100) @Common.Label:'{i18n>zzdt_SheetNameUp}';
    StartLine         : Int64       @Common.Label:'{i18n>zzdt_StartLine}' default 1;
    StartColumn       : String(5)   @Common.Label:'{i18n>zzdt_StartColumn}' default 'A';
    HasFieldnameLine  : Boolean     @Common.Label:'{i18n>zzdt_HasFieldnameLine}' default true;
    HasDescLine       : Boolean     @Common.Label:'{i18n>zzdt_HasDescLine}' default true;
    to_Config         : Association to one BatchImportConfig
                            on ConfigUUID = to_Config.ID;
    to_Fields         : Composition of many BatchImportField
                            on to_Fields.StructureUUID = ID;
}

entity BatchImportField : cuid, managed {
    ConfigUUID       : UUID        @Common.Label:'{i18n>zzdt_ConfigUUID}';
    StructureUUID    : UUID        @Common.Label:'{i18n>zzdt_StructureUUID}';
    FieldName        : String(100) @Common.Label:'{i18n>zzdt_FieldName}';
    FieldDescription : String(255) @Common.Label:'{i18n>zzdt_FieldDescription}';
    IsKeyField       : Boolean     @Common.Label:'{i18n>zzdt_IsKeyField}' default false;
    Sequence         : Int64       @Common.Label:'{i18n>zzdt_Sequence}';
    IsForeignField   : Boolean     @Common.Label:'{i18n>zzdt_IsForeignField}' default false;
    ForeignField     : String(100) @Common.Label:'{i18n>zzdt_ForeignField}';
    FieldLength      : Int64       @Common.Label:'{i18n>zzdt_FieldLength}';
    FieldType        : String(30)  @Common.Label:'{i18n>zzdt_FieldType}';
    FieldDecimal     : Int64       @Common.Label:'{i18n>zzdt_FieldDecimal}';
    to_Config        : Association to one BatchImportConfig
                           on ConfigUUID = to_Config.ID;
    to_Structure     : Association to one BatchImportStructure
                           on StructureUUID = to_Structure.ID;
}

entity BatchImportFile : cuid, managed {
    ConfigUUID         : UUID         @Common.Label:'{i18n>zzdt_ConfigUUID}';
    MimeType           : String       @Core.IsMediaType;
    FileName           : String(255)  @Common.Label:'{i18n>zzdt_FileName}';
    Attachment         : LargeBinary  @Core.MediaType: MimeType  @Core.ContentDisposition.Filename: FileName;
    JobName            : String(150)  @Common.Label:'{i18n>zzdt_JobName}';
    Status             : String(1)    @Common.Label:'{i18n>zzdt_Status}';
    StatusText         : String(40)   @Common.Label:'{i18n>zzdt_StatusText}';
    StatusCriticality  : Int32        @Common.Label:'{i18n>zzdt_StatusCriticality}';
    to_Config          : Association to one BatchImportConfig
                             on ConfigUUID = to_Config.ID;
    to_Data            : Composition of many BatchImportData
                             on to_Data.FileUUID = ID;
    to_Messages        : Composition of many BatchImportMessage
                             on to_Messages.FileUUID = ID;
}

entity BatchImportData : cuid, managed {
    FileUUID      : UUID        @Common.Label:'{i18n>zzdt_FileUUID}';
    StructureName : String(100) @Common.Label:'{i18n>zzdt_StructName}';
    Line          : Int64       @Common.Label:'{i18n>zzdt_Line}';
    DataJson      : LargeString @Common.Label:'{i18n>zzdt_DataJson}';
    to_File       : Association to one BatchImportFile
                        on FileUUID = to_File.ID;
}

entity BatchImportMessage : cuid, managed {
    FileUUID    : UUID        @Common.Label:'{i18n>zzdt_FileUUID}';
    Line        : Int64       @Common.Label:'{i18n>zzdt_Line}';
    Type        : String(1)   @Common.Label:'{i18n>zzdt_MessageType}';
    Code        : String(100) @Common.Label:'{i18n>zzdt_MessageCode}';
    Message     : LargeString @Common.Label:'{i18n>zzdt_Message}';
    Details     : LargeString @Common.Label:'{i18n>zzdt_MessageDetails}';
    to_File     : Association to one BatchImportFile
                      on FileUUID = to_File.ID;
}

@cds.persistence.skip
entity ImportStructure {
    key Name     : String(100);
    Description : String(255);
}

@cds.persistence.skip
entity ImplementedByClass {
    key Name     : String;
    Description : String;
}

@cds.persistence.skip
entity ProcessKeyValueHelp {
    key ProcessKey : String(100);
    Description    : String(255);
}

@cds.persistence.skip
entity ImportFieldType {
    key Code       : String(30);
    Description    : String(255);
}
