package org.example

import org.apache.pdfbox.pdmodel.PDDocument
import technology.tabula.ObjectExtractor
import technology.tabula.extractors.BasicExtractionAlgorithm
import technology.tabula.Page
import technology.tabula.Table
import technology.tabula.Cell

import java.io.InputStream

class VisaBulletinTableExtractor {

    fun extractTables(pdfInputStream: InputStream): List<Table> {
        val allExtractedTables = mutableListOf<Table>()

        PDDocument.load(pdfInputStream).use { document ->
            val objectExtractor = ObjectExtractor(document)
            val basicExtractionAlgorithm = BasicExtractionAlgorithm()

            for (i in 1..document.numberOfPages) {
                val page: Page = objectExtractor.extract(i)
                val tables: List<Table> = basicExtractionAlgorithm.extract(page)
                allExtractedTables.addAll(tables)
            }
        }
        return allExtractedTables
    }
}
