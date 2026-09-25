package com.qabas.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qabas.app.ui.theme.CairoFont
import com.qabas.app.ui.theme.CardSurface
import com.qabas.app.ui.theme.DeepSlate
import com.qabas.app.ui.theme.GoldPrimary
import com.qabas.app.ui.theme.GoldSecondary
import com.qabas.app.ui.theme.NotoSansFont
import com.qabas.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * شاشة بداية وتحميل بيانات الاستوديو (Splash & Data Loading).
 * تعرض هوية قبس ستوديو الجديدة كلياً مع الخلفية السينمائية وشريط التقدّم.
 */
@Composable
fun DataLoadingScreen(
    onFinished: () -> Unit,
    onLoadProjects: suspend () -> Unit = {}
) {
    val appContext = LocalContext.current.applicationContext
    var finished by remember { mutableStateOf(false) }
    fun finish() {
        if (finished) return
        finished = true
        onFinished()
    }

    data class LoadStep(val title: String, val icon: ImageVector)

    val steps = remember {
        listOf(
            LoadStep("تهيئة الخدمات المحلية", Icons.Default.Settings),
            LoadStep("مزامنة السحابة وFirebase", Icons.Default.CloudDownload),
            LoadStep("تحميل المشاريع والمفضلات", Icons.Default.Folder)
        )
    }

    var currentStep by remember { mutableIntStateOf(0) }
    var overallProgress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("جاري التحضير...") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    LaunchedEffect(Unit) {
        currentStep = 0
        statusText = steps[0].title
        overallProgress = 0.08f
        try {
            AppServices.init(appContext)
            AudioPlayerManager.init(appContext)
            CloudServices.tryInitFromSavedConfig(appContext)
            StyleBrain.init(appContext)
            StyleManager.init(appContext)
        } catch (_: Exception) { }
        delay(450)
        overallProgress = 0.28f

        currentStep = 1
        statusText = steps[1].title
        delay(350)
        overallProgress = 0.48f
        try {
            CloudServices.Database.getLeaderboard()
        } catch (_: Exception) { }
        delay(400)
        overallProgress = 0.62f

        currentStep = 2
        statusText = steps[2].title
        try {
            onLoadProjects()
        } catch (_: Exception) { }
        delay(400)
        overallProgress = 0.88f
        delay(250)
        overallProgress = 1f
        statusText = "تم التحميل بنجاح"
        delay(350)
        finish()
    }

    LaunchedEffect(Unit) {
        delay(9000)
        finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF03060C)),
        contentAlignment = Alignment.Center
    ) {
        // الخلفية السينمائية الجديدة
        Image(
            painter = painterResource(id = R.drawable.img_qabas_splash),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            alpha = 0.35f
        )

        // تدرج لوني عميق للقراءة والوضوح
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xBD03060C),
                            Color(0xD903060C),
                            Color(0xF503060C)
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            // الشعار الرسمي الجديد مع إطار هادئ وتأثير نبض لطيف
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .size(136.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(
                        1.5.dp,
                        Brush.linearGradient(
                            listOf(
                                GoldPrimary.copy(alpha = 0.8f),
                                Color(0xFF2563EB).copy(alpha = 0.5f)
                            )
                        ),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_qabas_logo),
                    contentDescription = "قبس ستوديو",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "قبس ستوديو",
                color = GoldPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = CairoFont
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "QABAS STUDIO",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontFamily = NotoSansFont
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "من شرارة الفكرة .. إلى نور الأثر",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            QabasProgressBar(
                progress = overallProgress,
                label = statusText,
                showPercentage = true,
                height = 8.dp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                steps.forEachIndexed { index, step ->
                    val isDone = index < currentStep || overallProgress >= 1f
                    val isActive = index == currentStep && overallProgress < 1f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    isDone -> GoldPrimary.copy(alpha = 0.12f)
                                    isActive -> CardSurface
                                    else -> Color(0xFF0B1120)
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isDone -> GoldPrimary.copy(alpha = 0.45f)
                                    isActive -> GoldPrimary.copy(alpha = 0.3f)
                                    else -> Color(0xFF1E293B)
                                },
                                RoundedCornerShape(14.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> GoldPrimary
                                        isActive -> GoldPrimary.copy(alpha = 0.2f)
                                        else -> Color(0xFF151B2B)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isDone -> Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = DeepSlate,
                                    modifier = Modifier.size(18.dp)
                                )
                                isActive -> QabasGlowingSpinner(size = 22.dp, strokeWidth = 2.dp)
                                else -> Icon(
                                    step.icon,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = step.title,
                            color = when {
                                isDone -> GoldPrimary
                                isActive -> Color.White
                                else -> TextSecondary
                            },
                            fontFamily = CairoFont,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "من شرارة الفكرة .. نبني نور المعرفة",
                color = GoldSecondary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = NotoSansFont,
                textAlign = TextAlign.Center
            )
        }
    }
}
