package com.example.model

data class DatingProfile(
    val id: String,
    val name: String,
    val age: Int,
    val distanceKm: Int,
    val matchPercentage: Int,
    val imageUrl: String,
    val additionalImages: List<String>,
    val bio: String,
    val aboutMe: String,
    val locationName: String = "Paris",
    val hobbies: List<String> = listOf("Voyage", "Art", "Musique", "Café"),
    val phoneNumber: String = "+243810000000"
)
