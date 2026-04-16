sap.ui.define(["sap/m/MessageToast", 
    // "zzhdappdmpimp/ext/list/JSONViewer",
    "zzdtimpfile/ext/control/json/json-viewer"
], function (MessageToast, JsonViewer1) {
    "use strict";
    return {
        previewButtonPressed: function (oEvent) {
            // MessageToast.show("Button pressed for item " + oEvent.getSource().getBindingContext().getObject().ID);
            if (oEvent.getSource().getBindingContext()) {
                var sJson = oEvent.getSource().getBindingContext().getProperty("DataJson");
                var oJSON = JSON.parse(sJson);


                this.pDialog ??= this.loadFragment({
                    name: "zzdtimpfile.ext.list.JSONViewDialog"
                });
                this.pDialog.then((oDialog) => {
                    
                    sap.ui.getCore().byId("jsonViewer").setContent(
                        // viewers.default.toJSON(oJSON)
                        jsonViewer(oJSON, false)
                    );
                    oDialog.open();
                });

                this.onJSONViewDialogClose = function (oEvent) {
                    this.pDialog.then((oDialog) => oDialog.close());
                }
            }
        }
    };
});
