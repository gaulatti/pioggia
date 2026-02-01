package com.gaulatti.celesti

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.gaulatti.celesti.device.DeviceManager
import com.gaulatti.celesti.service.TVControlService
import com.gaulatti.celesti.ui.theme.CelestiTheme
import com.gaulatti.celesti.ui.theme.EncodeSans
import com.gaulatti.celesti.ui.theme.LibreFranklin
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request

enum class RegistrationState {
    PENDING,
    STANDBY
}

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get device ID
        val deviceManager = DeviceManager(this)
        val deviceId = deviceManager.getDeviceId()
        
        setContent {
            CelestiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    RegistrationFlow(
                        deviceId = deviceId,
                        onRegistered = {
                            // Start the TV control service when registered
                            val serviceIntent = Intent(this@MainActivity, TVControlService::class.java)
                            startForegroundService(serviceIntent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RegistrationFlow(
    deviceId: String,
    onRegistered: () -> Unit
) {
    var state by remember { mutableStateOf(RegistrationState.PENDING) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with logo
        Header()
        
        // Main content
        when (state) {
            RegistrationState.PENDING -> {
                PendingScreen(
                    deviceId = deviceId,
                    onRegistered = {
                        state = RegistrationState.STANDBY
                        onRegistered()
                    }
                )
            }
            RegistrationState.STANDBY -> {
                StandbyScreen()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun Header() {
    var currentTime by remember { mutableStateOf(getCurrentTimeString()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTimeString()
            delay(1000) // Update every second
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Logo and brand
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Gaulatti Logo",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Vertical divider with sunset gradient
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0f)
                            )
                        )
                    )
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // App name
            Text(
                text = "celesti",
                fontSize = 24.sp,
                fontFamily = EncodeSans,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Right side: Current time
        Text(
            text = currentTime,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

fun getCurrentTimeString(): String {
    val formatter = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    return formatter.format(java.util.Date())
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PendingScreen(
    deviceId: String,
    onRegistered: () -> Unit
) {
    val client = remember { OkHttpClient() }
    
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val request = Request.Builder()
                    .url("https://celesti.gaulatti.com/devices/whoami")
                    .header("X-Device-ID", deviceId)
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                when (response.code) {
                    204 -> {
                        response.close()
                        onRegistered()
                        return@LaunchedEffect
                    }
                    404 -> {
                        response.close()
                        // Continue polling
                    }
                    else -> {
                        response.close()
                        // Continue polling on other errors
                    }
                }
            } catch (e: Exception) {
                // Continue polling on network errors
            }
            
            delay(3000) // Poll every 3 seconds
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            // Cleanup
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left column: Code and instructions
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = deviceId,
                fontSize = 56.sp,
                fontFamily = EncodeSans,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Enter this code in the app to register this device",
                fontSize = 24.sp,
                fontFamily = LibreFranklin,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 32.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(64.dp))
        
        // Right column: QR code
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val qrBitmap = remember(deviceId) {
                generateQRCode("https://celesti.gaulatti.com/register/$deviceId", 400)
            }
            
            qrBitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = "Registration QR Code",
                    modifier = Modifier.size(240.dp)
                )
                Text(
                    text = "Scan to register",
                    fontSize = 20.sp,
                    fontFamily = LibreFranklin,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}

fun generateQRCode(content: String, size: Int): ImageBitmap? {
    return try {
        val writer = com.google.zxing.qrcode.QRCodeWriter()
        val bitMatrix = writer.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
            }
        }
        
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun StandbyScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            fontFamily = EncodeSans,
            fontWeight = FontWeight.SemiBold,
            text = "Standby",
            fontSize = 48.sp,
            modifier = Modifier.graphicsLayer(alpha = alpha)
        )
    }
}
