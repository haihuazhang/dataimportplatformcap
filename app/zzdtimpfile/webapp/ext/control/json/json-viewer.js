sap.ui.define([], function () {
    "use strict";

    var TEMPLATES = {
        item: '<div class="json__item"><div class="json__key">%KEY%</div><div class="json__value json__value--%TYPE%">%VALUE%</div></div>',
        itemCollapsible: '<label class="json__item json__item--collapsible"><input type="checkbox" class="json__toggle"/><div class="json__key">%KEY%</div><div class="json__value json__value--type-%TYPE%">%VALUE%</div>%CHILDREN%</label>',
        itemCollapsibleOpen: '<label class="json__item json__item--collapsible"><input type="checkbox" checked class="json__toggle"/><div class="json__key">%KEY%</div><div class="json__value json__value--type-%TYPE%">%VALUE%</div>%CHILDREN%</label>'
    };

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function createItem(key, value, type) {
        var displayValue = type === "string" ? '"' + value + '"' : value;

        return TEMPLATES.item
            .replace("%KEY%", escapeHtml(key))
            .replace("%VALUE%", escapeHtml(displayValue))
            .replace("%TYPE%", escapeHtml(type));
    }

    function createCollapsibleItem(key, type, children, collapsible) {
        var tpl = collapsible ? "itemCollapsibleOpen" : "itemCollapsible";

        return TEMPLATES[tpl]
            .replace("%KEY%", escapeHtml(key))
            .replace("%VALUE%", escapeHtml(type))
            .replace("%TYPE%", escapeHtml(type))
            .replace("%CHILDREN%", children);
    }

    function handleChildren(key, value, type, collapsible) {
        var html = "";

        Object.keys(value || {}).forEach(function (item) {
            html += handleItem(item, value[item], collapsible);
        });

        return createCollapsibleItem(key, type, html, collapsible);
    }

    function handleItem(key, value, collapsible) {
        if (value === null) {
            return createItem(key, "null", "null");
        }

        if (Array.isArray(value)) {
            return handleChildren(key, value, "array", collapsible);
        }

        var type = typeof value;
        if (type === "object") {
            return handleChildren(key, value, type, collapsible);
        }

        return createItem(key, value, type);
    }

    return function jsonViewer(json, collapsible) {
        var result = '<div class="json">';

        Object.keys(json || {}).forEach(function (item) {
            result += handleItem(item, json[item], collapsible);
        });

        result += "</div>";

        return result;
    };
});
