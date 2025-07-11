package org.example

import kotlinx.serialization.Serializable

@Serializable
data class VisaCategory(
    val category: String,
    val region: String,
    val date: String
)
