using {zzdt} from '../db/model-dt';
using {batch} from '../db/model-springbatch';

/** Services */
service DataImportService {
    entity BatchImportConfig    as projection on zzdt.BatchImportConfig;
    entity BatchImportFile      as projection on zzdt.BatchImportFile;
    entity JobInstance          as projection on batch.job_instance;
    entity JobExecution         as projection on batch.job_execution;
    entity JobExecutionContext  as projection on batch.job_execution_context;
    entity JobExecutionParam    as projection on batch.job_execution_params;
    entity StepExecution        as projection on batch.step_execution;
    entity StepExecutionContext as projection on batch.step_execution_context;
    entity ImportStructure      as projection on zzdt.ImportStructure;
    entity ImplementedByClass   as projection on zzdt.ImplementedByClass;
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
    ID         @UI.Hidden;
    FileName   @UI.Hidden;
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
            }
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
    ],
    LineItem              : [
        {Value: to_Config.ObjectName},
        {Value: Attachment},
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
    ImplementedByClass @Common: {ValueList: {
        $Type         : 'Common.ValueListType',
        CollectionPath: 'ImplementedByClass',
        Parameters    : [
            {
                $Type            : 'Common.ValueListParameterInOut',
                LocalDataProperty: 'ImplementedByClass',
                ValueListProperty: 'Name'
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
            ID    : 'Function',
            Target: '@UI.FieldGroup#Function_FG',
            $Type : 'UI.ReferenceFacet',
            Label : 'Java Functions'
        // position       : 20
        },
        {
            ID    : 'File',
            Target: '@UI.FieldGroup#File_FG',
            $Type : 'UI.ReferenceFacet',
            Label : 'File Processing'
        // position       : 20
        },
        {
            ID    : 'Template',
            Target: '@UI.FieldGroup#Template_FG',
            $Type : 'UI.ReferenceFacet',
            Label : 'Template Area'
        // position       : 20
        }
    ],
    Identification         : [
        {Value: Object},
        {Value: ObjectName}
    ],
    FieldGroup #Function_FG: {
        $Type: 'UI.FieldGroupType',
        Data : [
            {Value: ImplementedByClass},
            {Value: StructName}
        ]
    },
    FieldGroup #File_FG    : {
        $Type: 'UI.FieldGroupType',
        Data : [
            {
                Value        : MimeType,
                ![@UI.Hidden]: true
            },
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
        {Value: ImplementedByClass},
        {
            Value        : MimeType,
            ![@UI.Hidden]: true
        },
        {Value: SheetName},
        {Value: StructName},
        {Value: Template},
        {
            Value        : FileName,
            ![@UI.Hidden]: true
        },
        {Value: Object},
        {Value: Object},
    ]
};

annotate DataImportService.BatchImportConfig with @odata.draft.enabled;


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
