using {example} from '../db/model-example';

service ExampleService {
    entity ZZTable01 as projection on example.table01;
}

annotate ExampleService.ZZTable01 with @odata.draft.enabled;
