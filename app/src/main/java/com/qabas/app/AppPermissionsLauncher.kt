package com.qabas.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.qabas.app.ui.theme.*

/**
 * طلب جميع الصلاحيات تلقائياً عند دخول التطبيق:
 * - الإشعارات (POST_NOTIFICATIONS) للتذكيرات القرآنية والأذكار
 * - الميكروفون (RECORD_AUDIO) لاستوديو الصوتيات والتلاوة
 * - الكاميرا (CAMERA) لاستوديو الفيديو والتصوير
 */
@Composable
fun AppPermissionsEntryLauncher(
    onPermissionsHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE) }
    var hasRequestedOnStartup by remember {
        mutableStateOf(prefs.getBoolean("has_prompted_all_permissions_v1", false))
    }
    var showExplanationDialog by remember { mutableStateOf(false) }

    val permissionsToRequest = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
        }.toTypedArray()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultsMap ->
        prefs.edit().putBoolean("has_prompted_all_permissions_v1", true).apply()
        hasRequestedOnStartup = true
        onPermissionsHandled()
    }

    LaunchedEffect(Unit) {
        if (!hasRequestedOnStartup) {
            val ungranted = permissionsToRequest.filter { perm ->
                ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
            }
            if (ungranted.isNotEmpty()) {
                launcher.launch(permissionsToRequest)
            } else {
                prefs.edit().putBoolean("has_prompted_all_permissions_v1", true).apply()
            }
        }
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            containerColor = CardSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("صلاحيات استوديو قبس", color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "للحصول على أفضل تجربة، يحتاج التطبيق إلى الصلاحيات التالية:",
                        color = TextSecondary,
                        fontFamily = CairoFont,
                        fontSize = 13.sp
                    )
                    PermissionExplItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "الإشعارات الذكية",
                        desc = "لتنبيهات الأذكار وأوقات الصلاة ومتابعة الرندرة في الخلفية"
                    )
                    PermissionExplItem(
                        icon = Icons.Default.Mic,
                        title = "تسجيل الصوتيات",
                        desc = "لتسجيل التلاوات والمقاطع في استوديو الصوتيات الاحترافي"
                    )
                    PermissionExplItem(
                        icon = Icons.Default.PhotoCamera,
                        title = "الكاميرا",
                        desc = "لتصوير المشاهد وإضافتها في استوديو الفيديو"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExplanationDialog = false
                        launcher.launch(permissionsToRequest)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DeepSlate),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("منح الصلاحيات", fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExplanationDialog = false }) {
                    Text("لاحقاً", color = TextSecondary, fontFamily = CairoFont)
                }
            }
        )
    }
}

@Composable
private fun PermissionExplItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1E2638))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(desc, color = TextSecondary, fontFamily = CairoFont, fontSize = 11.sp)
        }
    }
}
