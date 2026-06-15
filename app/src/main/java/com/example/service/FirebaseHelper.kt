package com.example.service

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.DatingProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object FirebaseHelper {
    private const val TAG = "FirebaseHelper"
    var isInitialized = false
        private set

    fun initialize(context: Context) {
        if (isInitialized) return

        val apiKey = BuildConfig.FIREBASE_API_KEY
        val appId = BuildConfig.FIREBASE_APPLICATION_ID
        val projectId = BuildConfig.FIREBASE_PROJECT_ID

        // Check if config has been modified by the user in the AI Studio environment
        if (apiKey.isBlank() || apiKey == "YOUR_FIREBASE_API_KEY_HERE" ||
            appId.isBlank() || appId == "YOUR_FIREBASE_APP_ID_HERE" ||
            projectId.isBlank() || projectId == "YOUR_FIREBASE_PROJECT_ID_HERE") {
            Log.w(TAG, "Firebase credentials are not configured yet. Running in offline/demo mode.")
            return
        }

        try {
            val options = FirebaseOptions.Builder()
                .setApiKey(apiKey)
                .setApplicationId(appId)
                .setProjectId(projectId)
                .build()

            // Initialize app programmatically
            FirebaseApp.initializeApp(context, options)
            isInitialized = true
            Log.d(TAG, "Firebase initialized successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
        }
    }

    fun getAuth(): FirebaseAuth? {
        return if (isInitialized) FirebaseAuth.getInstance() else null
    }

    fun getFirestore(): FirebaseFirestore? {
        return if (isInitialized) FirebaseFirestore.getInstance() else null
    }

    // Sign in anonymously and store the user registration data safely on Firestore
    suspend fun registerUser(name: String, phone: String, age: Int): Boolean {
        val auth = getAuth() ?: return false
        val db = getFirestore() ?: return false

        return try {
            val result = auth.signInAnonymously().await()
            val userId = result.user?.uid ?: return false

            val userMap = hashMapOf(
                "uid" to userId,
                "name" to name,
                "phone" to phone,
                "age" to age,
                "isSubscribed" to false,
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("users_profiles")
                .document(userId)
                .set(userMap, SetOptions.merge())
                .await()

            Log.d(TAG, "User registered in Firebase Firestore with UID: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed registering user: ${e.message}", e)
            false
        }
    }

    // Fetch dating profiles from Cloud Firestore
    suspend fun fetchDatingProfiles(): List<DatingProfile>? {
        val db = getFirestore() ?: return null
        return try {
            val snapshot = db.collection("dating_profiles").get().await()
            if (snapshot.isEmpty) {
                return emptyList()
            }
            val list = mutableListOf<DatingProfile>()
            for (doc in snapshot.documents) {
                val id = doc.id
                val name = doc.getString("name") ?: ""
                val age = doc.getLong("age")?.toInt() ?: 24
                val distanceKm = doc.getLong("distanceKm")?.toInt() ?: 10
                val matchPercentage = doc.getLong("matchPercentage")?.toInt() ?: 90
                val imageUrl = doc.getString("imageUrl") ?: ""
                @Suppress("UNCHECKED_CAST")
                val additionalImages = doc.get("additionalImages") as? List<String> ?: emptyList()
                val bio = doc.getString("bio") ?: ""
                val aboutMe = doc.getString("aboutMe") ?: ""
                val locationName = doc.getString("locationName") ?: "Paris"
                @Suppress("UNCHECKED_CAST")
                val hobbies = doc.get("hobbies") as? List<String> ?: emptyList()
                val phoneNumber = doc.getString("phoneNumber") ?: "+243"

                list.add(
                    DatingProfile(
                        id = id,
                        name = name,
                        age = age,
                        distanceKm = distanceKm,
                        matchPercentage = matchPercentage,
                        imageUrl = imageUrl,
                        additionalImages = additionalImages,
                        bio = bio,
                        aboutMe = aboutMe,
                        locationName = locationName,
                        hobbies = hobbies,
                        phoneNumber = phoneNumber
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed fetching dating profiles: ${e.message}", e)
            null
        }
    }

    // Populate Firestore with initial profiles if empty
    suspend fun seedProfilesIfEmpty(profiles: List<DatingProfile>) {
        val db = getFirestore() ?: return
        try {
            val snapshot = db.collection("dating_profiles").limit(1).get().await()
            if (snapshot.isEmpty) {
                Log.d(TAG, "Firestore 'dating_profiles' collection is empty. Seeding sample profiles...")
                for (profile in profiles) {
                    db.collection("dating_profiles")
                        .document(profile.id)
                        .set(profile, SetOptions.merge())
                        .await()
                }
                Log.d(TAG, "Sample profiles seeded successfully!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding Firestore profiles: ${e.message}", e)
        }
    }
}
