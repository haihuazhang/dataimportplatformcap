namespace scheduler;

using {
    cuid,
    managed
} from '@sap/cds/common';


entity Scheduler : cuid {
    JobName     : String(200);
    TriggerName : String(200);
    
}
