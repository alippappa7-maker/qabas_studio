package com.qabas.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qabas.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioGatewayScreen(
    onBack: () -> Unit,
    onNavigateToAiStudio: () -> Unit,
    onNavigateToProEditor: () -> Unit,
    onNavigateToStats: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    Scaffold(
        containerColor = DeepSlate,
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        Translator.tr("استوديو قبس لصناعة المحتوى"),
                        color = GoldPrimary,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = GoldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSlate)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = Translator.tr("اختر طريقة الصناعة المناسبة لهدفك الدعوي"),
                color = Color.White.copy(alpha = 0.7f),
                fontFamily = NotoSansFont,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 1. AI Studio Card (The Intelligent Path)
            StudioModeCard(
                title = Translator.tr("الاستوديو الذكي (AI)"),
                description = Translator.tr("حول فكرتك أو صوتك إلى فيديو متكامل خلال ثوانٍ. الذكاء الاصطناعي يقوم بالبحث عن الموارد، كتابة النصوص، والتركيب التلقائي."),
                icon = Icons.Default.AutoFixHigh,
                tag = Translator.tr("سريع وذكي"),
                color = GoldPrimary,
                onClick = onNavigateToAiStudio
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Pro Editor Card (The Manual Control Path)
            StudioModeCard(
                title = Translator.tr("المحرر الاحترافي (Pro)"),
                description = Translator.tr("تحكم كامل في كل ثانية. تايم لاين متعدد المسارات، تحريك نصوص، دمج مواردك الخاصة، ومونتاج يدوي باحترافية CapCut."),
                icon = Icons.Default.VideoSettings,
                tag = Translator.tr("تحكم كامل"),
                color = Color(0xFF3B82F6),
                onClick = onNavigateToProEditor
            )

            // 3. Social Stats Card (The Growth Path)
            StudioModeCard(
                title = Translator.tr("إحصائيات الانتشار (Stats)"),
                description = Translator.tr("تتبع نمو حساباتك الدعوية، عدد المشاهدات، المتابعين، وتلقَّ اقتراحات ذكية مدعومة بالذكاء الاصطناعي لزيادة التأثير."),
                icon = Icons.Default.BarChart,
                tag = Translator.tr("تحليل حقيقي"),
                color = Color(0xFF10B981),
                onClick = onNavigateToStats
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Quote / Motivation
            Surface(
                color = Color(0xFF151B2B),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Translator.tr("«قيمة المرء ما يُحسنُ صنعه».. اصنع محتوىً يبقى أثره ويُثقل ميزانك."),
                        color = Color.White.copy(alpha = 0.8f),
                        fontFamily = CairoFont,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StudioModeCard(
    title: String,
    description: String,
    icon: ImageVector,
    tag: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF151B2B),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Surface(
                    color = color.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = tag,
                        color = color,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                color = Color.White,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                color = Color.White.copy(alpha = 0.6f),
                fontFamily = NotoSansFont,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Translator.tr("ابدأ الآن"),
                    color = color,
                    fontFamily = CairoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // In RTL it will point left (forward)
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
