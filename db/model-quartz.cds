namespace qrtz;


@cds.persistence.exists
entity JOB_DETAILS {
    key SCHED_NAME        : String(120) not null;
    key JOB_NAME          : String(200) not null;
    key JOB_GROUP         : String(200) not null;
        DESCRIPTION       : String(250);
        JOB_CLASS_NAME    : String(128) not null;
        IS_DURABLE        : String(2) not null;
        IS_NONCONCURRENT  : String(2) not null;
        IS_UPDATE_DATA    : String(2) not null;
        REQUESTS_RECOVERY : String(2) not null;
        JOB_DATA          : LargeBinary;
// to_Executions   : Association to many job_execution
//                       on JOB_INSTANCE_ID = to_Executions.JOB_INSTANCE_ID;
}


@cds.persistence.exists
entity TRIGGERS {
    key SCHED_NAME     : String(120) NOT null;
    key TRIGGER_NAME   : String(200) NOT null;
    key TRIGGER_GROUP  : String(200) NOT null;
        JOB_NAME       : String(200) NOT null;
        JOB_GROUP      : String(200) NOT null;
        DESCRIPTION    : String(250);
        NEXT_FIRE_TIME : Int64;
        PREV_FIRE_TIME : Int64;
        PRIORITY       : Int64;
        TRIGGER_STATE  : String(16) NOT null;
        TRIGGER_TYPE   : String(8) NOT null;
        START_TIME     : Int64 NOT null;
        END_TIME       : Int64;
        CALENDAR_NAME  : String(200);
        MISFIRE_INSTR  : Int16;
        JOB_DATA       : LargeBinary;
        to_JobDetails  : Association to one JOB_DETAILS
                             on  SCHED_NAME = to_JobDetails.SCHED_NAME
                             and JOB_NAME   = to_JobDetails.JOB_NAME
                             and JOB_GROUP  = to_JobDetails.JOB_GROUP;
}

@cds.persistence.exists
entity SIMPLE_TRIGGERS {
    key SCHED_NAME      : String(120) NOT null;
    key TRIGGER_NAME    : String(200) NOT null;
    key TRIGGER_GROUP   : String(200) NOT null;
        REPEAT_COUNT    : Int64 NOT null;
        REPEAT_INTERVAL : Int64 NOT null;
        TIMES_TRIGGERED : Int64 NOT null;
        to_Triggers     : Association to one TRIGGERS
                              on  SCHED_NAME    = to_Triggers.SCHED_NAME
                              and TRIGGER_NAME  = to_Triggers.TRIGGER_NAME
                              and TRIGGER_GROUP = to_Triggers.TRIGGER_GROUP;
// PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
// FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
}

@cds.persistence.exists
entity SIMPROP_TRIGGERS {
    key SCHED_NAME    : String(120) NOT null;
    key TRIGGER_NAME  : String(200) NOT null;
    key TRIGGER_GROUP : String(200) NOT null;
        STR_PROP_1    : String(512) NULL;
        STR_PROP_2    : String(512) NULL;
        STR_PROP_3    : String(512) NULL;
        INT_PROP_1    : Integer NULL;
        INT_PROP_2    : Integer NULL;
        LONG_PROP_1   : Int64;
        LONG_PROP_2   : Int64 NULL;
        DEC_PROP_1    : Decimal(13, 4) NULL;
        DEC_PROP_2    : Decimal(13, 4) NULL;
        BOOL_PROP_1   : String(1) NULL;
        BOOL_PROP_2   : String(1) NULL;
        to_Triggers   : Association to one TRIGGERS
                            on  SCHED_NAME    = to_Triggers.SCHED_NAME
                            and TRIGGER_NAME  = to_Triggers.TRIGGER_NAME
                            and TRIGGER_GROUP = to_Triggers.TRIGGER_GROUP;
// PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
// FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
}


@cds.persistence.exists
entity CRON_TRIGGERS {
    key SCHED_NAME      : String(120) NOT null;
    key TRIGGER_NAME    : String(200) NOT null;
    key TRIGGER_GROUP   : String(200) NOT null;
        CRON_EXPRESSION : String(120) NOT null;
        TIME_ZONE_ID    : String(80);
        to_Triggers     : Association to one TRIGGERS
                              on  SCHED_NAME    = to_Triggers.SCHED_NAME
                              and TRIGGER_NAME  = to_Triggers.TRIGGER_NAME
                              and TRIGGER_GROUP = to_Triggers.TRIGGER_GROUP;
// PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
// FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
}

@cds.persistence.exists
entity BLOB_TRIGGERS {
    key SCHED_NAME    : String(120) NOT null;
    key TRIGGER_NAME  : String(200) NOT null;
    key TRIGGER_GROUP : String(200) NOT null;
        BLOB_DATA     : LargeBinary NULL;
        to_Triggers   : Association to one TRIGGERS
                            on  SCHED_NAME    = to_Triggers.SCHED_NAME
                            and TRIGGER_NAME  = to_Triggers.TRIGGER_NAME
                            and TRIGGER_GROUP = to_Triggers.TRIGGER_GROUP;
}


@cds.persistence.exists
entity CALENDARS {
    key SCHED_NAME    : String(120) NOT null;
    key CALENDAR_NAME : String(200) NOT null;
        DESCRIPTION   : String(250) NULL;
        CALENDAR      : LargeBinary NOT null;
}

@cds.persistence.exists
entity PAUSED_TRIGGER_GRPS {
    key SCHED_NAME    : String(120) NOT null;
    key TRIGGER_GROUP : String(200) NOT null;
}

@cds.persistence.exists
entity FIRED_TRIGGERS {
    key SCHED_NAME        : String(120) NOT null;
    key ENTRY_ID          : String(95) NOT null;
        TRIGGER_NAME      : String(200) NOT null;
        TRIGGER_GROUP     : String(200) NOT null;
        INSTANCE_NAME     : String(200) NOT null;
        FIRED_TIME        : Int64 NOT null;
        SCHED_TIME        : Int64 NOT null;
        PRIORITY          : Int64 NOT null;
        STATE             : String(16) NOT null;
        JOB_NAME          : String(200) NULL;
        JOB_GROUP         : String(200) NULL;
        IS_NONCONCURRENT  : String(1) NULL;
        REQUESTS_RECOVERY : String(1) NULL;
}

@cds.persistence.exists
entity SCHEDULER_STATE {
    key SCHED_NAME        : String(120) NOT null;
    key INSTANCE_NAME     : String(200) NOT null;
        LAST_CHECKIN_TIME : Int64 NOT null;
        CHECKIN_INTERVAL  : Int64 NOT null;
}


@cds.persistence.exists
entity LOCKS {
    key SCHED_NAME : String(120) NOT null;
    key LOCK_NAME  : String(40) NOT null;
}
