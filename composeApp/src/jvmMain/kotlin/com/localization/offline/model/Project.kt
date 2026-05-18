package com.localization.offline.model

import kotlinx.serialization.Serializable

@Serializable
data class Project(
    val id: String,
    val name: String
)