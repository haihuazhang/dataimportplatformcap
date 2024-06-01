using {qrtz} from '../db/model-quartz';


service QuartzService {
    entity QuartzJobDetails  as projection on qrtz.JOB_DETAILS;
    entity Triggers          as projection on qrtz.TRIGGERS;
    entity SimpleTriggers    as projection on qrtz.SIMPLE_TRIGGERS;
    entity SimpropTriggers   as projection on qrtz.SIMPROP_TRIGGERS;
    entity CronTriggers      as projection on qrtz.CRON_TRIGGERS;
    entity BlobTriggers      as projection on qrtz.BLOB_TRIGGERS;
    entity Calendars         as projection on qrtz.CALENDARS;
    entity PausedTriggerGrps as projection on qrtz.PAUSED_TRIGGER_GRPS;
    entity FiredTriggers     as projection on qrtz.FIRED_TRIGGERS;
    entity SchedulerState    as projection on qrtz.SCHEDULER_STATE;
    entity Locks             as projection on qrtz.LOCKS;
    action SimpleQuartzTrigger() returns Boolean;
}
