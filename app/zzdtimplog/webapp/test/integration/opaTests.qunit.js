sap.ui.require(
    [
        'sap/fe/test/JourneyRunner',
        'zzdtimplog/test/integration/FirstJourney',
		'zzdtimplog/test/integration/pages/JobInstanceList',
		'zzdtimplog/test/integration/pages/JobInstanceObjectPage',
		'zzdtimplog/test/integration/pages/JobExecutionObjectPage'
    ],
    function(JourneyRunner, opaJourney, JobInstanceList, JobInstanceObjectPage, JobExecutionObjectPage) {
        'use strict';
        var JourneyRunner = new JourneyRunner({
            // start index.html in web folder
            launchUrl: sap.ui.require.toUrl('zzdtimplog') + '/index.html'
        });

       
        JourneyRunner.run(
            {
                pages: { 
					onTheJobInstanceList: JobInstanceList,
					onTheJobInstanceObjectPage: JobInstanceObjectPage,
					onTheJobExecutionObjectPage: JobExecutionObjectPage
                }
            },
            opaJourney.run
        );
    }
);