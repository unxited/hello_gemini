package org.example

class VisaBulletinParser {

    fun parseEmploymentTableData(tableData: List<List<String>>): List<VisaCategory> {
        if (tableData.isEmpty()) return emptyList()

        // Clean the data by trimming and removing line breaks
        val cleanedData = tableData.map { row ->
            row.map { cell -> cell.replace("\r", " ").trim() }
        }

        // 1. Find the header row index
        val headerKeywords = listOf("CHINA", "INDIA", "MEXICO", "PHILIPPINES")
        val headerRowIndex = cleanedData.indexOfFirst { row ->
            headerKeywords.any { keyword -> row.joinToString().contains(keyword) }
        }
        if (headerRowIndex == -1) return emptyList()

        // 2. Combine header rows if they are split
        val combinedHeader = cleanedData[headerRowIndex].toMutableList()
        if (headerRowIndex > 0) {
            val previousRow = cleanedData[headerRowIndex - 1]
            for (i in combinedHeader.indices) {
                if (i < previousRow.size && previousRow[i].isNotBlank()) {
                    combinedHeader[i] = "${previousRow[i]} ${combinedHeader[i]}"
                }
            }
        }
        
        // Manual correction for known header issues
        val header = combinedHeader.map { 
            it.replace("mainland born", "mainland-born")
              .replace("All Chargeability Areas Except Those Listed", "All Chargeability Areas")
        }

        // 3. Find the starting row for data
        val dataStartRowIndex = cleanedData.indexOfFirst { row ->
            row.firstOrNull()?.let {
                it.contains("1st") || it.contains("2nd") || it.contains("3rd")
            } ?: false
        }
        if (dataStartRowIndex == -1) return emptyList()

        val categories = mutableListOf<VisaCategory>()
        
        // 4. Process data rows
        for (i in dataStartRowIndex until cleanedData.size) {
            val row = cleanedData[i]
            val rowTitle = row.firstOrNull()?.trim() ?: continue
            
            // Skip rows that are not data rows
            if (rowTitle.isBlank() || headerKeywords.any{ it in rowTitle}) continue

            for (j in 1 until header.size) {
                val region = header[j]
                val date = row.getOrNull(j)?.trim() ?: ""
                
                if (date.isNotBlank()) {
                     categories.add(VisaCategory(rowTitle, region, date))
                }
            }
        }

        return categories
    }
}