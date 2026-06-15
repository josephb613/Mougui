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
    val isFirebaseConnecting: Boolean = false,
    val isTestingFirebase: Boolean = false,
    val firebaseTestSuccess: Boolean? = null,
    val firebaseTestMessage: String = ""
)

class DatingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DatingUiState())
    val uiState: StateFlow<DatingUiState> = _uiState.asStateFlow()

    init {
        loadFirebaseData()
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
                _uiState.update { it.copy(isFirebaseConnecting = true) }
                // Ensure initial seed profiles are uploaded if backend is clean
                FirebaseHelper.seedProfilesIfEmpty(SampleProfiles)
                // Retrieve list from Firestore
                val remoteProfiles = FirebaseHelper.fetchDatingProfiles()
                if (!remoteProfiles.isNullOrEmpty()) {
                    _uiState.update {
                        it.copy(
                            profiles = remoteProfiles,
                            isFirebaseConnecting = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isFirebaseConnecting = false) }
                }
            }
        }
    }

    fun registerUser(name: String, phone: String) {
        val finalName = name.ifBlank { "Joseph" }
        val finalPhone = phone.ifBlank { "+243 812 000 000" }

        _uiState.update {
            it.copy(
                userName = finalName,
                userPhone = finalPhone
            )
        }

        viewModelScope.launch {
            if (FirebaseHelper.isInitialized) {
                FirebaseHelper.registerUser(
                    name = finalName,
                    phone = finalPhone,
                    age = _uiState.value.userAge
                )
            }
        }
    }

    fun setSubscriptionActive(active: Boolean) {
        _uiState.update { it.copy(isSubscribed = active) }
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
    }

    fun sendMessage(profileId: String, text: String) {
        if (text.isBlank()) return
        _uiState.update { state ->
            val messages = state.chatHistory[profileId]?.toMutableList() ?: mutableListOf()
            messages.add(Message("me", text, "A l'instant"))
            val updatedHistory = state.chatHistory + (profileId to messages)
            state.copy(chatHistory = updatedHistory)
        }
    }
}
