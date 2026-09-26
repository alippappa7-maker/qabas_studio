package com.qabas.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.qabas.app.ui.theme.*

/**
 * مدير الصلاحيات الشامل — يطلب جميع الصلاحيات تلقائياً عند فتح التطبيق مع توضيح فاخر.
 */
@Composable
fun AppStartupPermissionHandler() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("qabas_permissions_prefs", Context.MODE_PRIVATE) }
    var hasRequestedInitially by remember { mutableStateOf(prefs.getBoolean("initial_permissions_requested", false)) }
    var showPermissionExplanation by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        val list = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.add(Manifest.permission.RECORD_AUDIO)
        list.add(Manifest.permission.CAMERA)
        list.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        prefs.edit().putBoolean("initial_permissions_requested", true).apply()
        hasRequestedInitially = true
    }

    fun checkAndRequest() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        if (!hasRequestedInitially) {
            // أول تشغيل للتطبيق: طلب الصلاحيات فوراً
            checkAndRequest()
        } else {
            // التحقق إذا كانت الصلاحيات الحيوية مفقودة
            val anyMissing = requiredPermissions.any {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (anyMissing && !prefs.getBoolean("explanation_dismissed", false)) {
                showPermissionExplanation = true
            }
        }
    }

    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = {
                showPermissionExplanation = false
                prefs.edit().putBoolean("explanation_dismissed", true).apply()
            },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "صلاحيات تطبيق قبس",
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "لضمان أفضل أداء واستقرار للميزات التالية، يرجى منح الصلاحيات المطلوبة:",
                        color = TextSecondary,
                        fontFamily = CairoFont,
                        fontSize = 12.sp
                    )

                    PermissionItemRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "الإشعارات وتشغيل الخلفية",
                        desc = "لتشغيل التلاوات في الخلفية واستلام التنبيهات الدينية والأذان"
                    )

                    PermissionItemRow(
                        icon = Icons.Default.Mic,
                        title = "الميكروفون والتسجيل الصوتي",
                        desc = "لتسجيل الصوت، التعليق الصوتي، وتحليل التجويد القرآني"
                    )

                    PermissionItemRow(
                        icon = Icons.Default.CameraAlt,
                        title = "الكاميرا ومخرجات الفيديو",
                        desc = "لالتقاط المشاهد المصورة واستوديو قبس الفني"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionExplanation = false
                        checkAndRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("منح الصلاحيات الآن", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionExplanation = false
                        prefs.edit().putBoolean("explanation_dismissed", true).apply()
                    }
                ) {
                    Text("لاحقاً", color = TextSecondary, fontFamily = CairoFont, fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF2A344A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = TextPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(desc, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 10.sp)
            }
        }
    }
}
