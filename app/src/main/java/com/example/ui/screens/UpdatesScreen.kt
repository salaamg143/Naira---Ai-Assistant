package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "ANNOUNCEMENTS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF112D4E),
                        letterSpacing = 1.2.sp
                    )
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
                    selected = false,
                    onClick = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Already Updates */ },
                    icon = { Icon(Icons.Default.Campaign, contentDescription = "Updates") },
                    label = { Text("Updates") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF112D4E),
                        selectedTextColor = Color(0xFF112D4E),
                        indicatorColor = Color(0xFFDBE2EF)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                // Header card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDBE2EF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Updates",
                                tint = Color(0xFF112D4E),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Naira Release Center",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF112D4E)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Stay up to date with core updates, enhancements, and personal assistant features.",
                                fontSize = 12.sp,
                                color = Color(0xFF3F72AF),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Update 1
                item {
                    UpdateCard(
                        tag = "NEW RELEASE",
                        version = "v2.0.0",
                        date = "July 2026",
                        title = "Voice Assistant Redesign",
                        description = "Naira is now fully upgraded with high-performance speech synthesis and speech recognition pipelines. Chat naturally, choose different personalities, and customize launcher app mappings directly from Settings.",
                        colorScheme = Color(0xFFE8E4F6) // Lavender
                    )
                }

                // Update 2
                item {
                    UpdateCard(
                        tag = "ENHANCEMENT",
                        version = "v1.8.0",
                        date = "May 2026",
                        title = "Introducing Hinglish & Custom Personalities",
                        description = "Caring partner mode 'girlfriend' mode is now fully supported. Interact with Naira using native Hinglish dialects with emotionally expressive voice, or switch to a highly structured Professional voice context.",
                        colorScheme = Color(0xFFE3FDFD) // Mint
                    )
                }

                // Update 3
                item {
                    UpdateCard(
                        tag = "COMING SOON",
                        version = "v2.1.0",
                        date = "Upcoming",
                        title = "Screen Vision & On-Device Actions",
                        description = "Automate device workflows by asking Naira to click, scroll, and launch applications directly on your behalf. Dynamic Accessibility service mappings are coming in the next core OTA release.",
                        colorScheme = Color(0xFFFFE3E3) // Peach/Pink
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateCard(
    tag: String,
    version: String,
    date: String,
    title: String,
    description: String,
    colorScheme: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorScheme)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
                Text(
                    text = "$version • $date",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF112D4E)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )
        }
    }
}
