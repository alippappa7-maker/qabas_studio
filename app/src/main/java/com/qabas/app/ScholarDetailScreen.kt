package com.qabas.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.qabas.app.R
import com.qabas.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScholarDetailScreen(
    scholar: ScholarBiography,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isAudioPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showShareModal by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun toggleAudio() {
        if (isAudioPlaying) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            isAudioPlaying = false
        } else {
            val url = scholar.audioUrl ?: return
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(url)
                    prepareAsync()
                    setOnPreparedListener {
                        start()
                        isAudioPlaying = true
                    }
                    setOnCompletionListener {
                        isAudioPlaying = false
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر تشغيل التسجيل الصوتي", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val tabs = listOf(
        "📜 المخطوطة",
        "⛓️ المحنة والأذى",
        "🛡️ الصبر والثبات",
        "🌅 مشهد الوفاة",
        "💌 الوصية للأمة"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Full Page Manuscript Background
        AsyncImage(
            model = R.drawable.scholar_manuscript_bg_1790092377371,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.18f
        )
        Box(modifier = Modifier.fillMaxSize().background(AntiqueParchment.copy(alpha = 0.90f)))

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            scholar.name,
                            color = OldInk,
                            fontFamily = AmiriFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = OldInk)
                        }
                    },
                    actions = {
                        if (scholar.audioUrl != null) {
                            IconButton(onClick = { toggleAudio() }) {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                    contentDescription = "صوت السيرة",
                                    tint = if (isAudioPlaying) Color(0xFF10B981) else ManuscriptGold
                                )
                            }
                        }
                        IconButton(onClick = { showShareModal = true }) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة السيرة كاملة", tint = ManuscriptGold)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Quick Category Tab Bar
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = ManuscriptGold,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontFamily = CairoFont,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp,
                                    color = if (selectedTab == index) OldInk else OldInk.copy(alpha = 0.6f)
                                )
                            }
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = ManuscriptGold.copy(alpha = 0.3f),
                    thickness = 1.dp
                )

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Portrait & Hero Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AgedPaper,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, ManuscriptGold.copy(alpha = 0.4f)),
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(140.dp),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(3.dp, ManuscriptGold),
                                shadowElevation = 6.dp
                            ) {
                                AsyncImage(
                                    model = scholar.imageRes,
                                    contentDescription = scholar.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = scholar.name,
                                color = OldInk,
                                fontFamily = AmiriFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                textAlign = TextAlign.Center
                            )

                            scholar.period?.let {
                                Text(
                                    text = "العصر: $it",
                                    color = ManuscriptGold,
                                    fontFamily = CairoFont,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Endurance Quote Pill
                            scholar.enduranceQuote?.let { quote ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = ManuscriptGold.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ManuscriptGold.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = "« $quote »",
                                        color = OldInk,
                                        fontFamily = AmiriFont,
                                        fontSize = 17.sp,
                                        lineHeight = 26.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dynamic Section Display based on tab
                    when (selectedTab) {
                        0 -> {
                            ManuscriptIntroSection(scholar)
                            Spacer(modifier = Modifier.height(16.dp))
                            HarmedAndPersecutionSection(context, scholar)
                            Spacer(modifier = Modifier.height(16.dp))
                            EnduranceAndPurposeSection(context, scholar)
                            Spacer(modifier = Modifier.height(16.dp))
                            DeathSceneAndFarewellSection(context, scholar)
                            Spacer(modifier = Modifier.height(16.dp))
                            LegacyAndMessageSection(context, scholar)
                        }
                        1 -> {
                            HarmedAndPersecutionSection(context, scholar)
                        }
                        2 -> {
                            EnduranceAndPurposeSection(context, scholar)
                        }
                        3 -> {
                            DeathSceneAndFarewellSection(context, scholar)
                        }
                        4 -> {
                            LegacyAndMessageSection(context, scholar)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Universal Share Button
                    Button(
                        onClick = { showShareModal = true },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ManuscriptGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "مشاركة السيرة كاملة (صورة أو نص)",
                            color = Color.White,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // Share Bottom Sheet Dialog
    if (showShareModal) {
        ScholarShareModal(
            scholar = scholar,
            onDismiss = { showShareModal = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScholarShareModal(
    scholar: ScholarBiography,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedFormat by remember { mutableIntStateOf(0) } // 0: صورة كاملة, 1: نص شامل
    var selectedStyle by remember { mutableStateOf(ScholarCardImageGenerator.CardStyle.FULL_EPIC) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingImage by remember { mutableStateOf(false) }
    var isSavingToGallery by remember { mutableStateOf(false) }

    // Generate bitmap whenever selected style or dialog opens
    LaunchedEffect(selectedStyle) {
        isGeneratingImage = true
        try {
            renderedBitmap = ScholarCardImageGenerator.generateScholarBitmap(context, scholar, selectedStyle)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "حدث خطأ أثناء إعداد الصورة", Toast.LENGTH_SHORT).show()
        } finally {
            isGeneratingImage = false
        }
    }

    val fullText = remember(scholar) {
        ScholarCardImageGenerator.buildFullBiographyText(scholar)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AgedPaper,
        dragHandle = { BottomSheetDefaults.DragHandle(color = ManuscriptGold) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = OldInk)
                }

                Text(
                    text = "مشاركة سيرة الإمام ${scholar.name}",
                    fontFamily = CairoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = OldInk
                )

                Icon(
                    Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = ManuscriptGold,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Selector: صورة عالية الدقة / نص شامل
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFormat = 0 },
                    color = if (selectedFormat == 0) ManuscriptGold else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = if (selectedFormat == 0) Color.White else OldInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "بطاقة مصورة (تفاصيل كاملة)",
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedFormat == 0) Color.White else OldInk
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFormat = 1 },
                    color = if (selectedFormat == 1) ManuscriptGold else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Article,
                            contentDescription = null,
                            tint = if (selectedFormat == 1) Color.White else OldInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "نص السيرة الشامل",
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedFormat == 1) Color.White else OldInk
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedFormat == 0) {
                // Image Share Content
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Style Selector (الملحمية الكاملة vs قصة سريعة)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedStyle == ScholarCardImageGenerator.CardStyle.FULL_EPIC,
                            onClick = { selectedStyle = ScholarCardImageGenerator.CardStyle.FULL_EPIC },
                            label = { Text("المخطوطة الملحمية الكاملة", fontFamily = CairoFont, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ManuscriptGold,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedStyle == ScholarCardImageGenerator.CardStyle.HIGHLIGHT_STORY,
                            onClick = { selectedStyle = ScholarCardImageGenerator.CardStyle.HIGHLIGHT_STORY },
                            label = { Text("بطاقة الحالة والقصة (9:16)", fontFamily = CairoFont, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ManuscriptGold,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Image Preview Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        color = Color.White.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, ManuscriptGold.copy(alpha = 0.4f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (isGeneratingImage) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = ManuscriptGold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "جاري رسم وتجهيز بطاقة السيرة بجودة عالية...",
                                        fontFamily = CairoFont,
                                        fontSize = 12.sp,
                                        color = OldInk
                                    )
                                }
                            } else if (renderedBitmap != null) {
                                Image(
                                    bitmap = renderedBitmap!!.asImageBitmap(),
                                    contentDescription = "معاينة البطاقة المصورة",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row (Share Image + Save to Gallery)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val bmp = renderedBitmap ?: return@launch
                                    val uri = ScholarCardImageGenerator.saveBitmapToCache(context, bmp, scholar.name)
                                    ScholarCardImageGenerator.shareImageUri(context, uri, scholar)
                                }
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ManuscriptGold),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isGeneratingImage && renderedBitmap != null
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "مشاركة الصورة",
                                color = Color.White,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val bmp = renderedBitmap ?: return@launch
                                    isSavingToGallery = true
                                    val success = ScholarCardImageGenerator.saveBitmapToGallery(context, bmp, scholar.name)
                                    isSavingToGallery = false
                                    if (success) {
                                        Toast.makeText(context, "تم حفظ بطاقة السيرة في المعرض بنجاح ✨", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "تعذر حفظ الصورة في المعرض", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, ManuscriptGold),
                            enabled = !isGeneratingImage && renderedBitmap != null && !isSavingToGallery
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = ManuscriptGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isSavingToGallery) "جاري الحفظ.." else "حفظ بالمعرض",
                                color = ManuscriptGold,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                // Text Share Content
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Scrollable Text Preview Box
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        color = Color.White.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, ManuscriptGold.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = fullText,
                                color = OldInk,
                                fontFamily = AmiriFont,
                                fontSize = 16.sp,
                                lineHeight = 26.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons Row (Share Text + Copy Text)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                ScholarCardImageGenerator.shareText(context, scholar)
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ManuscriptGold),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "مشاركة النص كاملاً",
                                color = Color.White,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                copyToClipboard(context, "سيرة الإمام ${scholar.name}", fullText)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, ManuscriptGold)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = ManuscriptGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "نسخ النص",
                                color = ManuscriptGold,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ManuscriptIntroSection(scholar: ScholarBiography) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AgedPaper,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ManuscriptGold.copy(alpha = 0.3f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = ManuscriptGold, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "تاريخُ الإمام ومكانته في الأمة:",
                    color = ManuscriptGold,
                    fontFamily = CairoFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = scholar.brief,
                color = OldInk,
                fontFamily = AmiriFont,
                fontSize = 18.sp,
                lineHeight = 30.sp,
                textAlign = TextAlign.Justify
            )

            scholar.travelDistance?.let { travel ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Explore, contentDescription = null, tint = ManuscriptGold.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "الرحلة والطلب: $travel",
                        color = OldInk.copy(alpha = 0.8f),
                        fontFamily = CairoFont,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HarmedAndPersecutionSection(context: Context, scholar: ScholarBiography) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AgedPaper,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFB91C1C).copy(alpha = 0.4f)),
        shadowElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Gavel,
                        contentDescription = null,
                        tint = Color(0xFFB91C1C),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "من الذين آذوه وكيف؟ (المحنة الكبرى)",
                        color = Color(0xFFB91C1C),
                        fontFamily = CairoFont,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        scholar.whoHarmedHimAndHow?.let { copyToClipboard(context, "محنة ${scholar.name}", it) }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color(0xFFB91C1C).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = scholar.whoHarmedHimAndHow ?: "واجه ابتلاءات شديدة في سبيل الله وصبر عليها.",
                color = OldInk,
                fontFamily = AmiriFont,
                fontSize = 17.5.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Justify
            )
        }
    }
}

@Composable
fun EnduranceAndPurposeSection(context: Context, scholar: ScholarBiography) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // How he endured
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AgedPaper,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF047857).copy(alpha = 0.4f)),
            shadowElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "كيف تحمّل؟ وسر الصمود الإيماني",
                            color = Color(0xFF047857),
                            fontFamily = CairoFont,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            scholar.howHeEndured?.let { copyToClipboard(context, "صبر ${scholar.name}", it) }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = Color(0xFF047857).copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = scholar.howHeEndured ?: "استعان بالله تعالى واحتسب الأجر في كل لحظة.",
                    color = OldInk,
                    fontFamily = AmiriFont,
                    fontSize = 17.5.sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Justify
                )
            }
        }

        // Why he endured
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AgedPaper,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ManuscriptGold.copy(alpha = 0.4f)),
            shadowElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = null,
                            tint = ManuscriptGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "لماذا تحمّل؟ القضية الكبرى والغاية",
                            color = ManuscriptGold,
                            fontFamily = CairoFont,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = {
                            scholar.whyHeEndured?.let { copyToClipboard(context, "ثبات ${scholar.name}", it) }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = ManuscriptGold.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = scholar.whyHeEndured ?: "لحفظ دين الله وصيانة شريعة الإسلام للأجيال.",
                    color = OldInk,
                    fontFamily = AmiriFont,
                    fontSize = 17.5.sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Justify
                )
            }
        }
    }
}

@Composable
fun DeathSceneAndFarewellSection(context: Context, scholar: ScholarBiography) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // The Death Scene
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AgedPaper,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6B21A8).copy(alpha = 0.4f)),
            shadowElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = Color(0xFF6B21A8),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "مشهد الوفاة واللحظات الأخيرة المؤثرة:",
                        color = Color(0xFF6B21A8),
                        fontFamily = CairoFont,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = scholar.deathScene ?: "فاضت روحه في طاعة الله وذكره.",
                    color = OldInk,
                    fontFamily = AmiriFont,
                    fontSize = 18.sp,
                    lineHeight = 30.sp,
                    textAlign = TextAlign.Justify
                )
            }
        }

        // Highlight Box: Last Words
        scholar.lastWords?.let { lastWords ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, ManuscriptGold)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = ManuscriptGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "آخر كلماته قبل خروج الروح:",
                        color = ManuscriptGold,
                        fontFamily = CairoFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "« $lastWords »",
                        color = OldInk,
                        fontFamily = AmiriFont,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Funeral and Aftermath
        scholar.funeralImpact?.let { funeral ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AgedPaper,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ManuscriptGold.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = ManuscriptGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "مشهد الجنازة وأثر الفقد على الأمة:",
                            color = ManuscriptGold,
                            fontFamily = CairoFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = funeral,
                        color = OldInk,
                        fontFamily = AmiriFont,
                        fontSize = 17.5.sp,
                        lineHeight = 28.sp,
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }
    }
}

@Composable
fun LegacyAndMessageSection(context: Context, scholar: ScholarBiography) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Global Impact
        scholar.globalImpact?.let { impact ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AgedPaper,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ManuscriptGold.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = ManuscriptGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "الأثر التاريخي الخالد:",
                            color = ManuscriptGold,
                            fontFamily = CairoFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = impact,
                        color = OldInk,
                        fontFamily = NotoSansFont,
                        fontSize = 14.5.sp,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Message to Grandson
        scholar.messageToGrandson?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ManuscriptGold.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, ManuscriptGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.HistoryEdu,
                        contentDescription = null,
                        tint = ManuscriptGold,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "وصيةُ الإمامِ لكَ يا حفيدَهُ:",
                        color = ManuscriptGold,
                        fontFamily = CairoFont,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "« $message »",
                        color = OldInk,
                        fontFamily = AmiriFont,
                        fontSize = 20.sp,
                        lineHeight = 32.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Modern Daily Challenge
        scholar.modernChallenge?.let { challenge ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.6f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ManuscriptGold.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = ManuscriptGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "تحدي الاقتداء المعاصر اليوم:",
                            color = ManuscriptGold,
                            fontFamily = CairoFont,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = challenge,
                            color = OldInk,
                            fontFamily = NotoSansFont,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
