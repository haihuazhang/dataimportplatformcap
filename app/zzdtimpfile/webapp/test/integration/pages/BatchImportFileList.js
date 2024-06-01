sap.ui.define(['sap/fe/test/ListReport'], function(ListReport) {
    'use strict';

    var CustomPageDefinitions = {
        actions: {},
        assertions: {}
    };

    return new ListReport(
        {
            appId: 'zzdtimpfile',
            componentId: 'BatchImportFileList',
            entitySet: 'BatchImportFile'
        },
        CustomPageDefinitions
    );
});