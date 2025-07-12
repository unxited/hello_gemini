package org.example

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.Period
import java.util.Locale

fun findTableByKeywords(doc: Element, keywords: List<String>): Element? {
    return doc.select("table").firstOrNull { table ->
        table.select("tr").firstOrNull()?.text()?.let { headerText ->
            keywords.all { keyword -> headerText.contains(keyword, ignoreCase = true) }
        } ?: false
    }
}

fun findTableByTitle(doc: Element, title: String): Element? {
    val normalizedTitle = title.uppercase()
    val headingB = doc.select("b").firstOrNull { b ->
        b.text().replace("\u00A0", " ").uppercase().contains(normalizedTitle)
    } ?: return null

    // Start searching from the heading's parent
    var anchor: Element? = headingB.parent()
    // Traverse up through ancestors
    while (anchor != null) {
        var sibling: Element? = anchor.nextElementSibling()
        while (sibling != null) {
            sibling.selectFirst("table")?.let { return it }
            sibling = sibling.nextElementSibling()
        }
        anchor = anchor.parent()
    }
    return null
}

fun fetchLatestBulletinUrl(): String? {
    val mainUrl = "https://travel.state.gov/content/travel/en/legal/visa-law0/visa-bulletin.html"
    return try {
        val doc = Jsoup.connect(mainUrl).get()
        doc.selectFirst("#recent_bulletins li.current a[href]")?.absUrl("href")
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// helper to parse dates from bulletin strings like 15NOV22/15NOV2022
fun parseBulletinDate(dateStr: String): LocalDate? {
    val cleaned = dateStr.trim().uppercase()
    if (cleaned == "C" || cleaned == "U") return null
    val patterns = listOf("ddMMMyy", "ddMMMyyyy")
    for (p in patterns) {
        try {
            return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(p, Locale.US))
        } catch (e: DateTimeParseException) { /*try next*/ }
    }
    return null
}

// helper to parse user input date (expects dd-MM-yyyy)
fun parseUserDate(dateStr: String): LocalDate? {
    val cleaned = dateStr.trim()
    val pattern = "dd-MM-yyyy"
    return try {
        LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(pattern, Locale.US))
    } catch (e: DateTimeParseException) {
        null
    }
}

fun main() = runBlocking {
    val parser = VisaBulletinParser()

    val defaultUrl = "https://travel.state.gov/content/travel/en/legal/visa-law0/visa-bulletin/2025/visa-bulletin-for-july-2025.html"
    val bulletinUrl = fetchLatestBulletinUrl() ?: defaultUrl
    println("Using bulletin URL: $bulletinUrl")

    val cacheDir = File("app/cache")
    if (!cacheDir.exists()) cacheDir.mkdirs()

    val slug = bulletinUrl.substringAfterLast('/')
        .removeSuffix(".html")
        .replace("[^A-Za-z0-9]+".toRegex(), "_")
    val cacheFile = File(cacheDir, "visa_bulletin_${slug}.json")

    println("Cache file path: ${cacheFile.absolutePath}")

    val allCategories: List<VisaCategory>

    if (cacheFile.exists()) {
        println("Loading visa bulletin data from cache...")
        allCategories = Json.decodeFromString<List<VisaCategory>>(cacheFile.readText())
    } else {
        println("Fetching HTML from: $bulletinUrl")
        val doc = Jsoup.connect(bulletinUrl).get()

        val finalActionTable = findTableByTitle(doc, "FINAL ACTION DATES FOR EMPLOYMENT-BASED PREFERENCE CASES")
        val datesForFilingTable = findTableByTitle(doc, "DATES FOR FILING OF EMPLOYMENT-BASED VISA APPLICATIONS")

        if (finalActionTable != null && datesForFilingTable != null) {
            val finalActionCategories = parser.parseEmploymentTableData(finalActionTable)
            val datesForFilingCategories = parser.parseEmploymentTableData(datesForFilingTable)

            println("Final Action Categories size: ${finalActionCategories.size}")
            println("Dates for Filing Categories size: ${datesForFilingCategories.size}")

            allCategories = finalActionCategories.map { it.copy(region = "Final Action - ${it.region}") } +
                          datesForFilingCategories.map { it.copy(region = "Dates for Filing - ${it.region}") }

            println("Total Categories size: ${allCategories.size}")

            cacheFile.writeText(Json.encodeToString(allCategories))
            println("Visa bulletin data cached successfully.")
        } else {
            println("Could not find the required tables in the HTML.")
            allCategories = emptyList()
        }
    }

    println("\n--- Final Action Dates for Employment-Based ---")
    allCategories.filter { it.region.startsWith("Final Action") }.forEach { category ->
        println("  Category: ${category.category}, Region: ${category.region.removePrefix("Final Action - ")}, Date: ${category.date}")
    }

    println("\n--- Dates for Filing of Employment-Based ---")
    allCategories.filter { it.region.startsWith("Dates for Filing") }.forEach { category ->
        println("  Category: ${category.category}, Region: ${category.region.removePrefix("Dates for Filing - ")}, Date: ${category.date}")
    }

    // ================= User Interaction =====================
    if (allCategories.isEmpty()) return@runBlocking

    println("\n===== Check Your Priority Date =====")
    print("Enter your Priority Date in dd-MM-yyyy format (e.g., 26-10-2023): ")
    val userDateStr = readlnOrNull()?.trim()
    if (userDateStr.isNullOrEmpty()) {
        println("No date entered. Exiting.")
        return@runBlocking
    }

    val userDate = parseUserDate(userDateStr)
    if (userDate == null) {
        println("Could not parse the date '$userDateStr'. Please use the exact format dd-MM-yyyy.")
        return@runBlocking
    }

    println("--> Interpreted your priority date as: $userDate")

    print("Enter Employment Category (e.g., 1st, 2nd, 3rd, Other Workers, 4th, 5th): ")
    val userCat = readlnOrNull()?.trim()?.uppercase() ?: return@runBlocking

    print("Enter Region/Country (e.g., INDIA, CHINA- MAINLAND BORN, or ALL): ")
    val userRegionInputRaw = readlnOrNull()?.trim()?.uppercase() ?: return@runBlocking

    val userRegionInput = if (userRegionInputRaw == "ALL") {
        "ALL CHARGEABILITY AREAS EXCEPT THOSE LISTED"
    } else {
        userRegionInputRaw
    }

    val finalActionData = allCategories.filter { it.region.startsWith("Final Action") }
    val match = finalActionData.firstOrNull { c ->
        c.category.uppercase() == userCat && c.region.removePrefix("Final Action - ").uppercase() == userRegionInput
    }

    if (match == null) {
        println("No matching category/region found in bulletin.")
        return@runBlocking
    }

    val bulletinDateStr = match.date.uppercase()

    if (bulletinDateStr == "C") {
        println("Your category is CURRENT. You can file immediately.")
        return@runBlocking
    }
    if (bulletinDateStr == "U") {
        println("Visa numbers are UNAVAILABLE for this category/region.")
        return@runBlocking
    }

    val bulletinDate = parseBulletinDate(bulletinDateStr)

    if (bulletinDate == null) {
        println("Could not parse bulletin date: ${match.date}")
        return@runBlocking
    }

    if (!userDate.isAfter(bulletinDate)) {
        println("Good news! Your priority date ${userDateStr} is CURRENT (<= ${match.date}).")
    } else {
        val diff = Period.between(bulletinDate, userDate)
        val months = diff.years * 12 + diff.months + if (diff.days > 0) 1 else 0
        println("Your priority date is not yet current. Estimated wait: ~${months} months (priority ${userDateStr} > bulletin ${match.date}).")
    }
}