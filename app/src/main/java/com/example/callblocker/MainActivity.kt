package com.example.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var isDefaultDialer by mutableStateOf(false)
    private var hasPermissions by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.all { it.value }
        if (hasPermissions) requestDefaultDialer()
    }

    private val dialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkDefaultDialer()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        checkPermissions()
        checkDefaultDialer()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CallBlockerScreen(
                        isDefaultDialer = isDefaultDialer,
                        hasPermissions = hasPermissions,
                        onRequestPermissions = { requestPermissions() },
                        onRequestDefaultDialer = { requestDefaultDialer() }
                    )
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
        hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS
        ))
    }

    private fun checkDefaultDialer() {
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        isDefaultDialer = packageName == telecomManager.defaultDialerPackage
    }

    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
            dialerLauncher.launch(intent)
        }
    }
}

@Composable
fun CallBlockerScreen(
    isDefaultDialer: Boolean,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onRequestDefaultDialer: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🛡️ Call Blocker", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        StatusCard("Permissões", hasPermissions)
        Spacer(modifier = Modifier.height(16.dp))
        StatusCard("App Padrão", isDefaultDialer)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!hasPermissions) {
            Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
                Text("Conceder Permissões")
            }
        }
        
        if (hasPermissions && !isDefaultDialer) {
            Button(onClick = onRequestDefaultDialer, modifier = Modifier.fillMaxWidth()) {
                Text("Definir como Padrão")
            }
        }
        
        if (hasPermissions && isDefaultDialer) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "✓ Proteção Ativa\n\nApenas contatos salvos podem ligar",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun StatusCard(title: String, isActive: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(if (isActive) "✓" else "○", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title)
        }
    }
}
