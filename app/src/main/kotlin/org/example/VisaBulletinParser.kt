package org.example

import org.jsoup.nodes.Element

class VisaBulletinParser {

    fun parseEmploymentTableData(table: Element): List<VisaCategory> {
        val categories = mutableListOf<VisaCategory>()
        val rows = table.select("tr")

        if (rows.isEmpty()) return emptyList()

        // The header is the first row, clean up multiline headers
        val headerCells = rows.first()?.select("th, td") ?: return emptyList()
        val header = headerCells.map { it.text().trim() }

        // Data starts from the second row
        for (i in 1 until rows.size) {
            val row = rows[i]
            val cells = row.select("td")
            if (cells.isEmpty()) continue

            val rowTitle = cells.first()?.text()?.trim() ?: continue
            if (rowTitle.isBlank()) continue
            
            for (j in 1 until cells.size) {
                val region = header.getOrNull(j) ?: continue
                val date = cells.getOrNull(j)?.text()?.trim() ?: ""
                if (date.isNotBlank()) {
                    categories.add(VisaCategory(rowTitle, region, date))
                }
            }
        }
        return categories
    }
}