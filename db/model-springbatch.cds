namespace batch;

@cds.persistence.exists
entity job_instance {
    key JOB_INSTANCE_ID : Int64;
        VERSION         : Int64;
        JOB_NAME        : String(100);
        JOB_KEY         : String(36);
        to_Executions   : Association to many job_execution
                              on JOB_INSTANCE_ID = to_Executions.JOB_INSTANCE_ID;
}

@cds.persistence.exists
entity job_execution {
    key JOB_EXECUTION_ID  : Int64;
        VERSION           : Int64;
        JOB_INSTANCE_ID   : Int64;
        CREATE_TIME       : Timestamp;
        START_TIME        : Timestamp;
        END_TIME          : Timestamp;
        STATUS            : String(10);
        EXIT_CODE         : String(2500);
        EXIT_MESSAGE      : String(2500);
        LAST_UPDATED      : Timestamp;
        to_Instance       : Association to one job_instance
                                on JOB_INSTANCE_ID = to_Instance.JOB_INSTANCE_ID;
        to_Context        : Association to one job_execution_context
                                on JOB_EXECUTION_ID = to_Context.JOB_EXECUTION_ID;
        to_Params         : Association to many job_execution_params
                                on JOB_EXECUTION_ID = to_Params.JOB_EXECUTION_ID;
        to_StepExecutions : Association to many step_execution
                                on JOB_EXECUTION_ID = to_StepExecutions.JOB_EXECUTION_ID;
}

@cds.persistence.exists
entity job_execution_context {
    key JOB_EXECUTION_ID   : Int64;
        SHORT_CONTEXT      : String(2500);
        SERIALIZED_CONTEXT : LargeString;
}

@cds.persistence.exists
entity job_execution_params {
    key JOB_EXECUTION_ID : Int64;
    key PARAMETER_NAME   : String(100);
    key PARAMETER_TYPE   : String(100);
        PARAMETER_VALUE  : String(2500);
        IDENTIFYING      : String(1);
}

@cds.persistence.exists
entity step_execution {
    key STEP_EXECUTION_ID  : Int64;
        VERSION            : Int64;
        STEP_NAME          : String(100);
        JOB_EXECUTION_ID   : Int64;
        CREATE_TIME        : Timestamp;
        START_TIME         : Timestamp;
        END_TIME           : Timestamp;
        STATUS             : String(10);
        COMMIT_COUNT       : Int64;
        READ_COUNT         : Int64;
        FILTER_COUNT       : Int64;
        WRITE_COUNT        : Int64;
        READ_SKIP_COUNT    : Int64;
        WRITE_SKIP_COUNT   : Int64;
        PROCESS_SKIP_COUNT : Int64;
        ROLLBACK_COUNT     : Int64;
        EXIT_CODE          : String(20);
        EXIT_MESSAGE       : String(2500);
        LAST_UPDATED       : Timestamp;
        to_JobExecution    : Association to one job_execution
                                 on JOB_EXECUTION_ID = to_JobExecution.JOB_EXECUTION_ID;
        to_Context         : Association to one step_execution_context
                                 on STEP_EXECUTION_ID = to_Context.STEP_EXECUTION_ID;
}

@cds.persistence.exists
entity step_execution_context {
    key STEP_EXECUTION_ID  : Int64;
        SHORT_CONTEXT      : String(2500);
        SERIALIZED_CONTEXT : LargeString;
}
