var Docxtemplater = require('docxtemplater');
var InspectModule = require("docxtemplater/js/inspect-module");
var PizZip = require('pizzip');
const angularParser = require('./angular-parser')

function replaceErrors(key, value) {
    if (value instanceof Error) {
        return Object.getOwnPropertyNames(value).reduce(function (error, key) {
            error[key] = value[key];
            return error;
        }, {});
    }
    return value;
}

function errorHandler(error) {
    console.log(JSON.stringify({ error: error }, replaceErrors));

    if (error.properties && error.properties.errors instanceof Array) {
        const errorMessages = error.properties.errors.map(function (error) {
            return error.properties.explanation;
        }).join("\n");
        console.log('errorMessages', errorMessages);
        // errorMessages is a humanly readable message looking like this :
        // 'The tag beginning with "foobar" is unopened'
    }
    throw error;
}

function testValueForNumberType(value) {
    if (value.search("number") > -1 || value.search("amount") > -1 || value.search("price") > -1 || value.search("cost") > -1) {
        return true
    }
    return false;
}

module.exports = function inspect(content) {
    var zip = new PizZip(content);
    var iModule = InspectModule();
    try {
        const doc = new Docxtemplater(zip, { modules: [iModule], parser: angularParser() });
        doc.render()
        const tags = iModule.getStructuredTags()
        function findType(value) {
            if (testValueForNumberType(value)) {
                return "number"
            } else if (value.search("date") > -1) {
                return "date"
            } else if (value.search("time") > -1) {
                return "time"
            } else {
                return "string"
            }
        }

        function transformSingles(value) {
            const items = value.split('.')
            if (items.length == 1) {
                return {
                    cardinality: "single",
                    field: {
                        [value]: findType(value)
                    }
                }
            } else {
                function nest(level) {
                    if (level == items.length - 1) {
                        return { [items[level]]: findType(items[level]) }
                    }
                    return { [`${items[level]}`]: nest(level + 1) }
                }
                return {
                    cardinality: "single",
                    field: { [items[0]]: nest(1) }
                }
            }

        }

        function applyTransformations(arr) {
            return arr.filter((v, i, arr) => {
                if (v.value.indexOf("|") == -1) {
                    return v
                }
            }).map((v, i, arr) => {
                if (v.module && v.module == "loop") {

                    const subItems = v.subparsed.map((v, i, arr) => transformSingles(v.value))

                    return {
                        cardinality: "loop",
                        field: {
                            [v.value]: subItems
                        }
                    }
                } else {
                    return {
                        ...transformSingles(v.value)
                    }
                }
            })
        }
        return applyTransformations(tags)
    } catch (error) {
        // Catch compilation errors (errors caused by the compilation of the template : misplaced tags)
        errorHandler(error);
    }
}