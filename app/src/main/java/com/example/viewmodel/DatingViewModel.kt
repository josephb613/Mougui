package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.DatingProfile
import com.example.model.SampleProfiles
import com.example.service.FirebaseHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

enum class FilterType {
    TODAY, FOR_YOU
}

data class Message(
    val senderId: String, // "me" or profile.id
    val text: String,
    val timestamp: String
)

data class DatingUiState(
    val profiles: List<DatingProfile> = SampleProfiles,
    val currentIndex: Int = 0,
    val likedProfileIds: Set<String> = emptySet(),
    val passedProfileIds: Set<String> = emptySet(),
    val matchShowcaseProfile: DatingProfile? = null,
    val currentFilter: FilterType = FilterType.TODAY,
    val selectedProfileForDetail: DatingProfile? = null,
    val chatHistory: Map<String, List<Message>> = mapOf(
        "1" to listOf(
            Message("1", "Salut ! J'adore ta photo de profil, tu fais du dessin aussi ?", "16:45"),
            Message("me", "Salut Mari ! Merci beaucoup ! Oui, j'adore peindre le week-end.", "16:48"),
            Message("1", "Trop cool ! Tu peins à l'huile ou à l'acrylique ?", "16:49")
        ),
        "2" to listOf(
            Message("2", "Coucou ! Tu es libre pour un café ce week-end ?", "Hier")
        )
    ),
    val unreadNotificationsCount: Int = 3,
    val currentBottomTab: String = "discover", // discover, matches, chat, profile
    val isSubscribed: Boolean = false,
    val userName: String = "Joseph",
    val userPhone: String = "+243 812 000 000",
    val userAge: Int = 27,
    val userImageUrl: String = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=300",
    val isFirebaseConnecting: Boolean = false,
    val isTestingFirebase: Boolean = false,
    val firebaseTestSuccess: Boolean? = null,
    val firebaseTestMessage: String = "",
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    val isOnboarded: Boolean = false
)

class DatingViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(DatingUiState())
    val uiState: StateFlow<DatingUiState> = _uiState.asStateFlow()

    private val prefs = getApplication<android.app.Application>()
        .getSharedPreferences("dating_app_prefs", android.content.Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val setAdapter = moshi.adapter<Set<String>>(
        Types.newParameterizedType(Set::class.java, String::class.java)
    )

    private val mapAdapter = moshi.adapter<Map<String, List<Message>>>(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Types.newParameterizedType(List::class.java, Message::class.java)
        )
    )

    private var profilesListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var userListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var activeChatListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        loadStateFromPrefs()
        loadFirebaseData()
    }

    private fun saveStateToPrefs() {
        val editor = prefs.edit()
        val state = _uiState.value
        editor.putBoolean("is_onboarded", state.isOnboarded)
        editor.putString("user_name", state.userName)
        editor.putString("user_phone", state.userPhone)
        editor.putInt("user_age", state.userAge)
        editor.putBoolean("is_subscribed", state.isSubscribed)
        editor.putInt("current_index", state.currentIndex)
        editor.putString("user_image_url", state.userImageUrl)
        
        try {
            editor.putString("liked_profile_ids", setAdapter.toJson(state.likedProfileIds))
            editor.putString("passed_profile_ids", setAdapter.toJson(state.passedProfileIds))
            editor.putString("chat_history", mapAdapter.toJson(state.chatHistory))
        } catch (e: Exception) {
            Log.e("DatingViewModel", "Error serializing state to prefs", e)
        }
        editor.apply()
    }

    private fun loadStateFromPrefs() {
        val onboarded = prefs.getBoolean("is_onboarded", false)
        val name = prefs.getString("user_name", "Joseph") ?: "Joseph"
        val phone = prefs.getString("user_phone", "+243 812 000 000") ?: "+243 812 000 000"
        val age = prefs.getInt("user_age", 27)
        val subscribed = prefs.getBoolean("is_subscribed", false)
        val currentIndex = prefs.getInt("current_index", 0)
        val imageUrl = prefs.getString(
            "user_image_url",
            "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=300"
        ) ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=300"

        var likedProfileIds = emptySet<String>()
        var passedProfileIds = emptySet<String>()
        var chatHistory: Map<String, List<Message>>? = null

        try {
            val likedJson = prefs.getString("liked_profile_ids", null)
            if (likedJson != null) {
                likedProfileIds = setAdapter.fromJson(likedJson) ?: emptySet()
            }
            val passedJson = prefs.getString("passed_profile_ids", null)
            if (passedJson != null) {
                passedProfileIds = setAdapter.fromJson(passedJson) ?: emptySet()
            }
            val chatJson = prefs.getString("chat_history", null)
            if (chatJson != null) {
                chatHistory = mapAdapter.fromJson(chatJson)
            }
        } catch (e: Exception) {
            Log.e("DatingViewModel", "Error deserializing state from prefs", e)
        }

        _uiState.update {
            it.copy(
                isOnboarded = onboarded,
                userName = name,
                userPhone = phone,
                userAge = age,
                isSubscribed = subscribed,
                currentIndex = currentIndex,
                likedProfileIds = likedProfileIds,
                passedProfileIds = passedProfileIds,
                chatHistory = chatHistory ?: it.chatHistory,
                userImageUrl = imageUrl
            )
        }

        // Start listening to user updates if they are onboarded & registered
        if (onboarded && phone.isNotBlank() && phone != "+243 812 000 000") {
            startUserListener(phone)
        }
    }

    fun setOnboarded(onboarded: Boolean) {
        _uiState.update { it.copy(isOnboarded = onboarded) }
        saveStateToPrefs()
    }

    fun syncAllDataToFirebase() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isTestingFirebase = true, 
                    firebaseTestSuccess = null, 
                    firebaseTestMessage = "Début de la sauvegarde manuelle vers Firestore..."
                ) 
            }
            
            if (!FirebaseHelper.isInitialized) {
                _uiState.update { 
                    it.copy(
                        isTestingFirebase = false, 
                        firebaseTestSuccess = false, 
                        firebaseTestMessage = "Impossible de sauvegarder : Firebase n'est pas initialisé d'après vos Secrets."
                    ) 
                }
                return@launch
            }
            
            try {
                val db = FirebaseHelper.getFirestore()
                if (db == null) {
                    _uiState.update { 
                        it.copy(
                            isTestingFirebase = false, 
                            firebaseTestSuccess = false, 
                            firebaseTestMessage = "Firestore est indisponible actuellement."
                        ) 
                    }
                    return@launch
                }
                
                _uiState.update { it.copy(firebaseTestMessage = "Sauvegarde en cours : Upload des 8 profils de test...") }
                
                // Write all sample profiles
                for (profile in SampleProfiles) {
                    db.collection("dating_profiles")
                        .document(profile.id)
                        .set(profile, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                }
                
                _uiState.update { it.copy(firebaseTestMessage = "Sauvegarde en cours : Enregistrement de votre profil utilisateur...") }
                
                // Write the user's personal profile as well
                val userId = FirebaseHelper.getAuth()?.currentUser?.uid ?: "user_default"
                val userMap = hashMapOf(
                    "uid" to userId,
                    "name" to _uiState.value.userName,
                    "phone" to _uiState.value.userPhone,
                    "age" to _uiState.value.userAge,
                    "isSubscribed" to _uiState.value.isSubscribed,
                    "createdAt" to System.currentTimeMillis()
                )
                db.collection("users_profiles").document(userId).set(userMap, com.google.firebase.firestore.SetOptions.merge()).await()
                
                // Reload list from Firestore so the app UI displays live Firestore data!
                val remoteProfiles = FirebaseHelper.fetchDatingProfiles()
                val updatedProfiles = if (!remoteProfiles.isNullOrEmpty()) remoteProfiles else SampleProfiles
                
                _uiState.update { 
                    it.copy(
                        profiles = updatedProfiles,
                        isTestingFirebase = false, 
                        firebaseTestSuccess = true, 
                        firebaseTestMessage = "Sauvegarde terminée ! 8 profils et votre compte utilisateur de test ont été synchronisés sur Firestore."
                    ) 
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Erreur réseau ou droits d'accès insuffisants (Rules)"
                _uiState.update { 
                    it.copy(
                        isTestingFirebase = false, 
                        firebaseTestSuccess = false, 
                        firebaseTestMessage = "Échec : $errorMsg. Vérifiez les règles d'écriture de votre base de données."
                    ) 
                }
            }
        }
    }

    fun testFirebaseConnection() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isTestingFirebase = true, 
                    firebaseTestSuccess = null, 
                    firebaseTestMessage = "Initialisation du test de diagnostic..."
                ) 
            }
            
            if (!FirebaseHelper.isInitialized) {
                _uiState.update { 
                    it.copy(
                        isTestingFirebase = false, 
                        firebaseTestSuccess = false, 
                        firebaseTestMessage = "Firebase n'est pas initialisé. Veuillez remplir vos clés privées dans le panel des Secrets."
                    ) 
                }
                return@launch
            }
            
            try {
                val db = FirebaseHelper.getFirestore()
                if (db == null) {
                    _uiState.update { 
                        it.copy(
                            isTestingFirebase = false, 
                            firebaseTestSuccess = false, 
                            firebaseTestMessage = "L'instance Firestore est indisponible."
                        ) 
                    }
                    return@launch
                }
                
                _uiState.update { it.copy(firebaseTestMessage = "[1/2] Écriture du document de test dans 'connection_tests'...") }
                
                val testId = "diag_" + System.currentTimeMillis()
                val testData = hashMapOf(
                    "testerName" to _uiState.value.userName,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "diagnostic_passed"
                )
                
                val docRef = db.collection("connection_tests").document(testId)
                docRef.set(testData).await()
                
                _uiState.update { it.copy(firebaseTestMessage = "[2/2] Lecture et comparaison des données écrites...") }
                
                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    val status = snapshot.getString("status")
                    if (status == "diagnostic_passed") {
                        // Clear testing/temporary file silently (no-blocking context)
                        try {
                            docRef.delete()
                        } catch (e: Exception) {
                            Log.e("DatingViewModel", "Warning: Failed to delete diag document", e)
                        }
                        
                        _uiState.update { 
                            it.copy(
                                isTestingFirebase = false, 
                                firebaseTestSuccess = true, 
                                firebaseTestMessage = "Succès ! Écriture et lecture validées sur Firestore."
                            ) 
                        }
                    } else {
                        _uiState.update { 
                            it.copy(
                                isTestingFirebase = false, 
                                firebaseTestSuccess = false, 
                                firebaseTestMessage = "Divergence de données : Statut inattendu lu."
                            ) 
                        }
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isTestingFirebase = false, 
                            firebaseTestSuccess = false, 
                            firebaseTestMessage = "Échec : Le document de test n'a pas pu être retrouvé sur Firestore."
                        ) 
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.localizedMessage ?: "Erreur réseau ou droits d'accès insuffisants (Rules)"
                _uiState.update { 
                    it.copy(
                        isTestingFirebase = false, 
                        firebaseTestSuccess = false, 
                        firebaseTestMessage = "Échec : $errorMsg"
                    ) 
                }
            }
        }
    }

    fun loadFirebaseData() {
        viewModelScope.launch {
            if (FirebaseHelper.isInitialized) {
                // Seed and start snaphot listeners in the background without setting a full-screen blocking UI flag.
                // This ensures offline-first instant app startup using local samples first, while Firestore data loads and updates in real-time.
                launch {
                    try {
                        FirebaseHelper.seedProfilesIfEmpty(SampleProfiles)
                    } catch (e: Exception) {
                        Log.e("DatingViewModel", "Error seeding profiles on startup: ${e.message}", e)
                    }
                }
                
                try {
                    startProfilesListener()
                } catch (e: Exception) {
                    Log.e("DatingViewModel", "Error starting profiles real-time listener: ${e.message}", e)
                }
            }
        }
    }

    fun registerUser(name: String, phone: String, password: String, onResult: (Boolean) -> Unit) {
        val finalName = name.ifBlank { "Joseph" }
        val finalPhone = phone.ifBlank { "+243 812 000 000" }

        _uiState.update {
            it.copy(
                userName = finalName,
                userPhone = finalPhone,
                isFirebaseConnecting = true
            )
        }
        
        saveStateToPrefs()

        viewModelScope.launch {
            if (FirebaseHelper.isInitialized) {
                try {
                    val success = FirebaseHelper.registerUser(
                        name = finalName,
                        phone = finalPhone,
                        password = password,
                        age = _uiState.value.userAge
                    )
                    if (success) {
                        startUserListener(finalPhone)
                        _uiState.update { it.copy(isFirebaseConnecting = false) }
                        saveStateToPrefs()
                        onResult(true)
                    } else {
                        _uiState.update { 
                            it.copy(
                                isFirebaseConnecting = false,
                                loginError = "Échec de l'inscription. Veuillez réessayer."
                            ) 
                        }
                        onResult(false)
                    }
                } catch (e: Exception) {
                    Log.e("DatingViewModel", "Foire de création de compte Firestore", e)
                    val errorMsg = e.localizedMessage ?: "Erreur inconnue"
                    val userFriendlyError = when {
                        errorMsg.contains("disabled", ignoreCase = true) || errorMsg.contains("configuration", ignoreCase = true) -> 
                            "Le fournisseur de connexion 'Email/Mot de passe' est désactivé dans votre console Firebase Authentication (onglet Sign-in method)."
                        errorMsg.contains("already in use", ignoreCase = true) || errorMsg.contains("ALREADY_EXISTS", ignoreCase = true) -> 
                            "Ce numéro de téléphone est déjà associé à un autre compte."
                        errorMsg.contains("weak", ignoreCase = true) || errorMsg.contains("WEAK_PASSWORD", ignoreCase = true) -> 
                            "Le mot de passe choisi est trop faible (6 caractères minimum)."
                        errorMsg.contains("permission-denied", ignoreCase = true) || errorMsg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                            "Erreur Firestore : Règles de sécurité insuffisantes pour enregistrer votre profil."
                        errorMsg.contains("network", ignoreCase = true) -> 
                            "Erreur réseau. Veuillez vérifier votre connexion ou vos clés Firebase."
                        else -> "Échec : $errorMsg"
                    }
                    _uiState.update { 
                        it.copy(
                            isFirebaseConnecting = false,
                            loginError = userFriendlyError
                        ) 
                    }
                    onResult(false)
                }
            } else {
                _uiState.update { it.copy(isFirebaseConnecting = false) }
                saveStateToPrefs()
                onResult(true)
            }
        }
    }

    fun loginUser(phone: String, password: String, onResult: (Boolean) -> Unit) {
        val finalPhone = phone.ifBlank { "+243 812 000 000" }
        _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
        
        viewModelScope.launch {
            if (FirebaseHelper.isInitialized) {
                try {
                    val authSuccess = FirebaseHelper.loginUser(finalPhone, password)
                    if (authSuccess) {
                        val db = FirebaseHelper.getFirestore()
                        if (db != null) {
                            Log.d("DatingViewModel", "Authentifié par Firebase. Recherche de son profil: $finalPhone")
                            val query = db.collection("users_profiles")
                                .whereEqualTo("phone", finalPhone)
                                .limit(1)
                                .get()
                                .await()
                                
                            if (!query.isEmpty) {
                                val userDoc = query.documents[0]
                                val name = userDoc.getString("name") ?: "Joseph"
                                val age = userDoc.get("age")?.let { 
                                    when(it) {
                                        is Number -> it.toInt()
                                        else -> 27
                                    }
                                } ?: 27
                                val isSubscribed = userDoc.getBoolean("isSubscribed") ?: false
                                val imageUrl = userDoc.getString("imageUrl") ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=300"
                                
                                _uiState.update {
                                    it.copy(
                                        userName = name,
                                        userPhone = finalPhone,
                                        userAge = age,
                                        isSubscribed = isSubscribed,
                                        userImageUrl = imageUrl,
                                        isLoggingIn = false,
                                        loginError = null
                                    )
                                }
                                
                                startUserListener(finalPhone)
                                saveStateToPrefs()

                                Log.d("DatingViewModel", "Connexion Firestore réussie pour: $name")
                                onResult(true)
                            } else {
                                // User authenticated but profile doc is missing, let's create a stub profile doc so they can still access
                                val auth = FirebaseHelper.getAuth()
                                val uid = auth?.currentUser?.uid ?: "stub_uid"
                                val userMap = hashMapOf(
                                    "uid" to uid,
                                    "name" to "Joseph",
                                    "phone" to finalPhone,
                                    "age" to 27,
                                    "isSubscribed" to false,
                                    "createdAt" to System.currentTimeMillis()
                                )
                                db.collection("users_profiles").document(uid).set(userMap).await()

                                _uiState.update {
                                    it.copy(
                                        userName = "Joseph",
                                        userPhone = finalPhone,
                                        userAge = 27,
                                        isSubscribed = false,
                                        isLoggingIn = false,
                                        loginError = null
                                    )
                                }
                                startUserListener(finalPhone)
                                saveStateToPrefs()
                                onResult(true)
                            }
                        } else {
                            // Firestore not available but authenticated
                            _uiState.update {
                                it.copy(
                                    userName = "Joseph",
                                    userPhone = finalPhone,
                                    isLoggingIn = false,
                                    loginError = null
                                )
                            }
                            saveStateToPrefs()
                            onResult(true)
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoggingIn = false,
                                loginError = "Numéro de téléphone ou mot de passe incorrect. Veuillez réessayer."
                            )
                        }
                        onResult(false)
                    }
                } catch (e: Exception) {
                    val errorMsg = e.localizedMessage ?: "Inconnu"
                    val userFriendlyError = when {
                        errorMsg.contains("disabled", ignoreCase = true) || errorMsg.contains("configuration", ignoreCase = true) -> 
                            "Le fournisseur 'Email/Mot de passe' est désactivé dans votre console Firebase."
                        errorMsg.contains("invalid-credential", ignoreCase = true) || errorMsg.contains("wrong-password", ignoreCase = true) || errorMsg.contains("user-not-found", ignoreCase = true) -> 
                            "Identifiants incorrects ou utilisateur inexistant."
                        errorMsg.contains("network", ignoreCase = true) -> 
                            "Erreur réseau ou clé Firebase non configurée dans vos Secrets."
                        else -> "Erreur de connexion : $errorMsg"
                    }
                    _uiState.update {
                        it.copy(
                            isLoggingIn = false,
                            loginError = userFriendlyError
                        )
                    }
                    onResult(false)
                }
            } else {
                // If firebase is offline or not configured, simulated success so the app works instantly
                _uiState.update {
                    it.copy(
                        userName = "Joseph (Demo)",
                        userPhone = finalPhone,
                        isLoggingIn = false,
                        loginError = null
                    )
                }
                
                saveStateToPrefs()
                onResult(true)
            }
        }
    }

    fun clearLoginError() {
        _uiState.update { it.copy(loginError = null) }
    }

    fun setSubscriptionActive(active: Boolean) {
        _uiState.update { it.copy(isSubscribed = active) }
        saveStateToPrefs()
    }

    fun setFilter(filter: FilterType) {
        _uiState.update { it.copy(currentFilter = filter) }
    }

    fun selectTab(tab: String) {
        _uiState.update { it.copy(currentBottomTab = tab) }
    }

    fun selectProfileDetail(profile: DatingProfile?) {
        _uiState.update { it.copy(selectedProfileForDetail = profile) }
    }

    fun likeCurrentProfile() {
        val current = currentProfile() ?: return
        _uiState.update { state ->
            val newLikes = state.likedProfileIds + current.id
            val nextIndex = state.currentIndex + 1
            state.copy(
                likedProfileIds = newLikes,
                currentIndex = nextIndex,
                // Match triggers! Show the match banner modal
                matchShowcaseProfile = current,
                // Automatically add a welcome starter message
                chatHistory = state.chatHistory + (current.id to listOf(
                    Message(current.id, "Coucou ! On a matché ! ✨ Raconte-moi, qu'est-ce qui t'a plu dans mon profil ?", "17:52")
                ))
            )
        }
        saveStateToPrefs()

        if (FirebaseHelper.isInitialized) {
            val db = FirebaseHelper.getFirestore()
            if (db != null) {
                viewModelScope.launch {
                    try {
                        db.collection("chat_rooms")
                            .document(current.id)
                            .collection("messages")
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                if (querySnapshot.isEmpty) {
                                    val starterMsg = hashMapOf(
                                        "senderId" to current.id,
                                        "text" to "Coucou ! On a matché ! ✨ Raconte-moi, qu'est-ce qui t'a plu dans mon profil ?",
                                        "timestamp" to "17:52",
                                        "createdAt" to System.currentTimeMillis()
                                    )
                                    db.collection("chat_rooms")
                                        .document(current.id)
                                        .collection("messages")
                                        .add(starterMsg)
                                }
                            }
                    } catch (e: Exception) {
                        Log.e("DatingViewModel", "Error setting up initial Firestore message", e)
                    }
                }
            }
        }
    }

    fun passCurrentProfile() {
        val current = currentProfile() ?: return
        _uiState.update { state ->
            val newPassed = state.passedProfileIds + current.id
            val nextIndex = state.currentIndex + 1
            state.copy(
                passedProfileIds = newPassed,
                currentIndex = nextIndex
            )
        }
        saveStateToPrefs()
    }

    fun dismissMatchModal() {
        _uiState.update { it.copy(matchShowcaseProfile = null) }
    }

    fun currentProfile(): DatingProfile? {
        val state = _uiState.value
        return if (state.currentIndex < state.profiles.size) {
            state.profiles[state.currentIndex]
        } else {
            null
        }
    }

    fun resetSwipes() {
        _uiState.update { state ->
            state.copy(
                currentIndex = 0,
                likedProfileIds = emptySet(),
                passedProfileIds = emptySet()
            )
        }
        saveStateToPrefs()
    }

    fun sendMessage(profileId: String, text: String) {
        if (text.isBlank()) return
        val timestamp = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        _uiState.update { state ->
            val messages = state.chatHistory[profileId]?.toMutableList() ?: mutableListOf()
            messages.add(Message("me", text, timestamp))
            val updatedHistory = state.chatHistory + (profileId to messages)
            state.copy(chatHistory = updatedHistory)
        }
        saveStateToPrefs()

        if (FirebaseHelper.isInitialized) {
            val db = FirebaseHelper.getFirestore()
            if (db != null) {
                val currentUid = FirebaseHelper.getAuth()?.currentUser?.uid ?: "me"
                val chatRoomId = if (currentUid < profileId) "${currentUid}_${profileId}" else "${profileId}_${currentUid}"
                
                val msgMap = hashMapOf(
                    "chatRoomId" to chatRoomId,
                    "senderId" to currentUid,
                    "receiverId" to profileId,
                    "text" to text,
                    "timestamp" to timestamp,
                    "createdAt" to System.currentTimeMillis()
                )
                viewModelScope.launch {
                    try {
                        // 1. Save to the main real-time cross-user collection
                        db.collection("chats")
                            .add(msgMap)
                            .await()

                        // 2. Backward compatibility
                        db.collection("chat_rooms")
                            .document(profileId)
                            .collection("messages")
                            .add(msgMap)
                            .await()
                    } catch (e: Exception) {
                        Log.e("DatingViewModel", "Error sending message to Firestore: ${e.message}", e)
                    }
                }
            }
        }
    }

    fun setActiveChatRoom(profileId: String?) {
        activeChatListenerRegistration?.remove()
        activeChatListenerRegistration = null

        if (profileId == null) return

        if (!FirebaseHelper.isInitialized) return
        val db = FirebaseHelper.getFirestore() ?: return

        val currentUid = FirebaseHelper.getAuth()?.currentUser?.uid ?: "me"
        val chatRoomId = if (currentUid < profileId) "${currentUid}_${profileId}" else "${profileId}_${currentUid}"

        activeChatListenerRegistration = db.collection("chats")
            .whereEqualTo("chatRoomId", chatRoomId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("DatingViewModel", "Error listening to chats: ${error.message}. Retrying via subcollection...")
                    db.collection("chat_rooms")
                        .document(profileId)
                        .collection("messages")
                        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                        .get()
                        .addOnSuccessListener { subSnap ->
                            val msgs = mutableListOf<Message>()
                            for (doc in subSnap.documents) {
                                val sId = doc.getString("senderId") ?: ""
                                val txt = doc.getString("text") ?: ""
                                val tStamp = doc.getString("timestamp") ?: ""
                                if (sId.isNotBlank() && txt.isNotBlank()) {
                                    val mapped = if (sId == currentUid || sId == "me") "me" else sId
                                    msgs.add(Message(mapped, txt, tStamp))
                                }
                            }
                            if (msgs.isNotEmpty()) {
                                _uiState.update { state ->
                                    val updatedHistory = state.chatHistory + (profileId to msgs)
                                    state.copy(chatHistory = updatedHistory)
                                }
                                saveStateToPrefs()
                            }
                        }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = mutableListOf<Message>()
                    for (doc in snapshot.documents) {
                        val senderId = doc.getString("senderId") ?: ""
                        val text = doc.getString("text") ?: ""
                        val timestamp = doc.getString("timestamp") ?: ""
                        if (senderId.isNotBlank() && text.isNotBlank()) {
                            val mapped = if (senderId == currentUid || senderId == "me") "me" else senderId
                            messages.add(Message(mapped, text, timestamp))
                        }
                    }
                    if (messages.isNotEmpty()) {
                        _uiState.update { state ->
                            val updatedHistory = state.chatHistory + (profileId to messages)
                            state.copy(chatHistory = updatedHistory)
                        }
                        saveStateToPrefs()
                    }
                }
            }
    }

    fun startProfilesListener() {
        if (!FirebaseHelper.isInitialized) return
        val db = FirebaseHelper.getFirestore() ?: return
        
        profilesListenerRegistration?.remove()
        
        profilesListenerRegistration = db.collection("dating_profiles")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DatingViewModel", "Error listening to profiles: ${error.message}", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = mutableListOf<DatingProfile>()
                    for (doc in snapshot.documents) {
                        val id = doc.id
                        val name = doc.getString("name") ?: ""
                        val age = doc.get("age")?.let { 
                            when(it) {
                                is Number -> it.toInt()
                                else -> 24
                            }
                        } ?: 24
                        val distanceKm = doc.get("distanceKm")?.let { 
                            when(it) {
                                is Number -> it.toInt()
                                else -> 10
                            }
                        } ?: 10
                        val matchPercentage = doc.get("matchPercentage")?.let { 
                            when(it) {
                                is Number -> it.toInt()
                                else -> 90
                            }
                        } ?: 90
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
                    _uiState.update { it.copy(profiles = list) }
                }
            }
    }

    fun startUserListener(phone: String) {
        if (!FirebaseHelper.isInitialized) return
        val db = FirebaseHelper.getFirestore() ?: return
        
        userListenerRegistration?.remove()
        
        userListenerRegistration = db.collection("users_profiles")
            .whereEqualTo("phone", phone)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DatingViewModel", "Error listening to user profile: ${error.message}", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    val name = doc.getString("name") ?: "Joseph"
                    val age = doc.get("age")?.let { 
                        when(it) {
                            is Number -> it.toInt()
                            else -> 27
                        }
                    } ?: 27
                    val isSubscribed = doc.getBoolean("isSubscribed") ?: false
                    val imageUrl = doc.getString("imageUrl") ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=300"
                    
                    _uiState.update {
                        it.copy(
                            userName = name,
                            userAge = age,
                            isSubscribed = isSubscribed,
                            userImageUrl = imageUrl
                        )
                    }
                    
                    prefs.edit()
                        .putString("user_name", name)
                        .putInt("user_age", age)
                        .putBoolean("is_subscribed", isSubscribed)
                        .putString("user_image_url", imageUrl)
                        .apply()
                }
            }
    }

    fun uploadProfileImage(uri: android.net.Uri, onResult: (Boolean, String?) -> Unit) {
        if (!FirebaseHelper.isInitialized) {
            onResult(false, "Firebase n'est pas initialisé d'après vos Secrets.")
            return
        }

        viewModelScope.launch {
            try {
                val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                val currentUid = FirebaseHelper.getAuth()?.currentUser?.uid ?: "user_default"
                
                // Reference to /profile_images/{uid}.jpg
                val imageRef = storage.reference.child("profile_images/$currentUid.jpg")
                
                // Upload task
                imageRef.putFile(uri).await()
                
                // Get download url
                val downloadUrl = imageRef.downloadUrl.await().toString()
                
                // Update local state and prefs
                _uiState.update { it.copy(userImageUrl = downloadUrl) }
                saveStateToPrefs()
                
                // Save/Sync downloadUrl to user_profiles inside Firestore
                val db = FirebaseHelper.getFirestore()
                if (db != null) {
                    val userMap = hashMapOf<String, Any>(
                        "imageUrl" to downloadUrl
                    )
                    db.collection("users_profiles")
                        .document(currentUid)
                        .set(userMap, com.google.firebase.firestore.SetOptions.merge())
                        .await()

                    // Also set/update online profile so they're visible immediately
                    val profileMap = hashMapOf<String, Any>(
                        "name" to _uiState.value.userName,
                        "age" to _uiState.value.userAge,
                        "imageUrl" to downloadUrl,
                        "phoneNumber" to _uiState.value.userPhone
                    )
                    db.collection("dating_profiles")
                        .document(currentUid)
                        .set(profileMap, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                }
                
                onResult(true, downloadUrl)
            } catch (e: Exception) {
                Log.e("DatingViewModel", "Failed uploading image to Firebase Storage: ${e.message}", e)
                onResult(false, e.localizedMessage)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        profilesListenerRegistration?.remove()
        userListenerRegistration?.remove()
        activeChatListenerRegistration?.remove()
    }
}
