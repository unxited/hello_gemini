package org.example

import kotlinx.coroutines.runBlocking
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.io.ByteArrayInputStream

fun findTableByTitle(tables: List<technology.tabula.Table>, title: String): technology.tabula.Table? {
    return tables.firstOrNull { table ->
        table.rows.any { row ->
            row.any { cell ->
                cell.text.contains(title, ignoreCase = true)
            }
        }
    }
}

fun main() = runBlocking {
    val client = HttpClient(CIO)
    val tableExtractor = VisaBulletinTableExtractor()
    val parser = VisaBulletinParser()
    val fullPdfUrl = "https://travel.state.gov/content/dam/visas/Bulletins/visabulletin_July2025.pdf"

    println("Downloading PDF from: $fullPdfUrl")
    val pdfBytes = client.get(fullPdfUrl).readBytes()
    println("Extracting tables from PDF...")

    val extractedTables = tableExtractor.extractTables(ByteArrayInputStream(pdfBytes))

    if (extractedTables.isNotEmpty()) {
        val finalActionTable = findTableByTitle(extractedTables, "FINAL ACTION DATES FOR EMPLOYMENT-BASED PREFERENCE CASES")
        if (finalActionTable != null) {
            val finalActionEmploymentData = finalActionTable.getRows().map { row ->
                row.map { cell -> cell.getText() }
            }
            println("\n--- Parsing Final Action Dates for Employment-Based ---")
            val finalActionCategories = parser.parseEmploymentTableData(finalActionEmploymentData)
            finalActionCategories.forEach { category ->
                println("  Category: ${category.category}, Region: ${category.region}, Date: ${category.date}")
            }
        } else {
            println("Could not find 'Final Action Dates' table.")
        }

        val datesForFilingTable = findTableByTitle(extractedTables, "DATES FOR FILING OF EMPLOYMENT-BASED VISA APPLICATIONS")
        if (datesForFilingTable != null) {
            val datesForFilingEmploymentData = datesForFilingTable.getRows().map { row ->
                row.map { cell -> cell.getText() }
            }
            println("\n--- Parsing Dates for Filing of Employment-Based ---")
            val datesForFilingCategories = parser.parseEmploymentTableData(datesForFilingEmploymentData)
            datesForFilingCategories.forEach { category ->
                println("  Category: ${category.category}, Region: ${category.region}, Date: ${category.date}")
            }
        } else {
            println("Could not find 'Dates for Filing' table.")
        }
    } else {
        println("No tables extracted from PDF.")
    }
}