package com.example.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private var isDefaultDialer by mutableStateOf(false)
    private var hasPermissions by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.all { it.value }
        if (hasPermissions) {
            Toast.makeText(this, "Permissões concedidas!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Algumas permissões foram negadas", Toast.LENGTH_LONG).show()
        }
    }

    private val dialerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        checkDefaultDialer()
        if (isDefaultDialer) {
            Toast.makeText(this, "✓ Configurado como app padrão!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Você precisa selecionar Call Blocker na lista", Toast.LENGTH_LONG).show()
        }
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

    override fun onResume() {
        super.onResume()
        checkDefaultDialer()
        checkPermissions()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun checkDefaultDialer() {
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        isDefaultDialer = packageName == telecomManager.defaultDialerPackage
    }

    private fun requestDefaultDialer() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(RoleManager::class.java)
                
                if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                    Toast.makeText(
                        this, 
                        "Seu dispositivo não suporta apps de discagem personalizados", 
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                dialerLauncher.launch(intent)
            } else {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                dialerLauncher.launch(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
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
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🛡️ Call Blocker",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        StatusCard("Permissões", hasPermissions)
        Spacer(modifier = Modifier.height(16.dp))
        StatusCard("App Padrão", isDefaultDialer)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!hasPermissions) {
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Conceder Permissões")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Aceite todas as permissões solicitadas",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        
        if (hasPermissions && !isDefaultDialer) {
            Button(
                onClick = onRequestDefaultDialer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Definir como App Padrão")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Selecione 'Call Blocker' na próxima tela",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
        
        if (hasPermissions && isDefaultDialer) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "✓ Proteção Ativa",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Apenas contatos salvos na agenda podem ligar para você",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(title: String, isActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isActive) "✓" else "○",
                style = MaterialTheme.typography.headlineMedium,
                color = if (isActive) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
