package tech.titans.runwars.views

import android.content.Context
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.titans.runwars.models.User
import tech.titans.runwars.repo.UserRepo

class FriendsViewModel: ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _friendsList = MutableStateFlow<List<User>>(emptyList())
    val friendsList = _friendsList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // Track which friends' territories should be shown on the map
    private val _friendsWithVisibleTerritories = MutableStateFlow<Set<String>>(emptySet())
    val friendsWithVisibleTerritories = _friendsWithVisibleTerritories.asStateFlow()

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadFriends()
    }

    /**
     * Load the visible territories preference from SharedPreferences
     */
    fun loadVisibleTerritoriesPreference(context: Context) {
        val prefs = context.getSharedPreferences("RunWarsPrefs", Context.MODE_PRIVATE)
        val savedSet = prefs.getStringSet("visibleFriendTerritories", emptySet()) ?: emptySet()
        _friendsWithVisibleTerritories.value = savedSet
        println("📂 FriendsViewModel: Loaded ${savedSet.size} visible friend territories from prefs")
    }

    /**
     * Save the visible territories preference to SharedPreferences
     */
    private fun saveVisibleTerritoriesPreference(context: Context) {
        val prefs = context.getSharedPreferences("RunWarsPrefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("visibleFriendTerritories", _friendsWithVisibleTerritories.value).apply()
        println("💾 FriendsViewModel: Saved ${_friendsWithVisibleTerritories.value.size} visible friend territories to prefs")
    }

    /**
     * Toggle territory visibility for a specific friend
     */
    fun toggleFriendTerritoryVisibility(friendId: String, context: Context) {
        val currentSet = _friendsWithVisibleTerritories.value.toMutableSet()
        if (friendId in currentSet) {
            currentSet.remove(friendId)
            println("👁️‍🗨️ FriendsViewModel: Hiding territories for friend $friendId")
        } else {
            currentSet.add(friendId)
            println("👁️ FriendsViewModel: Showing territories for friend $friendId")
        }
        _friendsWithVisibleTerritories.value = currentSet
        saveVisibleTerritoriesPreference(context)
    }

    /**
     * Check if a friend's territory is visible
     */
    fun isFriendTerritoryVisible(friendId: String): Boolean {
        return friendId in _friendsWithVisibleTerritories.value
    }

    fun onSearchQueryChanged(query: String) {
        _searchText.value = query
        if (query.length >= 2) {
            UserRepo.searchUsers(query) { users ->
                // Filter out the current user from results so you can't add yourself
                _searchResults.value = users.filter { it.userId != currentUserId }
            }
        } else {
            _searchResults.value = emptyList()
        }
    }

    fun loadFriends() {
        currentUserId?.let { uid ->
            UserRepo.getUser(uid) { user, _ ->
                if (user != null) {
                    _friendsList.value = user.friendsList
                }
            }
        }
    }

    fun addFriend(userToAdd: User) {
        currentUserId?.let { uid ->
            _isLoading.value = true
            UserRepo.addFriend(uid, userToAdd) { success, _ ->
                _isLoading.value = false
                if (success) {
                    // Refresh the friends list and clear search
                    loadFriends()
                    _searchText.value = ""
                    _searchResults.value = emptyList()
                }
            }
        }
    }

    fun removeFriend(friendId: String) {
        currentUserId?.let { uid ->
            _isLoading.value = true
            UserRepo.removeFriend(uid, friendId) { success, _ ->
                _isLoading.value = false
                if (success) {
                    loadFriends() // Refresh the list to update UI
                }
            }
        }
    }
}