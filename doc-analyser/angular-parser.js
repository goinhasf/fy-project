var expressions = require('angular-expressions');
var assign = require("lodash/assign");

module.exports = function () {
    expressions.filters.lower = function (input) {
        if (!input) return input;
        return input.toLowerCase();
    }
    expressions.filters.sumby = function (input, field) {
        if (!input) return input;
        return input.reduce(function (sum, object) {
            return sum + object[field];
        }, 0);
    }

    return function angularParser(tag) {
        if (tag === '.') {
            return {
                get: function (s) { return s; }
            };
        }
        const expr = expressions.compile(
            tag.replace(/(’|‘)/g, "'").replace(/(“|”)/g, '"')
        );
        return {
            get: function (scope, context) {
                let obj = {};
                const scopeList = context.scopeList;
                const num = context.num;
                for (let i = 0, len = num + 1; i < len; i++) {
                    obj = assign(obj, scopeList[i]);
                }
                return expr(scope, obj);
            }
        };
    }
}