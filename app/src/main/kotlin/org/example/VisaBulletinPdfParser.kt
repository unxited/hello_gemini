package org.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream
import kotlin.text.RegexOption

class VisaBulletinPdfParser {

    private val client = HttpClient(CIO)

    suspend fun downloadPdfAndExtractText(pdfUrl: String): String {
        val response: HttpResponse = client.get(pdfUrl)
        val pdfBytes = response.readBytes()

        PDDocument.load(ByteArrayInputStream(pdfBytes)).use { document ->
            val stripper = PDFTextStripper()
            return stripper.getText(document)
        }
    }

    fun parseVisaBulletinText(pdfText: String): Map<String, List<VisaCategory>> {
        val parsedData = mutableMapOf<String, List<VisaCategory>>()

        // Regex to find the section for Final Action Dates for Employment-Based Preferences
        // Using (?s) flag to make . match all characters including newlines
        val finalActionEmploymentRegex = """(?s)FINAL ACTION DATES FOR EMPLOYMENT-BASED PREFERENCES\n(.+?)(?=\n\n|\Z)""".toRegex()
        val finalActionEmploymentMatch = finalActionEmploymentRegex.find(pdfText)

        finalActionEmploymentMatch?.let { matchResult ->
            val sectionText = matchResult.groupValues[1]
            println("\n--- Employment-Based Section Text ---")
            println(sectionText)
            println("--- End Employment-Based Section Text ---")

            val categories = parseEmploymentBasedTable(sectionText)
            parsedData["FINAL ACTION DATES FOR EMPLOYMENT-BASED PREFERENCES"] = categories
        }

        // Add similar logic for Dates for Filing, Family-Sponsored, etc.

        return parsedData
    }

    private fun parseEmploymentBasedTable(sectionText: String): List<VisaCategory> {
        val categories = mutableListOf<VisaCategory>()

        val lines = sectionText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        println("Debug: Lines after split and trim: $lines")

        // Identify row headers (visa categories like 1st, 2nd, 3rd, etc.)
        val rowHeaders = listOf(
            "1st", "2nd", "3rd", "Other Workers", "4th", "Certain Religious Workers", "5th Unreserved", "Rural (20%)", "High Unemployment (10%)", "Infrastructure (2%)"
        )

        // Step 1: Find the header block and collect header lines
        var headerBlockStartLineIndex = -1
        for (i in lines.indices) {
            val line = lines[i]
            // Look for a line that contains some of the key column headers
            if (line.contains("All Charge-") && line.contains("CHINA-") && line.contains("INDIA")) {
                headerBlockStartLineIndex = i
                break
            }
        }

        if (headerBlockStartLineIndex == -1) {
            println("Debug: Header block start line not found.")
            return categories
        }

        var headerLines = mutableListOf<String>()
        var dataStartIndex = -1
        // Collect lines from headerBlockStartLineIndex until a data row is encountered
        for (i in headerBlockStartLineIndex until lines.size) {
            val line = lines[i]
            if (rowHeaders.any { line.startsWith(it) }) {
                dataStartIndex = i
                break
            } else {
                headerLines.add(line)
            }
        }

        if (dataStartIndex == -1) {
            println("Debug: Data start index not found after header block.")
            return categories
        }

        val fullHeader = headerLines.joinToString(" ")
        println("Debug: Full Header: $fullHeader")

        // Extract actual column names from the fullHeader using a more flexible regex
        val extractedColumnHeaders = mutableListOf<String>()
        val columnHeaderRegex = Regex("(All Chargeability Areas Except Those Listed Separately|CHINA-mainland born|INDIA|MEXICO|PHILIPPINES)")
        columnHeaderRegex.findAll(fullHeader).forEach { matchResult ->
            extractedColumnHeaders.add(matchResult.value)
        }
        println("Debug: Extracted Column Headers: $extractedColumnHeaders")

        // Step 2: Iterate through data lines and extract values
        for (i in dataStartIndex until lines.size) {
            val line = lines[i]
            println("Debug: Processing data line: '$line'")

            var matchedRowHeader: String? = null
            for (header in rowHeaders) {
                if (line.startsWith(header)) {
                    matchedRowHeader = header
                    break
                }
            }

            if (matchedRowHeader != null) {
                val rowData = mutableListOf<String>()
                val lineAfterRowHeader = line.substringAfter(matchedRowHeader).trim()
                println("Debug: Line after row header: '$lineAfterRowHeader'")

                // Use a flexible regex to capture dates/status for each column
                val dataRegex = Regex("([0-9]{2}[A-Z]{3}[0-9]{2}|C|U)") // More specific regex for dates/C/U
                val matches = dataRegex.findAll(lineAfterRowHeader).map { it.value }.toList()
                rowData.addAll(matches)

                println("Debug: Extracted row data: $rowData")

                // Map extracted values to VisaCategory objects
                if (rowData.size == extractedColumnHeaders.size) {
                    for (j in extractedColumnHeaders.indices) {
                        val region = extractedColumnHeaders[j]
                        val date = rowData[j]
                        categories.add(VisaCategory(matchedRowHeader, region, date))
                    }
                } else {
                    println("Debug: Mismatch in rowData size (${rowData.size}) and extractedColumnHeaders size (${extractedColumnHeaders.size}) for line: '$line'")
                }
            }
        }

        return categories
    }
}