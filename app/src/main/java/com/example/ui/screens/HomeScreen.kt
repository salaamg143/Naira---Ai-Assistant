package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.data.ChatSession
import com.example.data.NairaDatabase
import com.example.ui.components.VoiceOrb
import com.example.voice.AssistantState
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    assistantManager: VoiceAssistantManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { NairaDatabase.getDatabase(context) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    // Database states
    val sessions by db.chatDao().getAllSessions().collectAsState(initial = emptyList())
    
    var currentSessionId by remember { mutableStateOf<Long?>(null) }
    
    // Create first session if none exists
    LaunchedEffect(sessions) {
        if (sessions.isEmpty()) {
            withContext(Dispatchers.IO) {
                val newSessionId = db.chatDao().insertSession(
                    ChatSession(title = "Chat Session 1")
                )
                currentSessionId = newSessionId
            }
        } else if (currentSessionId == null) {
            currentSessionId = sessions.first().sessionId
        }
    }

    // Pass active session to voice assistant manager
    LaunchedEffect(currentSessionId) {
        currentSessionId?.let { id ->
            assistantManager.setActiveSession(id)
        }
    }

    // User's own messages in current session
    val messagesFlow = remember(currentSessionId) {
        if (currentSessionId != null) {
            db.chatDao().getUserMessagesForSession(currentSessionId!!)
        } else {
            null
        }
    }
    val userMessages by messagesFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    // Audio recording permission state
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    val assistantState by assistantManager.state.collectAsState()
    val subtitles by assistantManager.liveSubtitles.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = Color(0xFFF9F7F7) // Soft calming background
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "NAIRA AI Companion",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3F72AF),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Text(
                    text = "Your caring personal space",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFDBE2EF))

                // New Chat Session Button
                Button(
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            withContext(Dispatchers.IO) {
                                val sdf = SimpleDateFormat("h:mm a, d MMM", Locale.getDefault())
                                val title = "Chat @ ${sdf.format(Date())}"
                                val newId = db.chatDao().insertSession(ChatSession(title = title))
                                withContext(Dispatchers.Main) {
                                    currentSessionId = newId
                                    assistantManager.setActiveSession(newId)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("new_chat_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3F72AF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Start Fresh Chat")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Fresh Chat")
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Past Conversations",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                // List of past sessions
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    items(sessions) { session ->
                        val isSelected = session.sessionId == currentSessionId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFDBE2EF) else Color.Transparent)
                                .clickable {
                                    scope.launch {
                                        currentSessionId = session.sessionId
                                        drawerState.close()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = "Session",
                                    tint = if (isSelected) Color(0xFF112D4E) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = session.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF112D4E) else Color.DarkGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Delete Session / View Transcript
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Transcript screen button
                                IconButton(
                                    onClick = {
                                        navController.navigate("transcript/${session.sessionId}")
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = "View Transcript",
                                        tint = Color(0xFF3F72AF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                
                                // Delete button
                                IconButton(
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            db.chatDao().deleteSession(session)
                                            if (currentSessionId == session.sessionId) {
                                                withContext(Dispatchers.Main) {
                                                    currentSessionId = null
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Session",
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Divider(color = Color(0xFFDBE2EF))
                Text(
                    text = "Made with ❤️ by Salaam",
                    fontSize = 11.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NAIRA",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF112D4E),
                                letterSpacing = 1.5.sp
                            )
                            val currentDate = remember {
                                val sdf = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                                sdf.format(Date())
                            }
                            Text(
                                text = currentDate,
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF112D4E))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { navController.navigate("settings") },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color(0xFF112D4E))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFFF9F7F7)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFFF9F7F7),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = true,
                        onClick = { /* Already Home */ },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF112D4E),
                            selectedTextColor = Color(0xFF112D4E),
                            indicatorColor = Color(0xFFDBE2EF)
                        )
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate("updates") },
                        icon = { Icon(Icons.Default.Campaign, contentDescription = "Updates") },
                        label = { Text("Updates") },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            },
            containerColor = Color(0xFFF9F7F7)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFF9F7F7), Color(0xFFF4F6FA))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // Floating animated Voice Orb
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        VoiceOrb(state = assistantState)
                    }

                    // Dynamic Subtitle Text (Visualizes speech)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = subtitles.ifEmpty { "Tap mic and say hello to NAIRA" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (assistantState == AssistantState.IDLE) Color.Gray else Color(0xFF112D4E),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("subtitle_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // User spoken list bubbles (Current Live Session - User's messages ONLY)
                    val listState = rememberLazyListState()
                    LaunchedEffect(userMessages.size) {
                        if (userMessages.isNotEmpty()) {
                            listState.animateScrollToItem(userMessages.size - 1)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        if (userMessages.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Your spoken inputs appear here",
                                    fontSize = 13.sp,
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(userMessages) { msg ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(
                                                    RoundedCornerShape(
                                                        topStart = 16.dp,
                                                        topEnd = 16.dp,
                                                        bottomStart = 16.dp,
                                                        bottomEnd = 0.dp
                                                    )
                                                )
                                                .background(Color(0xFFDBE2EF))
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = msg.text,
                                                fontSize = 14.sp,
                                                color = Color(0xFF112D4E)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Main Pastel Floating Mic Button
                    Box(
                        modifier = Modifier
                            .padding(bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (hasAudioPermission) {
                                    if (assistantState == AssistantState.LISTENING) {
                                        assistantManager.stopListening()
                                    } else {
                                        assistantManager.startListening()
                                    }
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            shape = CircleShape,
                            containerColor = when (assistantState) {
                                AssistantState.LISTENING -> Color(0xFFA6E3E9) // Pastel turquoise active
                                AssistantState.SPEAKING -> Color(0xFFFFD3B6) // Warm pastel orange active
                                AssistantState.THINKING -> Color(0xFFFFB6B9) // Thinking pastel pink
                                AssistantState.IDLE -> Color(0xFFDBE2EF) // Calm slate blue
                            },
                            contentColor = Color(0xFF112D4E),
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("mic_fab")
                        ) {
                            Icon(
                                imageVector = when (assistantState) {
                                    AssistantState.LISTENING -> Icons.Default.Stop
                                    AssistantState.SPEAKING -> Icons.Default.VolumeUp
                                    AssistantState.THINKING -> Icons.Default.HourglassEmpty
                                    AssistantState.IDLE -> Icons.Default.Mic
                                },
                                contentDescription = "Voice Assistant Control",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
