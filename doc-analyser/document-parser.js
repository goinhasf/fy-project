var PizZip = require('pizzip');
var Docxtemplater = require('docxtemplater');
var fs = require("fs");
var tmp = require('tmp');
const { promisify } = require('util');
const angularParser = require('./angular-parser')

// The error object contains additional information when logged with JSON.stringify (it contains a properties object containing all suberrors).
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

module.exports = (file, data) => {

    return new Promise(async (resolve, reject) => {

        const readFilePromise = promisify(fs.readFile)
        const buffer = await readFilePromise(file)

        var zip = new PizZip(buffer);
        var doc;
        try {
            doc = new Docxtemplater(zip, { parser: angularParser() });
        } catch (error) {
            // Catch compilation errors (errors caused by the compilation of the template : misplaced tags)
            errorHandler(error);
        }

        //set the templateVariables
        doc.setData(JSON.parse(data));

        try {
            // render the document (replace all occurences of {first_name} by John, {last_name} by Doe, ...)
            doc.render()
        }
        catch (error) {
            // Catch rendering errors (errors relating to the rendering of the template : angularParser throws an error)
            errorHandler(error);
        }

        const newZip = doc.getZip()

        tmp.file((err, path, fd, cleanup) => {
            if (err) reject(err)
            else {
                // Returns a buffer
                fs.writeFile(path, newZip.generate({ type: 'nodebuffer' }), err => {
                    if (err) reject(err)
                    else resolve(path)
                })
            }
        })

    })

}
