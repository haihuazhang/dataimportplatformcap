sap.ui.define([
    "sap/m/Button",
    "sap/m/Dialog",
    "sap/m/MessageToast",
    "sap/ui/core/HTML",
    "zzdtimpfile/ext/control/json/json-viewer"
], function (Button, Dialog, MessageToast, HTML, jsonViewer) {
    "use strict";

    let oPreviewDialog;
    let oJsonContent;

    function getViewFromControl(oControl) {
        let oParent = oControl;
        while (oParent) {
            if (oParent.isA && oParent.isA("sap.ui.core.mvc.View")) {
                return oParent;
            }
            oParent = oParent.getParent && oParent.getParent();
        }
        return null;
    }

    function getView(oControllerContext) {
        if (oControllerContext && typeof oControllerContext.getView === "function") {
            return oControllerContext.getView();
        }
        if (oControllerContext && oControllerContext.base
            && typeof oControllerContext.base.getView === "function") {
            return oControllerContext.base.getView();
        }
        return null;
    }

    function resolveSelectedContext(oBindingContext, aSelectedContexts) {
        if (Array.isArray(aSelectedContexts) && aSelectedContexts.length > 0) {
            return aSelectedContexts[0];
        }
        if (Array.isArray(oBindingContext) && oBindingContext.length > 0) {
            return oBindingContext[0];
        }
        if (oBindingContext && typeof oBindingContext.requestProperty === "function") {
            return oBindingContext;
        }
        return null;
    }

    async function openPreviewByContext(oContext, oView) {
        if (!oContext || !oView) {
            return;
        }

        let rawJson = "{}";
        try {
            const value = await oContext.requestProperty("DataJson");
            rawJson = typeof value === "string" && value.trim() ? value : "{}";
        } catch (error) {
            rawJson = "{}";
        }

        let parsedJson = {};
        try {
            parsedJson = JSON.parse(rawJson);
        } catch (error) {
            parsedJson = { raw: rawJson };
        }

        if (!oPreviewDialog) {
            createPreviewDialog(oView);
        }

        oJsonContent.setContent(jsonViewer(parsedJson, false));
        oPreviewDialog.open();
    }

    function createPreviewDialog(oView) {
        oJsonContent = new HTML({
            content: "",
            sanitizeContent: true
        });

        oPreviewDialog = new Dialog({
            title: oView.getModel("i18n").getResourceBundle().getText("previewJsonDialogTitle"),
            contentWidth: "70%",
            contentHeight: "70%",
            stretchOnPhone: true,
            content: [oJsonContent],
            endButton: new Button({
                text: oView.getModel("i18n").getResourceBundle().getText("previewJsonClose"),
                press: function () {
                    oPreviewDialog.close();
                }
            })
        });

        oView.addDependent(oPreviewDialog);
    }

    async function previewDataJson(oBindingContext, aSelectedContexts) {
        const oView = getView(this);
        if (!oView) {
            return;
        }

        const oContext = resolveSelectedContext(oBindingContext, aSelectedContexts);
        if (!oContext) {
            MessageToast.show(oView.getModel("i18n").getResourceBundle().getText("previewJsonNoSelection"));
            return;
        }

        await openPreviewByContext(oContext, oView);
    }

    async function previewDataJsonInRow(oEvent) {
        const oSource = oEvent && oEvent.getSource ? oEvent.getSource() : null;
        const oContext = oSource && oSource.getBindingContext ? oSource.getBindingContext() : null;
        const oView = getViewFromControl(oSource);
        await openPreviewByContext(oContext, oView);
    }

    return {
        previewDataJson: previewDataJson,
        previewDataJsonInRow: previewDataJsonInRow
    };
});
