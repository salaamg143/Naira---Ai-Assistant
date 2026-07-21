package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppLockScreen(
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("naira_prefs", Context.MODE_PRIVATE) }
    val savedPin = remember { sharedPrefs.getString("pin_code", "") ?: "" }

    var inputPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun handleNumKey(num: String) {
        isError = false
        if (inputPin.length < 4) {
            val updated = inputPin + num
            inputPin = updated
            if (updated.length == 4) {
                if (updated == savedPin) {
                    onUnlockSuccess()
                } else {
                    isError = true
                    inputPin = ""
                }
            }
        }
    }

    fun handleBackspace() {
        if (inputPin.isNotEmpty()) {
            inputPin = inputPin.dropLast(1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F7F7))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFDBE2EF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                tint = Color(0xFF112D4E),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Naira is Locked",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF112D4E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isError) "Incorrect PIN. Try again." else "Please enter your 4-digit security PIN to continue.",
            fontSize = 13.sp,
            color = if (isError) Color.Red else Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PIN Indicators (Dots)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val filled = i < inputPin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) Color(0xFF112D4E) else Color(0xFFDBE2EF)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Keypad Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "back")
            )

            for (row in keys) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (key in row) {
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.size(64.dp))
                        } else if (key == "back") {
                            IconButton(
                                onClick = { handleBackspace() },
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDBE2EF).copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = Color(0xFF112D4E)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDBE2EF).copy(alpha = 0.5f))
                                    .clickable { handleNumKey(key) }
                                    .testTag("keypad_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF112D4E)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
