sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'zzdtimpfile/test/integration/FirstJourney',
		'zzdtimpfile/test/integration/pages/BatchImportFileList',
		'zzdtimpfile/test/integration/pages/BatchImportFileObjectPage'
    ],
    function(JourneyRunner, opaJourney, BatchImportFileList, BatchImportFileObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('zzdtimpfile') + '/index.html'
        });

       
        JourneyRunner.run(
            {
                pages: { 
					onTheBatchImportFileList: BatchImportFileList,
					onTheBatchImportFileObjectPage: BatchImportFileObjectPage
                }
            },
            opaJourney.run
        );
    }
);