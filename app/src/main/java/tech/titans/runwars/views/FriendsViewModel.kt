package tech.titans.runwars.views

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

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadFriends()
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