package com.qabas.app

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qabas.app.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

data class HasanatSectionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val desc: String,
    val badge: String,
    val category: String, // "القرآن والسنة", "العبادات والمواقيت", "السير والتاريخ", "المكتبة والدروس"
    val icon: ImageVector,
    val accentColor: Color,
    val route: AppState
)

data class SpiritualPearl(
    val type: String, // "آية قرآنية", "حديث نبوي"
    val text: String,
    val source: String,
    val benefit: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HasanatHubScreen(
    onBack: () -> Unit,
    onNavigate: (AppState) -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // TTS للكبسولة الإيمانية
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var speech: TextToSpeech? = null
        speech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = speech?.setLanguage(Locale("ar"))
                isTtsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
        tts = speech
        onDispose {
            speech?.stop()
            speech?.shutdown()
        }
    }

    // حالات البحث والفلترة والعرض
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("الكل") }
    var isGridView by remember { mutableStateOf(true) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // عداد التسبيح السريع التفاعلي
    val prefs = remember { context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE) }
    var tasbeehCount by remember { mutableIntStateOf(prefs.getInt("mishkat_tasbeeh_count", 0)) }
    var selectedZikrIndex by remember { mutableIntStateOf(0) }
    val zikrOptions = remember {
        listOf(
            "سُبْحَانَ اللَّهِ",
            "الْحَمْدُ لِلَّهِ",
            "لا إِلَهَ إِلا اللَّهُ",
            "اللَّهُ أَكْبَرُ",
            "أَسْتَغْفِرُ اللَّهَ",
            "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ"
        )
    }

    // لآلئ الوحيين اليومية (آيات وأحاديث موثقة وصحيحة)
    val spiritualPearls = remember {
        listOf(
            SpiritualPearl(
                type = "آية قرآنية",
                text = "إِنَّ هَٰذَا الْقُرْآنَ يَهْدِي لِلَّتِي هِيَ أَقْوَمُ وَيُبَشِّرُ الْمُؤْمِنِينَ الَّذِينَ يَعْمَلُونَ الصَّالِحَاتِ أَنَّ لَهُمْ أَجْرًا كَبِيرًا",
                source = "سورة الإسراء • آية 9",
                benefit = "القرآن هو البوصلة الإلهية الكاملة لتهذيب النفس ورشاد الحياة وبشارة الصالحين."
            ),
            SpiritualPearl(
                type = "حديث نبوي شريف",
                text = "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ",
                source = "صحيح البخاري • رقم 5027",
                benefit = "أعظم مراتب الشرف والخيرية في الأمة هي الاشتغال بكتاب الله تلاوةً وفهماً وتعليماً."
            ),
            SpiritualPearl(
                type = "حديث نبوي شريف",
                text = "كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي الْمِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
                source = "صحيح البخاري ومسلم • متفق عليه",
                benefit = "ملازمة هذين الذكرين تملأ ميزان العبد بالحسنات وتجلب محبة الرحمن."
            ),
            SpiritualPearl(
                type = "آية قرآنية",
                text = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
                source = "سورة الرعد • آية 28",
                benefit = "السكينة الحقيقية وزوال القلق والاضطراب لا يكون إلا بدوام الاتصال بذكر الله."
            )
        )
    }
    var currentPearlIndex by remember { mutableIntStateOf(0) }
    val activePearl = spiritualPearls[currentPearlIndex % spiritualPearls.size]

    // حساب حقيقي وصادق لمواقيت الصلاة من تقويم الجهاز
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val defaultCity = remember { PrayerCalculationHelper.popularCities[0] } // مكة المكرمة كمرجع أولي أو المدينة المخزنة
    val calculatedPrayerTimes = remember(currentTimeMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        PrayerCalculationHelper.calculatePrayerTimes(cal, defaultCity, defaultCity.defaultMethod)
    }

    // حساب الصلاة القادمة والوقت المتبقي حقيقياً
    val nextPrayerInfo = remember(calculatedPrayerTimes, currentTimeMillis) {
        val nextName = calculatedPrayerTimes.nextPrayerName
        val nextTime = calculatedPrayerTimes.nextPrayerTimeFormatted
        val millis = calculatedPrayerTimes.millisUntilNextPrayer
        val totalSec = (millis / 1000L).coerceAtLeast(0L)
        val remH = totalSec / 3600L
        val remM = (totalSec % 3600L) / 60L
        val remS = totalSec % 60L
        val countdownStr = String.format("%02d:%02d:%02d", remH, remM, remS)

        Triple(nextName, nextTime, countdownStr)
    }

    // التاريخ الهجري الحقيقي المحسوب فلكياً
    val realDateInfo = remember(currentTimeMillis) {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        val gregorianFormat = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
        val gregStr = gregorianFormat.format(cal.time)

        // خوارزمية تقريبية دقيقة للتقويم الهجري المعتمد
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        var m = month
        var y = year
        if (m < 3) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5

        val z = jd + 0.5
        val ijd = floor(z)
        val l = ijd - 1948440 + 10632
        val n = floor((l - 1) / 10631.0)
        val lPrime = l - 10631 * n + 354
        val j = (floor((10985 - lPrime) / 5316.0)) * (floor((50 * lPrime) / 17719.0)) + (floor(lPrime / 5670.0)) * (floor((43 * lPrime) / 15238.0))
        val lDoublePrime = lPrime - (floor((30 - j) / 15.0)) * (floor((17719 * j) / 50.0)) - (floor(j / 16.0)) * (floor((15238 * j) / 43.0)) + 29
        val hijriMonth = floor((24 * lDoublePrime) / 709.0).toInt()
        val hijriDay = (lDoublePrime - floor((709 * hijriMonth) / 24.0)).toInt()
        val hijriYear = (30 * n + j - 30).toInt()

        val hijriMonthsAr = listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )
        val hMonthName = hijriMonthsAr.getOrElse(hijriMonth - 1) { "رمضان" }
        val hijriStr = "$hijriDay $hMonthName $hijriYear هـ"

        Pair(hijriStr, gregStr)
    }

    // أقسام مشكاة الهدى الـ 8 المعتمدة مع البيانات الحقيقية والبيان
    val sections = remember {
        listOf(
            HasanatSectionItem(
                id = "sahihain",
                title = "الصحيحان: البخاري ومسلم",
                subtitle = "أصح كتابين بعد كتاب الله",
                desc = "متون محققة كاملة، شروح الأئمة، تفريغ أحاديث، ونظام استماع صوتي نقي.",
                badge = "7,563 حديث صحيح",
                category = "القرآن والسنة",
                icon = Icons.Default.AutoStories,
                accentColor = GoldPrimary,
                route = AppState.ISLAMIC_LIBRARY
            ),
            HasanatSectionItem(
                id = "quran",
                title = "القرآن الكريم والتجويد",
                subtitle = "المصحف المرتل برواية حفص",
                desc = "تلاوات خاشعة لكبار القراء، أحكام التجويد، تدبر الآيات، وحفظ الآيات متزامنة.",
                badge = "114 سورة • 6236 آية",
                category = "القرآن والسنة",
                icon = Icons.Default.MenuBook,
                accentColor = Color(0xFF10B981),
                route = AppState.QURAN_HUB
            ),
            HasanatSectionItem(
                id = "library",
                title = "المكتبة الإسلامية الجامعة",
                subtitle = "أمهات كتب السلف والتراث",
                desc = "كتب العقيدة والتفسير والفقه المعتمدة مع فهارس ذكية وكبسولات تلخيص حكمة.",
                badge = "كتب محققة معتمدة",
                category = "المكتبة والدروس",
                icon = Icons.Default.LibraryBooks,
                accentColor = Color(0xFFF59E0B),
                route = AppState.ISLAMIC_LIBRARY
            ),
            HasanatSectionItem(
                id = "azkar",
                title = "الأذكار وحصن المسلم",
                subtitle = "أذكار الصباح والمساء واليوم",
                desc = "الأدعية المأثورة الموثقة مع عداد سبحة تفاعلي وتنبيهات أوقات الاستجابة.",
                badge = "130+ ذكر مأثور",
                category = "العبادات والمواقيت",
                icon = Icons.Default.AutoAwesome,
                accentColor = Color(0xFFEAB308),
                route = AppState.AZKAR
            ),
            HasanatSectionItem(
                id = "prayer",
                title = "محراب الصلاة والمواقيت",
                subtitle = "حساب فلكي دقيق وبوصلة القبلة",
                desc = "أوقات الصلوات الخمس بحساب فلكي معتمد، تنبيهات الأذان، ورياض الجنة وسنن الرواتب.",
                badge = "مواقيت حية • بوصلة",
                category = "العبادات والمواقيت",
                icon = Icons.Default.NotificationsActive,
                accentColor = Color(0xFF38BDF8),
                route = AppState.PRAYER_TIMES
            ),
            HasanatSectionItem(
                id = "scholars",
                title = "سيرة الأكابر والتراجم",
                subtitle = "أئمة السلف والتابعين والعلماء",
                desc = "سير الأئمة الأربعة وعلماء الحديث وصناع التاريخ الإسلامي المشرق مع الدروس المستفادة.",
                badge = "تراجم أئمة الهدى",
                category = "السير والتاريخ",
                icon = Icons.Default.PersonSearch,
                accentColor = Color(0xFF14B8A6),
                route = AppState.SCHOLAR_BIOGRAPHIES
            ),
            HasanatSectionItem(
                id = "qasas",
                title = "قصص الأنبياء والعِبر",
                subtitle = "سرد قرآني مهيب وتأملات",
                desc = "قصص الرسل كما وردت في الذكر الحكيم مع استخلاص القواعد الحياتية والإيمانية.",
                badge = "قصص القرآن المحكمة",
                category = "السير والتاريخ",
                icon = Icons.Default.HistoryEdu,
                accentColor = Color(0xFFA855F7),
                route = AppState.QASAS
            ),
            HasanatSectionItem(
                id = "audio",
                title = "الصوتيات والمحاضرات",
                subtitle = "تسجيلات وتلاوات نقية مؤثرة",
                desc = "مكتبة صوتية إسلامية تضم تلاوات نادرة ومحاضرات علمية منتقاة بعناية.",
                badge = "تسجيلات عالية النقاء",
                category = "المكتبة والدروس",
                icon = Icons.Default.Audiotrack,
                accentColor = Color(0xFFEC4899),
                route = AppState.AUDIO_LIBRARY
            )
        )
    }

    // تصفية الأقسام بحسب البحث والتصنيف
    val filteredSections = remember(searchQuery, selectedCategory, sections) {
        sections.filter { item ->
            val matchCategory = selectedCategory == "الكل" || item.category == selectedCategory
            val matchSearch = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.subtitle.contains(searchQuery, ignoreCase = true) ||
                    item.desc.contains(searchQuery, ignoreCase = true) ||
                    item.badge.contains(searchQuery, ignoreCase = true)
            matchCategory && matchSearch
        }
    }

    Scaffold(
        containerColor = DeepSlate,
        bottomBar = bottomBar,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.15f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mosque, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "مِشكاة الهُدى",
                                color = GoldPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                maxLines = 1
                            )
                            Text(
                                "رحاب القرآن والسنة وعلوم سلف الأمة",
                                color = TextSecondary,
                                fontFamily = NotoSansFont,
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = GoldPrimary)
                    }
                },
                actions = {
                    // زر تبديل البحث
                    IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                        Icon(
                            if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = if (isSearchExpanded) GoldPrimary else TextSecondary
                        )
                    }
                    // زر تبديل نمط العرض (شبكة / قائمة)
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "نمط العرض",
                            tint = GoldPrimary
                        )
                    }
                    // زر دليل مشكاة الهدى
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "حول مشكاة الهدى", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0A0F1A))
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // شريط البحث المدمج السلس
            if (isSearchExpanded) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث في أقسام ومحتويات مشكاة الهدى...", color = TextSecondary, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldPrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "مسح", tint = TextSecondary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF26334D),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            }

            // 1️⃣ صرح مشكاة الهدى الزجاجي الفاخر (Hero Header - Dark Luxury + Glassmorphism)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1728).copy(alpha = 0.95f)),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(GoldPrimary.copy(alpha = 0.5f), Color(0x33B89758), GoldSecondary.copy(alpha = 0.35f))
                        )
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF162035), Color(0xFF0F1626), Color(0xFF090D18))
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Column {
                            // سطر التاريخ الهجري والميلادي الحقيقي
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = realDateInfo.first,
                                        color = GoldPrimary,
                                        fontFamily = CairoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = realDateInfo.second,
                                    color = TextSecondary,
                                    fontFamily = NotoSansFont,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // بطاقة الصلاة القادمة الحية التفاعلية
                            Surface(
                                color = Color(0xFF0A0F1D).copy(alpha = 0.8f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF1E2D4A)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigate(AppState.PRAYER_TIMES) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                                                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                "الصلاة القادمة: صلاة ${nextPrayerInfo.first} (${nextPrayerInfo.second})",
                                                color = Color.White,
                                                fontFamily = CairoFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                "متبقي على النداء: ${nextPrayerInfo.third}",
                                                color = Color(0xFF38BDF8),
                                                fontFamily = NotoSansFont,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "فتح المواقيت", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 2️⃣ كبسولة الحكمة ونور الوحيين اليومية مع خيارات تحكم كاملة
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111728).copy(alpha = 0.95f)),
                    border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "✨ ${activePearl.type}",
                                    color = GoldPrimary,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }

                            // أزرار التحكم بالكبسولة (استماع، نسخ، تبديل)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        if (isTtsReady && tts != null) {
                                            tts?.speak(activePearl.text, TextToSpeech.QUEUE_FLUSH, null, "pearl_tts")
                                            Toast.makeText(context, "جاري القراءة الصوتية 🔊", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "محرك الصوت غير متوفر في جهازك", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "استماع", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val fullText = "«${activePearl.text}»\n📌 المصدر: ${activePearl.source}\n💡 الفائدة: ${activePearl.benefit}\n✨ تطبيق قبس — مشكاة الهدى"
                                        clipboardManager.setText(AnnotatedString(fullText))
                                        Toast.makeText(context, "تم نسخ النص إلى الحافظة 📋", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val shareText = "«${activePearl.text}»\n📌 ${activePearl.source}\n✨ تطبيق قبس — مشكاة الهدى"
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "مشاركة الحكمة"))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { currentPearlIndex++ },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "تبديل", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // نص الآية / الحديث بالخط العربي الأصيل
                        Text(
                            text = "« ${activePearl.text} »",
                            color = Color.White,
                            fontFamily = AmiriFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📌 ${activePearl.source}",
                                color = GoldSecondary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "💡 ${activePearl.benefit}",
                                color = TextSecondary,
                                fontFamily = NotoSansFont,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }

            // 3️⃣ السبحة الذكية المدمجة (Quick Interactive Tasbeeh Counter)
            item {
                Surface(
                    color = Color(0xFF0D1424).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF1E2B44)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEAB308).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFFEAB308), modifier = Modifier.size(15.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "السبحة الذكية المدمجة",
                                    color = Color.White,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            // زر تصفير العداد
                            TextButton(
                                onClick = {
                                    tasbeehCount = 0
                                    prefs.edit().putInt("mishkat_tasbeeh_count", 0).apply()
                                    Toast.makeText(context, "تمت إعادة تعيين العداد", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("تصفير", color = TextSecondary, fontFamily = CairoFont, fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // شريط خيارات الأذكار
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(zikrOptions.indices.toList()) { idx ->
                                val isSelected = selectedZikrIndex == idx
                                Surface(
                                    color = if (isSelected) GoldPrimary else Color(0xFF131B2C),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isSelected) GoldPrimary else Color(0xFF23304A)),
                                    modifier = Modifier.clickable { selectedZikrIndex = idx }
                                ) {
                                    Text(
                                        text = zikrOptions[idx],
                                        color = if (isSelected) DeepSlate else Color.White,
                                        fontFamily = CairoFont,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // زر الضغط للتسبيح الدائري المضيء
                        Button(
                            onClick = {
                                tasbeehCount++
                                prefs.edit().putInt("mishkat_tasbeeh_count", tasbeehCount).apply()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152035)),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = zikrOptions[selectedZikrIndex],
                                    color = GoldPrimary,
                                    fontFamily = AmiriFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Surface(
                                    color = GoldPrimary.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.dp, GoldPrimary)
                                ) {
                                    Text(
                                        text = "$tasbeehCount",
                                        color = Color.White,
                                        fontFamily = CairoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4️⃣ فلاتر تصنيف الأقسام السريعة (Category Filter Chips)
            item {
                val categoryFilters = listOf("الكل", "القرآن والسنة", "العبادات والمواقيت", "السير والتاريخ", "المكتبة والدروس")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categoryFilters) { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            color = if (isSelected) GoldPrimary else Color(0xFF121927),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (isSelected) GoldPrimary else Color(0xFF222F47)),
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) DeepSlate else TextSecondary,
                                fontFamily = CairoFont,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 5️⃣ أقسام مشكاة الهدى الـ 8 إما بنمط الشبكة المتزنة أو القائمة المفصلة
            if (isGridView) {
                // نمط الشبكة الأنيقة ثنائية الأعمدة (Grid Mode)
                item {
                    val chunked = filteredSections.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        chunked.forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { item ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        MishkatGridCard(section = item) {
                                            onNavigate(item.route)
                                        }
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                // نمط القائمة المفصلة المباشرة (List Mode)
                items(filteredSections, key = { it.id }) { item ->
                    MishkatListCard(section = item) {
                        onNavigate(item.route)
                    }
                }
            }
        }
    }

    // حوار تعريف مشكاة الهدى ورسالتها
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mosque, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("رسالة مِشكاة الهُدى", fontFamily = CairoFont, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "قسم «مِشكاة الهُدى» في تطبيق قبس هو المرفأ الإيماني والعلمي الجامع للمسلم، يهدف إلى:",
                        color = TextSecondary,
                        fontFamily = NotoSansFont,
                        fontSize = 11.sp
                    )
                    Text("• توفير نصوص القرآن الكريم وتلاواته المعتمدة آية بآية.", color = Color.White, fontFamily = CairoFont, fontSize = 11.sp)
                    Text("• خدمة الصحيحين (البخاري ومسلم) بأعلى درجات التوثيق والتحقيق.", color = Color.White, fontFamily = CairoFont, fontSize = 11.sp)
                    Text("• ضبط مواقيت الصلاة والأذكار النبوية ببيانات حقيقية خالية من أي تزييف.", color = Color.White, fontFamily = CairoFont, fontSize = 11.sp)
                    Text("• ربط أجيال الأمة بسيرة أئمة وسلف الهدى الصالحين.", color = Color.White, fontFamily = CairoFont, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حسناً", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF131A2B),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/** بطاقة القسم في نمط الشبكة المتزنة دون أي مساحات ميتة */
@Composable
private fun MishkatGridCard(
    section: HasanatSectionItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF111726).copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(section.accentColor.copy(alpha = 0.45f), Color(0x1AFFFFFF), section.accentColor.copy(alpha = 0.15f))
            )
        ),
        shadowElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(section.accentColor.copy(alpha = 0.08f), Color.Transparent, Color(0xFF0A0E18).copy(alpha = 0.4f))
                    )
                )
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // الأيقونة والشارة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(section.accentColor.copy(alpha = 0.15f))
                            .border(1.dp, section.accentColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(section.icon, contentDescription = null, tint = section.accentColor, modifier = Modifier.size(20.dp))
                    }

                    Surface(
                        color = section.accentColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, section.accentColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = section.badge,
                            color = section.accentColor,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // العنوان الرئيسي
                Text(
                    text = section.title,
                    color = Color.White,
                    fontFamily = CairoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // الوصف المختصر المركز
                Text(
                    text = section.desc,
                    color = TextSecondary,
                    fontFamily = NotoSansFont,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** بطاقة القسم في نمط القائمة المفصلة الفاخرة */
@Composable
private fun MishkatListCard(
    section: HasanatSectionItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF111726).copy(alpha = 0.95f),
        border = BorderStroke(1.dp, section.accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(section.accentColor.copy(alpha = 0.15f))
                    .border(1.dp, section.accentColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(section.icon, contentDescription = null, tint = section.accentColor, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = section.title,
                        color = Color.White,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Surface(
                        color = section.accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = section.badge,
                            color = section.accentColor,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = section.subtitle,
                    color = GoldSecondary,
                    fontFamily = CairoFont,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = section.desc,
                    color = TextSecondary,
                    fontFamily = NotoSansFont,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "فتح", tint = section.accentColor, modifier = Modifier.size(16.dp))
        }
    }
}
