using {zzdt} from '../db/model-dt';
using {batch} from '../db/model-springbatch';

/** Services */
service DataImportService {
    entity BatchImportConfig    as projection on zzdt.BatchImportConfig;
    entity BatchImportStructure as projection on zzdt.BatchImportStructure;
    entity BatchImportField     as projection on zzdt.BatchImportField;
    entity BatchImportFile      as projection on zzdt.BatchImportFile;
    entity BatchImportData      as projection on zzdt.BatchImportData;
    entity BatchImportMessage   as projection on zzdt.BatchImportMessage;
    entity JobInstance          as projection on batch.job_instance;
    entity JobExecution         as projection on batch.job_execution;
    entity JobExecutionContext  as projection on batch.job_execution_context;
    entity JobExecutionParam    as projection on batch.job_execution_params;
    entity StepExecution        as projection on batch.step_execution;
    entity StepExecutionContext as projection on batch.step_execution_context;
    entity ImportStructure      as projection on zzdt.ImportStructure;
    entity ImplementedByClass   as projection on zzdt.ImplementedByClass;
    entity ProcessKeyValueHelp  as projection on zzdt.ProcessKeyValueHelp;
    entity ImportFieldType      as projection on zzdt.ImportFieldType;
}


/** Data Import Structures*/
aspect dtimp : {
    MessageType : String(1);
    Message     : LargeString;
}

@batchdataimport: 'structure'
@title          : 'Test Import Structure'
// @cds.java.extends: ['dtimp']
// type zzsdtimp001   : dtimp {
type zzsdtimp001 {
    field_str01  : String(10);
    field_str02  : String(20);
    field_dec_01 : Decimal(13, 3);
};

@batchdataimport: 'structure'
@title          : 'Test Import Structure2'
type zzsdtimp002   : dtimp {
    field_str01  : String(10);
    field_str02  : String(20);
    field_dec_01 : Decimal(13, 3);
};

type teststructure : dtimp {
    field_str01  : String(10);
    field_str02  : String(20);
    field_dec_01 : Decimal(13, 3);
};

/*** Server Side Annotation*/

annotate DataImportService.BatchImportFile with {
    ConfigUUID @Common: {
        ValueList: {
            $Type         : 'Common.ValueListType',
            CollectionPath: 'BatchImportConfig',
            Parameters    : [
                {
                    $Type            : 'Common.ValueListParameterInOut',
                    LocalDataProperty: 'ConfigUUID',
                    ValueListProperty: 'ID'
                },
                {
                    $Type            : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty: 'ObjectName'
                // LocalDataProperty: to_Config.ObjectName
                },
                {
                    $Type            : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty: 'Object'
                }

            ]
        },
        Text     : to_Config.ObjectName
    };
    ID                   @UI.Hidden;
    FileName             @UI.Hidden;
    StatusCriticality    @UI.Hidden;
}

annotate DataImportService.BatchImportFile with @(UI: {
    FieldGroup #General_FG: {
        $Type: 'UI.FieldGroupType',
        Label: 'General',
        Data : [
            {
                Value: ConfigUUID,
                Label: 'Configuration'
            },
            {
                Value: Attachment,
                Label: 'Import File here'
            },
            {
                Value: FileName,
                ![@UI.Hidden]: true
            }
        ]
    },
    FieldGroup #Status_FG : {
        $Type: 'UI.FieldGroupType',
        Label: 'Status',
        Data : [
            {Value: StatusText},
            {Value: JobName},
        ]
    },
    FieldGroup #Job_FG    : {
        $Type: 'UI.FieldGroupType',
        Label: 'Job',
        Data : [{
            Value         : JobName,
            $Type         : 'UI.DataFieldWithIntentBasedNavigation',
            SemanticObject: 'zzdtimplog',
            Action        : 'display',
            Mapping       : [{
                LocalProperty         : JobName,
                SemanticObjectProperty: 'JOB_INSTANCE_ID'
            }]
        }]
    },
    Facets                : [
        {
            ID    : 'General',
            $Type : 'UI.ReferenceFacet',
            Target: '@UI.FieldGroup#General_FG'
        },
        {
            ID    : 'Job',
            $Type : 'UI.ReferenceFacet',
            Target: '@UI.FieldGroup#Job_FG'
        },
        {
            ID    : 'Status',
            $Type : 'UI.ReferenceFacet',
            Target: '@UI.FieldGroup#Status_FG'
        },
        {
            ID    : 'Data',
            $Type : 'UI.ReferenceFacet',
            Target: 'to_Data/@UI.LineItem'
        },
        {
            ID    : 'Messages',
            $Type : 'UI.ReferenceFacet',
            Target: 'to_Messages/@UI.LineItem'
        },
    ],
    LineItem              : [
        {Value: to_Config.ObjectName},
        {Value: FileName},
        {
            Value       : StatusText,
            Criticality : StatusCriticality,
            $Type       : 'UI.DataField'
        },
        {
            Value         : JobName,
            $Type         : 'UI.DataFieldWithIntentBasedNavigation',
            SemanticObject: 'zzdtimplog',
            Action        : 'display',
            Mapping       : [{
                LocalProperty         : JobName,
                SemanticObjectProperty: 'JOB_INSTANCE_ID'
            }]
        }
    ],

});

annotate DataImportService.BatchImportFile with @odata.draft.enabled;

annotate DataImportService.BatchImportConfig with {
    StructName         @Common: {ValueList: {
        $Type         : 'Common.ValueListType',
        CollectionPath: 'ImportStructure',
        Parameters    : [
            {
                $Type            : 'Common.ValueListParameterInOut',
                LocalDataProperty: 'StructName',
                ValueListProperty: 'Name'
            },
            {
                $Type            : 'Common.ValueListParameterDisplayOnly',
                ValueListProperty: 'Description'
            // LocalDataProperty: to_Config.ObjectName
            }

        ]
    }};
    ProcessKey         @Common: {ValueList: {
        $Type         : 'Common.ValueListType',
        CollectionPath: 'ProcessKeyValueHelp',
        Parameters    : [
            {
                $Type            : 'Common.ValueListParameterInOut',
                LocalDataProperty: 'ProcessKey',
                ValueListProperty: 'ProcessKey'
            },
            {
                $Type            : 'Common.ValueListParameterDisplayOnly',
                ValueListProperty: 'Description'

            }

        ]
    }};
};


annotate DataImportService.BatchImportConfig with @UI: {
    Facets                 : [
        {
            $Type : 'UI.ReferenceFacet',
            ID    : 'idIdentification',
            Label : 'Basic',
            Target: '@UI.Identification'
        },

        {
            ID    : 'Runtime',
            Target: '@UI.FieldGroup#Runtime_FG',
            $Type : 'UI.ReferenceFacet',
            Label : 'Runtime'
        },
        {
            ID    : 'Legacy',
            Target: '@UI.FieldGroup#Legacy_FG',
            $Type : 'UI.ReferenceFacet',
            Label : 'Legacy Bootstrap'
        },
        {
            ID    : 'Template',
            Target: '@UI.FieldGroup#Template_FG',
            $Type : 'UI.ReferenceFacet',
            Label : 'Template Area'
        },
        {
            ID    : 'Structures',
            Target: 'to_Structures/@UI.LineItem',
            $Type : 'UI.ReferenceFacet',
            Label : 'Structures'
        }
    ],
    Identification         : [
        {Value: Object},
        {Value: ObjectName}
    ],
    FieldGroup #Runtime_FG : {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: ProcessKey}
        ]
    },
    FieldGroup #Legacy_FG  : {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: StructName},
            {Value: SheetName},
            {Value: StartLine},
            {Value: StartColumn}
        ]
    },
    FieldGroup #Template_FG: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: Template},
            {
                Value        : FileName,
                ![@UI.Hidden]: true
            }
        ]
    },
    LineItem               : [
        {Value: Object},
        {Value: ObjectName},
        {Value: ProcessKey},
        {Value: Template},
        {Value: SheetName},
        {Value: StructName}
    ]
};

annotate DataImportService.BatchImportConfig with @odata.draft.enabled;

annotate DataImportService.BatchImportStructure with {
    ConfigUUID @Common: {
        ValueList: {
            $Type         : 'Common.ValueListType',
            CollectionPath: 'BatchImportConfig',
            Parameters    : [
                {
                    $Type            : 'Common.ValueListParameterInOut',
                    LocalDataProperty: 'ConfigUUID',
                    ValueListProperty: 'ID'
                },
                {
                    $Type            : 'Common.ValueListParameterDisplayOnly',
                    ValueListProperty: 'ObjectName'
                }
            ]
        }
    };
    ID        @UI.Hidden;
}

annotate DataImportService.BatchImportStructure with @UI: {
    Facets                   : [
        {
            ID    : 'General',
            $Type : 'UI.ReferenceFacet',
            Target: '@UI.Identification'
        },
        {
            ID    : 'Parsing',
            $Type : 'UI.ReferenceFacet',
            Target: '@UI.FieldGroup#Parsing_FG'
        },
        {
            ID    : 'Fields',
            $Type : 'UI.ReferenceFacet',
            Target: 'to_Fields/@UI.LineItem'
        }
    ],
    Identification           : [
        {Value: RootNode},
        {Value: SheetName},
        {Value: SheetNameUp}
    ],
    FieldGroup #Parsing_FG   : {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: StartLine},
            {Value: StartColumn},
            {Value: HasFieldnameLine},
            {Value: HasDescLine}
        ]
    },
    LineItem                 : [
        {Value: RootNode},
        {Value: SheetName},
        {Value: SheetNameUp},
        {Value: StartLine},
        {Value: StartColumn},
        {Value: HasFieldnameLine},
        {Value: HasDescLine}
    ]
};

annotate DataImportService.BatchImportField with {
    FieldType @Common: {ValueList: {
        $Type         : 'Common.ValueListType',
        CollectionPath: 'ImportFieldType',
        Parameters    : [
            {
                $Type            : 'Common.ValueListParameterInOut',
                LocalDataProperty: 'FieldType',
                ValueListProperty: 'Code'
            },
            {
                $Type            : 'Common.ValueListParameterDisplayOnly',
                ValueListProperty: 'Description'
            }
        ]
    }};
    ID        @UI.Hidden;
}

annotate DataImportService.BatchImportField with @UI: {
    Identification: [
        {Value: FieldName},
        {Value: FieldDescription}
    ],
    LineItem      : [
        {Value: Sequence},
        {Value: FieldName},
        {Value: FieldDescription},
        {Value: FieldType},
        {Value: FieldLength},
        {Value: FieldDecimal},
        {Value: IsKeyField},
        {Value: IsForeignField},
        {Value: ForeignField}
    ]
};

annotate DataImportService.BatchImportData with @(
    UI.CreateHidden : true,
    UI.UpdateHidden : true,
    UI.DeleteHidden : true,
    UI.LineItem     : [
        {Value: Line},
        {Value: StructureName},
        {Value: DataJson}
    ],
    UI.Identification: [
        {Value: Line},
        {Value: StructureName},
        {Value: DataJson}
    ]
);

annotate DataImportService.BatchImportMessage with @(
    UI.CreateHidden : true,
    UI.UpdateHidden : true,
    UI.DeleteHidden : true,
    UI.LineItem     : [
        {Value: Line},
        {Value: Type},
        {Value: Code},
        {Value: Message},
        {Value: Details}
    ],
    UI.Identification: [
        {Value: Line},
        {Value: Type},
        {Value: Code},
        {Value: Message},
        {Value: Details}
    ]
);


annotate DataImportService.JobInstance with @UI: {
    Facets        : [
        {
            $Type : 'UI.ReferenceFacet',
            ID    : 'idIdentification',
            Label : 'Basic',
            Target: '@UI.Identification'
        },

        {
            ID    : 'JobExecution',
            Target: 'to_Executions/@UI.LineItem',
            $Type : 'UI.ReferenceFacet',
            Label : 'Job Executions'
        // position       : 20
        }
    ],
    Identification: [
        {Value: JOB_INSTANCE_ID},
        {Value: JOB_NAME}
    ],

    LineItem      : [
        {Value: JOB_INSTANCE_ID},
        {Value: JOB_NAME}
    ]
};


annotate DataImportService.JobExecution with @UI: {
    Facets               : [
        {
            $Type : 'UI.ReferenceFacet',
            ID    : 'idIdentification',
            Label : 'Basic',
            Target: '@UI.Identification'
        },
        {
            ID    : 'Function',
            Target: '@UI.FieldGroup#Execution',
            $Type : 'UI.ReferenceFacet',
            Label : 'Execution'
        },
        {
            ID    : 'Status',
            Target: '@UI.FieldGroup#Status',
            $Type : 'UI.ReferenceFacet',
            Label : 'Status'
        },
        {
            ID    : 'Context',
            Target: 'to_Context/@UI.Identification',
            $Type : 'UI.ReferenceFacet',
            Label : 'Context'
        },
        {
            ID    : 'Parameters',
            Target: 'to_Params/@UI.LineItem',
            $Type : 'UI.ReferenceFacet',
            Label : 'Parameters'
        },
        {
            ID    : 'JobExecution',
            Target: 'to_StepExecutions/@UI.LineItem',
            $Type : 'UI.ReferenceFacet',
            Label : 'Steps'
        // position       : 20
        }
    ],
    Identification       : [
        // {Value: JOB_INSTANCE_ID},
        {Value: JOB_EXECUTION_ID},
        {Value: CREATE_TIME}
    ],
    FieldGroup #Execution: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: START_TIME},
            {Value: END_TIME},
        ]
    },
    FieldGroup #Status   : {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: STATUS},
            {Value: EXIT_CODE},
            {Value: EXIT_MESSAGE}
        ]
    },

    LineItem             : [
        // {Value: JOB_INSTANCE_ID},
        {Value: JOB_EXECUTION_ID},
        {Value: CREATE_TIME},
        {Value: START_TIME},
        {Value: END_TIME},
        {Value: STATUS},
        {Value: EXIT_CODE},
        {Value: EXIT_MESSAGE}
    ]
};


annotate DataImportService.JobExecutionContext with @UI: {
    LineItem      : [
        {Value: SHORT_CONTEXT},
        {Value: SERIALIZED_CONTEXT}
    ],
    Identification: [
        {Value: SHORT_CONTEXT},
        {Value: SERIALIZED_CONTEXT}
    ]
};

annotate DataImportService.JobExecutionParam with @UI: {LineItem: [
    {Value: PARAMETER_NAME},
    {Value: PARAMETER_TYPE},
    {Value: PARAMETER_VALUE}
]};


annotate DataImportService.StepExecution with @UI: {
    Facets               : [
        {
            $Type : 'UI.ReferenceFacet',
            ID    : 'idIdentification',
            Label : 'Basic',
            Target: '@UI.Identification'
        },
        {
            ID    : 'Function',
            Target: '@UI.FieldGroup#Execution',
            $Type : 'UI.ReferenceFacet',
            Label : 'Execution'
        },
        {
            ID    : 'Status',
            Target: '@UI.FieldGroup#Status',
            $Type : 'UI.ReferenceFacet',
            Label : 'Status'
        },
        {
            ID    : 'DataCount',
            Target: '@UI.FieldGroup#DataCount',
            $Type : 'UI.ReferenceFacet',
            Label : 'Data Count'
        },
        {
            ID    : 'Parameters',
            Target: 'to_Context/@UI.Identification',
            $Type : 'UI.ReferenceFacet',
            Label : 'Parameters'
        }

    // {
    //     ID    : 'JobExecution',
    //     Target: 'to_Executions/@UI.LineItem',
    //     $Type : 'UI.ReferenceFacet',
    //     Label : 'Job Executions'
    // // position       : 20
    // }
    ],
    Identification       : [
        // {Value: },
        // {Value: JOB_EXECUTION_ID},
        {Value: STEP_EXECUTION_ID},
        {Value: STEP_NAME},
        {Value: CREATE_TIME}
    ],
    FieldGroup #Execution: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: START_TIME},
            {Value: END_TIME},
        ]
    },
    FieldGroup #Status   : {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: STATUS},
            {Value: EXIT_CODE},
            {Value: EXIT_MESSAGE}
        ]
    },
    FieldGroup #DataCount: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: COMMIT_COUNT},
            {Value: READ_COUNT},
            {Value: FILTER_COUNT},
            {Value: WRITE_COUNT},
            {Value: READ_SKIP_COUNT},
            {Value: WRITE_SKIP_COUNT},
            {Value: PROCESS_SKIP_COUNT},
            {Value: ROLLBACK_COUNT}
        ]
    },

    LineItem             : [
        // {Value: JOB_EXECUTION_ID},
        {Value: STEP_EXECUTION_ID},
        {Value: STEP_NAME},
        {Value: CREATE_TIME},
        {Value: START_TIME},
        {Value: END_TIME},
        {Value: STATUS},
        {Value: EXIT_CODE},
        {Value: EXIT_MESSAGE},
        {Value: READ_COUNT},
        {Value: WRITE_COUNT}
    // {Value: to_Context.SHORT_CONTEXT}
    ]
};


annotate DataImportService.StepExecutionContext with @UI: {
    LineItem      : [
        {Value: SHORT_CONTEXT},
        {Value: SERIALIZED_CONTEXT}
    ],
    Identification: [
        {Value: SHORT_CONTEXT},
        {Value: SERIALIZED_CONTEXT}
    ]
};
