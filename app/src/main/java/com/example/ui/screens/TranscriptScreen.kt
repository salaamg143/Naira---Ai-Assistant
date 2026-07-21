package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.NairaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptScreen(
    sessionId: Long,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { NairaDatabase.getDatabase(context) }

    val messages by db.chatDao().getMessagesForSession(sessionId).collectAsState(initial = emptyList())
    var sessionTitle by remember { mutableStateOf("Conversation Transcript") }

    LaunchedEffect(sessionId) {
        withContext(Dispatchers.IO) {
            val session = db.chatDao().getSessionById(sessionId)
            if (session != null) {
                sessionTitle = session.title
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = sessionTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF112D4E)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF112D4E)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val session = db.chatDao().getSessionById(sessionId)
                                if (session != null) {
                                    db.chatDao().deleteSession(session)
                                    withContext(Dispatchers.Main) {
                                        navController.navigateUp()
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Conversation",
                            tint = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F7F7)
                )
            )
        },
        containerColor = Color(0xFFF9F7F7)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No conversation messages found.",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    items(messages) { msg ->
                        val isUser = msg.isUser
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 0.dp,
                                                bottomEnd = if (isUser) 0.dp else 16.dp
                                            )
                                        )
                                        .background(
                                            if (isUser) Color(0xFFDBE2EF) // Calm light gray blue
                                            else Color(0xFFE8E4F6) // Sweet pastel lavender
                                        )
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 14.sp,
                                        color = if (isUser) Color(0xFF112D4E) else Color(0xFF4A4E69)
                                    )
                                }
                                
                                Text(
                                    text = if (isUser) "You" else "Naira",
                                    fontSize = 10.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
