sap.ui.define([
    "zzdtimpfile/ext/control/json/json-viewer"
], function () {
    "use strict";

    var BASE_WIDTH_REM = 50;
    var BASE_HEIGHT_REM = 33;
    var HEIGHT_PADDING_REM = 10;

    function getRemSizePx() {
        var rootFontSize = parseFloat(window.getComputedStyle(document.documentElement).fontSize);
        return Number.isFinite(rootFontSize) ? rootFontSize : 16;
    }

    function remToPx(rem) {
        return rem * getRemSizePx();
    }

    function pxToRem(px) {
        return px / getRemSizePx();
    }

    function getJsonViewerDom() {
        var oViewer = sap.ui.getCore().byId("jsonViewer");
        return oViewer && oViewer.getDomRef ? oViewer.getDomRef() : null;
    }

    function applyDialogSize(oDialog) {
        var oViewerDom = getJsonViewerDom();
        if (!oViewerDom) {
            oDialog.setContentWidth(BASE_WIDTH_REM + "rem");
            oDialog.setContentHeight(BASE_HEIGHT_REM + "rem");
            return;
        }

        var heightRem = Math.max(
            BASE_HEIGHT_REM,
            pxToRem(oViewerDom.scrollHeight + remToPx(HEIGHT_PADDING_REM))
        );

        oDialog.setContentWidth(BASE_WIDTH_REM + "rem");
        oDialog.setContentHeight(heightRem + "rem");
    }

    function setupAutoResize(oDialog) {
        if (oDialog._jsonAutoResizeSetupDone) {
            return;
        }

        oDialog._jsonAutoResizeSetupDone = true;
        oDialog.attachAfterOpen(function () {
            var oViewerDom = getJsonViewerDom();
            if (!oViewerDom) {
                applyDialogSize(oDialog);
                return;
            }

            if (!oDialog._jsonToggleResizeHandler) {
                oDialog._jsonToggleResizeHandler = function (oEvent) {
                    var target = oEvent && oEvent.target;
                    if (!target || !target.classList || !target.classList.contains("json__toggle")) {
                        return;
                    }

                    window.requestAnimationFrame(function () {
                        applyDialogSize(oDialog);
                    });
                };
            }

            if (oDialog._jsonResizeBoundDom !== oViewerDom) {
                if (oDialog._jsonResizeBoundDom && oDialog._jsonToggleResizeHandler) {
                    oDialog._jsonResizeBoundDom.removeEventListener("change", oDialog._jsonToggleResizeHandler);
                }
                oViewerDom.addEventListener("change", oDialog._jsonToggleResizeHandler);
                oDialog._jsonResizeBoundDom = oViewerDom;
            }

            applyDialogSize(oDialog);
        });
    }

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
                    setupAutoResize(oDialog);
                    oDialog.setContentWidth(BASE_WIDTH_REM + "rem");
                    oDialog.setContentHeight(BASE_HEIGHT_REM + "rem");
                    
                    sap.ui.getCore().byId("jsonViewer").setContent(
                        // viewers.default.toJSON(oJSON)
                        jsonViewer(oJSON, false)
                    );

                    applyDialogSize(oDialog);
                    oDialog.open();
                });

                this.onJSONViewDialogClose = function (oEvent) {
                    this.pDialog.then((oDialog) => oDialog.close());
                }
            }
        }
    };
});
