using DataImportService as service from '../../srv/service-dt';

// annotate service.JobInstance with @(
//     UI.Facets : [
//         {
//             $Type : 'UI.ReferenceFacet',
//             ID : 'idIdentification',
//             Label : '{i18n>Basic}',
//             Target : '@UI.Identification',
//         },
//         {
//             $Type : 'UI.ReferenceFacet',
//             ID : 'JobExecution',
//             Target : 'to_Executions/@UI.LineItem',
//             Label : '{i18n>JobExecutions}',
//         },
//     ]
// );
// annotate service.JobExecution with @(
//     UI.Facets : [
//         {
//             $Type : 'UI.ReferenceFacet',
//             ID : 'idIdentification',
//             Label : '{i18n>Basic}',
//             Target : '@UI.Identification',
//         },
//         {
//             $Type : 'UI.ReferenceFacet',
//             ID : 'Function',
//             Target : '@UI.FieldGroup#Execution',
//             Label : '{i18n>Execution}',
//         },
//         {
//             $Type : 'UI.ReferenceFacet',
//             ID : 'Status',
//             Target : '@UI.FieldGroup#Status',
//             Label : '{i18n>Status}',
//         },
//         {
//             $Type : 'UI.ReferenceFacet',
//             ID : 'Context',
//             Target : 'to_Context/@UI.Identification',
//             Label : '{i18n>Context}',
//         },
//         {
//             $Type : 'UI.ReferenceFacet',
//             ID : 'Parameters',
//             Target : 'to_Params/@UI.LineItem',
//             Label : '{i18n>Parameters}',
//         },
//         {
//             $Type : 'UI.ReferenceFacet',
//             ID : 'JobExecution',
//             Target : 'to_StepExecutions/@UI.LineItem',
//             Label : '{i18n>Steps}',
//         },
//     ]
// );
