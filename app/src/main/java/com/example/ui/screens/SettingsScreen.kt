package com.example.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.AppNickname
import com.example.data.NairaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { NairaDatabase.getDatabase(context) }
    
    val sharedPrefs = remember { context.getSharedPreferences("naira_prefs", Context.MODE_PRIVATE) }
    
    // SharedPreferences State
    var apiKey by remember { mutableStateOf(sharedPrefs.getString("api_key", "") ?: "") }
    var userName by remember { mutableStateOf(sharedPrefs.getString("user_name", "User") ?: "User") }
    var personality by remember { mutableStateOf(sharedPrefs.getString("personality", "girlfriend") ?: "girlfriend") }
    var useAppLock by remember { mutableStateOf(sharedPrefs.getBoolean("use_app_lock", false)) }
    var pinCode by remember { mutableStateOf(sharedPrefs.getString("pin_code", "") ?: "") }

    var isApiKeyVisible by remember { mutableStateOf(false) }

    // App Nicknames State
    var installedApps by remember { mutableStateOf<List<AppNickname>>(emptyList()) }
    val dbNicknames by db.chatDao().getAllNicknames().collectAsState(initial = emptyList())
    var currentNicknameTab by remember { mutableStateOf(false) } // Open/close apps drawer

    // Load installed apps from device
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val list = mutableListOf<AppNickname>()
            for (app in apps) {
                val isLaunchable = pm.getLaunchIntentForPackage(app.packageName) != null
                if (isLaunchable) {
                    val label = app.loadLabel(pm).toString()
                    val saved = db.chatDao().getNicknameForPackage(app.packageName)
                    list.add(AppNickname(
                        packageName = app.packageName,
                        systemLabel = label,
                        customNickname = saved?.customNickname
                    ))
                }
            }
            installedApps = list.sortedBy { it.systemLabel }
        }
    }

    // Sync saved DB nicknames with loaded list
    val finalAppsList = remember(installedApps, dbNicknames) {
        installedApps.map { app ->
            val match = dbNicknames.find { it.packageName == app.packageName }
            app.copy(customNickname = match?.customNickname)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF9F7F7)
                )
            )
        },
        containerColor = Color(0xFFF9F7F7)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // Section 1: API Configuration
            item {
                Text(
                    text = "API Configuration",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3F72AF)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Gemini API Key",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = {
                                apiKey = it
                                sharedPrefs.edit().putString("api_key", it).apply()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input"),
                            placeholder = { Text("Enter Gemini API Key") },
                            singleLine = true,
                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle API Key Visibility"
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3F72AF),
                                unfocusedBorderColor = Color(0xFFDBE2EF)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Get your Gemini key securely from Google AI Studio. Used locally to request completions.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }

            // Section 2: Companion Persona
            item {
                Text(
                    text = "Personal Profile & Voice",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3F72AF)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Your Name",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = userName,
                            onValueChange = {
                                userName = it
                                sharedPrefs.edit().putString("user_name", it).apply()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3F72AF),
                                unfocusedBorderColor = Color(0xFFDBE2EF)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Naira Voice Personality",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Personality choices
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "girlfriend" to "Partner",
                                "professional" to "Professional",
                                "friendly" to "Friendly"
                            ).forEach { (id, label) ->
                                val selected = personality == id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) Color(0xFFDBE2EF) else Color(0xFFF9F7F7))
                                        .clickable {
                                            personality = id
                                            sharedPrefs.edit().putString("personality", id).apply()
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color(0xFF112D4E) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: App Security
            item {
                Text(
                    text = "Security Configuration",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3F72AF)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Use App Lock PIN",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.DarkGray
                                )
                                Text(
                                    text = "Requires 4-digit PIN on startup",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Switch(
                                checked = useAppLock,
                                onCheckedChange = {
                                    useAppLock = it
                                    sharedPrefs.edit().putBoolean("use_app_lock", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF112D4E),
                                    checkedTrackColor = Color(0xFFDBE2EF)
                                )
                            )
                        }
                        
                        AnimatedVisibility(visible = useAppLock) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Text(
                                    text = "Set 4-Digit PIN",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = pinCode,
                                    onValueChange = {
                                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                            pinCode = it
                                            sharedPrefs.edit().putString("pin_code", it).apply()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF3F72AF),
                                        unfocusedBorderColor = Color(0xFFDBE2EF)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Custom App Nicknames
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { currentNicknameTab = !currentNicknameTab }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Nicknames Mappings",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3F72AF)
                    )
                    Icon(
                        imageVector = if (currentNicknameTab) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand Mappings",
                        tint = Color(0xFF3F72AF)
                    )
                }
                
                AnimatedVisibility(visible = currentNicknameTab) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        if (finalAppsList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF3F72AF))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                items(finalAppsList) { app ->
                                    var textVal by remember(app.packageName) { mutableStateOf(app.customNickname ?: "") }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1.2f)) {
                                            Text(
                                                text = app.systemLabel,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF112D4E)
                                            )
                                            Text(
                                                text = app.packageName,
                                                fontSize = 10.sp,
                                                color = Color.LightGray,
                                                maxLines = 1
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        OutlinedTextField(
                                            value = textVal,
                                            onValueChange = { newVal ->
                                                textVal = newVal
                                                scope.launch(Dispatchers.IO) {
                                                    db.chatDao().insertNickname(
                                                        AppNickname(
                                                            packageName = app.packageName,
                                                            systemLabel = app.systemLabel,
                                                            customNickname = newVal.ifEmpty { null }
                                                        )
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp),
                                            placeholder = { Text("Nickname", fontSize = 11.sp) },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF3F72AF),
                                                unfocusedBorderColor = Color(0xFFDBE2EF)
                                            ),
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                        )
                                    }
                                    Divider(color = Color(0xFFF9F7F7), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
