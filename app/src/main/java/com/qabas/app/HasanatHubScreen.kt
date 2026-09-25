package com.qabas.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
fun HasanatHubScreen(
    onBack: () -> Unit,
    onNavigate: (AppState) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    Scaffold(
        containerColor = DeepSlate,
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mosque, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "مِشكاة الهُدى",
                            color = GoldPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
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
        ) {
            Text(
                text = "رحاب النور والقرآن وعلوم أئمة وسلف الأمة المباركة",
                color = Color.White.copy(alpha = 0.75f),
                fontFamily = NotoSansFont,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 🌟 المميز في مشكاة الهدى: الصحيحان (صحيح البخاري وصحيح مسلم)
            Surface(
                color = Color(0xFF151B2B),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onNavigate(AppState.ISLAMIC_LIBRARY) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF1E293B),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(GoldPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "الصحيحان: البخاري ومسلم 🌟",
                                            fontFamily = CairoFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = GoldPrimary
                                        )
                                    }
                                    Text(
                                        "القسم الخاص بالكتب • أصح كتابين بعد كتاب الله بإجماع الأمة",
                                        fontFamily = NotoSansFont,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "مكتمل 📖",
                                    color = Color(0xFF34D399),
                                    fontFamily = CairoFont,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "تصفح كامل أبواب الصحيحين الـ 24 الموثقة، مع الشرح والبيان، استماع صوتي فصيح، اختبارات تدبر، وصناعة ريلز فورية.",
                            fontFamily = CairoFont,
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFFCBD5E1)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onNavigate(AppState.ISLAMIC_LIBRARY) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("صحيح البخاري 📖", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { onNavigate(AppState.ISLAMIC_LIBRARY) },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.8f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("صحيح مسلم 📜", color = Color(0xFF10B981), fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            val hasanatSections = listOf(
                HasanatSectionItem(
                    title = "القرآن الكريم",
                    desc = "تلاوات، تجويد، ومصحف",
                    icon = Icons.Default.MenuBook,
                    color = GoldPrimary,
                    route = AppState.QURAN_HUB
                ),
                HasanatSectionItem(
                    title = "المكتبة والكتب",
                    desc = "صحيح البخاري ومسلم، وأمهات الكتب",
                    icon = Icons.Default.LibraryBooks,
                    color = Color(0xFFF59E0B),
                    route = AppState.ISLAMIC_LIBRARY
                ),
                HasanatSectionItem(
                    title = "الأذكار وحصن المسلم",
                    desc = "أذكار الصباح والمساء والسبحة",
                    icon = Icons.Default.AutoAwesome,
                    color = ManuscriptGold,
                    route = AppState.AZKAR
                ),
                HasanatSectionItem(
                    title = "سيرة الأكابر",
                    desc = "تراجم أئمة أهل السنة والعلماء",
                    icon = Icons.Default.PersonSearch,
                    color = Color(0xFF14B8A6),
                    route = AppState.SCHOLAR_BIOGRAPHIES
                ),
                HasanatSectionItem(
                    title = "الأذان ومواقيت الصلاة",
                    desc = "غرفة التحكم، أصوات الأذان والزاد الإيماني",
                    icon = Icons.Default.NotificationsActive,
                    color = Color(0xFF3B82F6),
                    route = AppState.PRAYER_TIMES
                ),
                HasanatSectionItem(
                    title = "الصوتيات والدروس",
                    desc = "محاضرات وخطب مؤثرة",
                    icon = Icons.Default.Audiotrack,
                    color = Color(0xFFEC4899),
                    route = AppState.AUDIO_LIBRARY
                )
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(hasanatSections) { section ->
                    HasanatSectionCard(section = section) {
                        section.route?.let { onNavigate(it) }
                    }
                }
            }
        }
    }
}

data class HasanatSectionItem(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val color: Color,
    val route: AppState?
)

@Composable
fun HasanatSectionCard(
    section: HasanatSectionItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF151B2B),
        border = androidx.compose.foundation.BorderStroke(1.dp, section.color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(section.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = null,
                    tint = section.color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = Translator.tr(section.title),
                color = Color.White,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Translator.tr(section.desc),
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = NotoSansFont,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}
