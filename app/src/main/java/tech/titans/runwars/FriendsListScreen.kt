package tech.titans.runwars

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import tech.titans.runwars.models.FriendTerritoryColors
import tech.titans.runwars.models.User
import tech.titans.runwars.views.FriendsViewModel
import tech.titans.runwars.repo.UserRepo

@Composable
fun FriendsListScreen(
    navController: androidx.navigation.NavController,
    viewModel: FriendsViewModel = viewModel()
) {
    val context = LocalContext.current
    val searchText by viewModel.searchText.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val friendsList by viewModel.friendsList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val visibleTerritories by viewModel.friendsWithVisibleTerritories.collectAsState()

    // State for territory viewer modal
    var showTerritoryViewerModal by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf("") }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Load preferences and user info when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadVisibleTerritoriesPreference(context)
        // Get current user's name
        UserRepo.getUser(currentUserId) { user, _ ->
            if (user != null) {
                currentUserName = user.userName
            }
        }
    }

    // Territory Viewer Modal
    if (showTerritoryViewerModal) {
        TerritoryViewerModal(
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            friendsList = friendsList,
            onSelectPerson = { selectedUserId, selectedUserName ->
                showTerritoryViewerModal = false
                // Save selection to prefs so HomeScreen can pick it up
                val prefs = context.getSharedPreferences("RunWarsPrefs", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("pendingKingdomView", selectedUserId)
                    .putString("pendingKingdomViewName", selectedUserName)
                    .apply()
                // Navigate back to home
                navController.popBackStack()
            },
            onDismiss = { showTerritoryViewerModal = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C1E3C)) // Matches your app theme
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 8.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() }, // Go back to Home
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "Friends",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        // Search Bar
        OutlinedTextField(
            value = searchText,
            onValueChange = { viewModel.onSearchQueryChanged(it) },
            label = { Text("Find friends", color = Color.LightGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF8E5DFF),
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF8E5DFF)
            )
        )

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                color = Color(0xFF8E5DFF)
            )
        }

        // List Content
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // 1. Show Search Results (if user is searching)
            if (searchText.isNotEmpty()) {
                item {
                    Text("Search Results", color = Color(0xFF8E5DFF), fontWeight = FontWeight.Bold)
                }
                if (searchResults.isEmpty() && searchText.length > 2) {
                    item { Text("No users found.", color = Color.Gray) }
                }
                items(searchResults) { user ->
                    FriendItem(
                        user = user,
                        isAlreadyFriend = false,
                        showTerritoryToggle = false,
                        isTerritoryVisible = false,
                        colorIndex = 0,
                        onAdd = { viewModel.addFriend(user) },
                        onRemove = {},
                        onToggleTerritory = {},
                        onClick = {}
                    )
                }
            }

            // 2. Show Existing Friends (if not searching)
            else {
                item {
                    Text("Your Squad", color = Color(0xFF8E5DFF), fontWeight = FontWeight.Bold)
                }
                if (friendsList.isEmpty()) {
                    item {
                        Text("You haven't added any friends yet.", color = Color.Gray, modifier = Modifier.padding(top=8.dp))
                    }
                }
                items(friendsList) { friend ->
                    val colorIndex = friendsList.indexOf(friend)
                    FriendItem(
                        user = friend,
                        isAlreadyFriend = true,
                        showTerritoryToggle = true,
                        isTerritoryVisible = friend.userId in visibleTerritories,
                        colorIndex = colorIndex,
                        onAdd = {},
                        onRemove = { viewModel.removeFriend(friend.userId) },
                        onToggleTerritory = { viewModel.toggleFriendTerritoryVisibility(friend.userId, context) },
                        onClick = { navController.navigate("profile/${friend.userId}") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // View Territories button
        Button(
            onClick = { showTerritoryViewerModal = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 48.dp), // Extra bottom padding for OneUI 7.0 transparent nav
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E5DFF))
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Territories", color = Color.White)
        }
    }
}

@Composable
fun FriendItem(
    user: User,
    isAlreadyFriend: Boolean,
    showTerritoryToggle: Boolean,
    isTerritoryVisible: Boolean,
    colorIndex: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onToggleTerritory: () -> Unit,
    onClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2C53)),
        modifier = Modifier.fillMaxWidth(),
        onClick = if (isAlreadyFriend) onClick else ({})
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Avatar Placeholder with color indicator for territory
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isAlreadyFriend && isTerritoryVisible)
                        Color(FriendTerritoryColors.getStrokeColor(colorIndex))
                    else
                        Color(0xFF8E5DFF),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))

                // Name Info
                Column {
                    Text(
                        text = user.userName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }

            // Action buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAlreadyFriend) {
                    // Territory visibility toggle
                    if (showTerritoryToggle) {
                        Switch(
                            checked = isTerritoryVisible,
                            onCheckedChange = { onToggleTerritory() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(FriendTerritoryColors.getStrokeColor(colorIndex)),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color(0xFF555555)
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    // Show Remove Button (Trash Icon)
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Friend",
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                } else {
                    IconButton(onClick = onAdd) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Friend",
                            tint = Color(0xFF8E5DFF)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modal for selecting a person to view their territories on the map
 */
@Composable
fun TerritoryViewerModal(
    currentUserId: String,
    currentUserName: String,
    friendsList: List<User>,
    onSelectPerson: (userId: String, userName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter friends based on search
    val filteredFriends = remember(searchQuery, friendsList) {
        if (searchQuery.isEmpty()) {
            friendsList
        } else {
            friendsList.filter { friend ->
                friend.userName.contains(searchQuery, ignoreCase = true) ||
                friend.firstName.contains(searchQuery, ignoreCase = true) ||
                friend.lastName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Check if "You" should be shown based on search
    val showSelf = searchQuery.isEmpty() ||
                   currentUserName.contains(searchQuery, ignoreCase = true) ||
                   "you".contains(searchQuery, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C1E3C),
        title = {
            Text(
                "View Territories",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search", color = Color.LightGray) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF8E5DFF),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF8E5DFF)
                    )
                )

                // Scrollable list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "You" item - always first
                    if (showSelf) {
                        item {
                            TerritoryListItem(
                                userName = currentUserName,
                                subtitle = "(You)",
                                color = Color(0xFF00AA00), // Green for own
                                onClick = { onSelectPerson(currentUserId, currentUserName) }
                            )
                        }
                    }

                    // Friends
                    items(filteredFriends) { friend ->
                        TerritoryListItem(
                            userName = friend.userName,
                            subtitle = "${friend.firstName} ${friend.lastName}",
                            color = Color(0xFF8E5DFF), // Purple for friends
                            onClick = { onSelectPerson(friend.userId, friend.userName) }
                        )
                    }

                    // Empty state
                    if (filteredFriends.isEmpty() && !showSelf) {
                        item {
                            Text(
                                "No results found",
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    if (friendsList.isEmpty() && showSelf) {
                        item {
                            Text(
                                "Add friends to view their territories!",
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF8E5DFF))
            }
        }
    )
}

@Composable
fun TerritoryListItem(
    userName: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF3D2C53)),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = color,
                modifier = Modifier.size(12.dp)
            ) {}

            Spacer(modifier = Modifier.width(12.dp))

            // Name info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }

            // Arrow indicator
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = "View Territories",
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

