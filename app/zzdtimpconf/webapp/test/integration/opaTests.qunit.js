sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'zzdtimpconf/test/integration/FirstJourney',
		'zzdtimpconf/test/integration/pages/BatchImportConfigList',
		'zzdtimpconf/test/integration/pages/BatchImportConfigObjectPage'
    ],
    function(JourneyRunner, opaJourney, BatchImportConfigList, BatchImportConfigObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('zzdtimpconf') + '/index.html'
        });

       
        JourneyRunner.run(
            {
                pages: { 
					onTheBatchImportConfigList: BatchImportConfigList,
					onTheBatchImportConfigObjectPage: BatchImportConfigObjectPage
                }
            },
            opaJourney.run
        );
    }
);