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
import tech.titans.runwars.models.FriendTerritoryColors
import tech.titans.runwars.models.User
import tech.titans.runwars.views.FriendsViewModel

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

    // Load preferences when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadVisibleTerritoriesPreference(context)
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