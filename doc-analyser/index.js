const MAX_FILE_SIZE = process.env.MAX_FILE_SIZE_MB * 1000 * 1000
const SERVICE_PATH_PREFIX = process.env.SERVICE_PATH_PREFIX || '/api/doc'

const express = require('express')
const Inspector = require('./document-inspector')
const DocumentParser = require('./document-parser')
const app = express()
const port = process.env.PORT | 3000
const multer = require('multer')
const docxConverter = require('docx-pdf');
const fs = require('fs')
const tmp = require('tmp');
const { promisify } = require('util')

const readFilePromise = promisify(fs.readFile)

const storage = multer.diskStorage({
  filename: (req, file, cb) => {
    cb(null, Date.now() + '-' + file.originalname)
  },
})

const upload = multer({
  storage: storage,
  limits: {
    fileSize: MAX_FILE_SIZE
  }
})

app.use(express.json())

app.post(`${SERVICE_PATH_PREFIX}/inspect`, upload.single('file'), async (req, res) => {
  if (req.file) {
    const buffer = await readFilePromise(req.file.path)
    res.send(Inspector(buffer))
  } else {
    res.status(400).send("A file of type docx was expected but none found")
  }
})

app.post(`${SERVICE_PATH_PREFIX}/fill`, upload.single('file'), async (req, res) => {
  const data = req.body.data
  if (data && req.file) {
    try {
      const parsed = JSON.parse(data)
      const generatedFilePath = await DocumentParser(req.file.path, parsed)
      const buffer = await readFilePromise(generatedFilePath)

      res.setHeader("Content-Disposition", "attachment")

      if (req.query['pdf'] == "true") {
        res.setHeader("Content-Type", "application/pdf")
        const tmpFilePath = tmp.tmpNameSync()
        docxConverter(generatedFilePath, `${tmpFilePath}.pdf`, async (err, result) => {
          if (err) res.status(500).send("An error occurred converting to pdf")
          else {
            const generatedFile = await readFilePromise(result.filename)
            res.send(generatedFile)
          }
        })
      } else {
        res.setHeader("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        res.end(buffer)
      }
    } catch (error) {
      console.log(error)
      res.status(400).send(`Error occurred ${error}`)
    }
  }
})

app.listen(port, () => {
  console.log(`Example app listening at http://localhost:${port}`)
})