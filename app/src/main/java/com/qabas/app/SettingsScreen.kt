package com.qabas.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.qabas.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * شاشات الإعدادات الفرعية المستقلة (Dedicated Sub-Screens Architecture)
 */
enum class SettingsSubScreen {
    MAIN,
    AI_IDENTITY,
    PREFERENCES_QUALITY,
    READING_CUSTOMIZATION,
    SPIRITUAL_PREFERENCES,
    STUDIO_WATERMARK,
    STORAGE_BACKUP,
    OFFICIAL_CHANNELS,
    SOCIAL_PLATFORMS,
    SUPPORT_CONTRIBUTION,
    ABOUT_APP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToTasteProfile: () -> Unit = {},
    onNavigateToPremiumUpgrade: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember(context) { context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE) }

    // حالة الشاشة الفرعية الحالية
    var currentSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }

    // معالجة الرجوع بنظام BackHandler
    BackHandler(enabled = currentSubScreen != SettingsSubScreen.MAIN) {
        currentSubScreen = SettingsSubScreen.MAIN
    }

    // بيانات المستخدم والملف الشخصي
    var savedDisplayName by remember {
        mutableStateOf(
            prefs.getString("user_display_name", null)
                ?: CloudServices.Auth.currentUser.value?.displayName
                ?: "مستخدم قبس"
        )
    }
    var avatarUriString by remember {
        mutableStateOf(
            prefs.getString("user_avatar_uri", null)
                ?: CloudServices.Auth.currentUser.value?.photoUrl?.toString()
                ?: ""
        )
    }
    var selectedPresetAvatar by remember {
        mutableIntStateOf(prefs.getInt("user_preset_avatar", 0))
    }

    val userEmail = CloudServices.Auth.currentUser.value?.email ?: "المستخدم الحالي"
    val isAdmin = prefs.getBoolean("is_admin", false) || CloudServices.isOwnerAccount(userEmail)
    val isRelative = prefs.getBoolean("is_relative_user", false)
    val isGuest = prefs.getBoolean("is_guest", false)

    var autoPublishing by remember {
        mutableStateOf(prefs.getBoolean("auto_publish", false))
    }
    var defaultQuality by remember {
        mutableStateOf(prefs.getString("default_export_quality", "1080p") ?: "1080p")
    }

    // الحسابات وقنوات التواصل
    var accountsList by remember { mutableStateOf(SocialAccountManager.getAccounts(context)) }
    var linkDialogState by remember { mutableStateOf<String?>(null) } // "youtube", "tiktok", "instagram"
    var isLinking by remember { mutableStateOf(false) }
    var linkVerifyMsg by remember { mutableStateOf<String?>(null) }

    val selectedPlatform = remember(linkDialogState, accountsList) {
        accountsList.find { it.id == linkDialogState }
    }
    var customHandle by remember(selectedPlatform) {
        mutableStateOf(selectedPlatform?.handle ?: "")
    }
    var customToken by remember(selectedPlatform) {
        mutableStateOf(if (selectedPlatform != null) SocialAccountManager.getAccessToken(context, selectedPlatform.id) else "")
    }

    // التحديثات والنظام
    var showAboutAppDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var updateState by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<UpdateManager.UpdateInfo?>(null) }
    var updateProgress by remember { mutableStateOf(0) }
    var updateDownloadedBytes by remember { mutableStateOf(0L) }
    var updateTotalBytes by remember { mutableStateOf(0L) }
    var updateSpeedBps by remember { mutableStateOf(0L) }
    var updatePhase by remember { mutableStateOf<UpdateManager.Phase?>(null) }
    var updateDoneMessage by remember { mutableStateOf("") }
    var updateActiveKey by remember { mutableStateOf<String?>(null) }
    var lastConsumedResult by remember { mutableStateOf<Triple<String, Boolean, String>?>(null) }
    var cacheSizeBytes by remember { mutableStateOf<Long?>(null) }

    // حساب حجم المؤقتات
    LaunchedEffect(Unit) {
        cacheSizeBytes = withContext(Dispatchers.IO) {
            runCatching {
                var total = 0L
                context.cacheDir.walkTopDown().forEach { f -> if (f.isFile) total += f.length() }
                total
            }.getOrNull()
        }
    }

    // خدمة تنزيل التحديثات في الخلفية
    LaunchedEffect(Unit) {
        UpdateManager.downloadProgress.collect { p ->
            if (p != null && updateState == "downloading" && updateActiveKey != null) {
                updateProgress = p.percent
                updateDownloadedBytes = p.bytesDownloaded
                updateTotalBytes = p.totalBytes
                updateSpeedBps = p.speedBytesPerSec
                updatePhase = p.phase
            }
        }
    }
    LaunchedEffect(Unit) {
        UpdateManager.downloadResult.collect { r ->
            if (r != null && r != lastConsumedResult && r.first == updateActiveKey && updateState == "downloading") {
                lastConsumedResult = r
                updateDoneMessage = r.third
                updateState = when {
                    r.second -> "done"
                    r.third.contains("أُلغي") -> "found"
                    else -> "error"
                }
            }
        }
    }

    fun beginServiceDownload(info: UpdateManager.UpdateInfo, forceFull: Boolean) {
        updateActiveKey = "${info.versionName}|${info.versionCode}"
        lastConsumedResult = null
        updateState = "downloading"
        updateProgress = 0
        updateDownloadedBytes = 0L
        updateTotalBytes = 0L
        updateSpeedBps = 0L
        updatePhase = null
        UpdateDownloadService.start(context, info, forceFull)
    }

    // فحص التحديثات
    LaunchedEffect(Unit) {
        if (updateState == null) {
            updateState = "checking..."
            runCatching {
                val info = UpdateManager.checkForUpdate(context)
                if (info != null) {
                    updateInfo = info
                    updateState = "found"
                    prefs.edit().putString("last_release_notes", info.releaseNotes).apply()
                } else {
                    updateState = null
                }
            }.onFailure {
                updateState = null
            }
        }
    }

    // Developer Mode
    var devTapCount by remember { mutableIntStateOf(0) }
    var showPassphraseDialog by remember { mutableStateOf(false) }
    var passphraseInput by remember { mutableStateOf("") }
    var passphraseError by remember { mutableStateOf(false) }
    var isDevMode by remember {
        mutableStateOf(prefs.getBoolean("is_developer", false))
    }

    // الحسابات الرسمية
    val canManageChannels = isAdmin || isDevMode
    var officialChannels by remember { mutableStateOf(SocialAccountManager.getOfficialChannels(context)) }
    fun refreshChannels() {
        officialChannels = SocialAccountManager.getOfficialChannels(context)
    }
    var channelDraftId by remember { mutableStateOf<String?>(null) }
    var chPlatform by remember { mutableStateOf("") }
    var chDisplay by remember { mutableStateOf("") }
    var chHandle by remember { mutableStateOf("") }
    var chUrl by remember { mutableStateOf("") }
    var chDesc by remember { mutableStateOf("") }
    var chUrlError by remember { mutableStateOf(false) }
    fun openChannelEditor(c: OfficialChannelInfo?) {
        channelDraftId = c?.id ?: ""
        chPlatform = c?.platformName ?: ""
        chDisplay = c?.displayName ?: ""
        chHandle = c?.handle ?: ""
        chUrl = c?.url ?: ""
        chDesc = c?.description ?: ""
        chUrlError = false
    }

    // حوار ربط حساب تواصل
    if (linkDialogState != null && selectedPlatform != null) {
        AlertDialog(
            onDismissRequest = { if (!isLinking) linkDialogState = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (selectedPlatform.id) {
                            "youtube" -> Icons.Default.OndemandVideo
                            "tiktok" -> Icons.Default.MusicVideo
                            else -> Icons.Default.CameraAlt
                        },
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        Translator.tr("إعدادات ربط ") + selectedPlatform.name,
                        color = GoldPrimary,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        Translator.tr("اربط حسابك لنشر المقاطع وتصديرها مباشرة إلى ") + selectedPlatform.name,
                        color = Color.White,
                        fontFamily = CairoFont,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = customHandle,
                        onValueChange = { customHandle = it },
                        label = { Text(Translator.tr("اسم المستخدم أو المعرف (Handle)"), fontSize = 12.sp) },
                        placeholder = { Text("@username", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = customToken,
                        onValueChange = { customToken = it },
                        label = { Text(Translator.tr("رمز الوصول (Access Token / API Key)"), fontSize = 12.sp) },
                        placeholder = { Text(Translator.tr("اختياري — للنشر المباشر"), color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    if (linkVerifyMsg != null) {
                        Text(linkVerifyMsg!!, color = if (linkVerifyMsg!!.contains("✅")) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 12.sp, fontFamily = CairoFont)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLinking = true
                        coroutineScope.launch {
                            SocialAccountManager.toggleConnection(
                                context,
                                selectedPlatform.id,
                                isConnected = customHandle.isNotBlank(),
                                handle = customHandle.trim(),
                                apiToken = customToken.trim()
                            )
                            accountsList = SocialAccountManager.getAccounts(context)
                            isLinking = false
                            linkDialogState = null
                            Toast.makeText(context, "تم حفظ بيانات الحساب بنجاح ✅", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    enabled = !isLinking
                ) {
                    Text(Translator.tr("حفظ"), color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { linkDialogState = null }, enabled = !isLinking) {
                    Text(Translator.tr("إلغاء"), color = Color.Gray, fontFamily = CairoFont)
                }
            },
            containerColor = CardSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // حوار محرر القنوات الرسمية
    if (channelDraftId != null && canManageChannels) {
        val editingId = channelDraftId!!
        val isFixed = editingId in SocialAccountManager.FIXED_CHANNEL_IDS
        AlertDialog(
            onDismissRequest = { channelDraftId = null },
            containerColor = CardSurface,
            title = { Text(if (editingId.isBlank()) "إضافة رابط رسمي" else "تعديل الرابط", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isFixed) {
                        OutlinedTextField(value = chPlatform, onValueChange = { chPlatform = it }, label = { Text("اسم المنصة", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = chDisplay, onValueChange = { chDisplay = it }, label = { Text("الاسم المعروض", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    OutlinedTextField(value = chHandle, onValueChange = { chHandle = it }, label = { Text("المعرّف (handle)", fontSize = 11.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = chUrl, onValueChange = { chUrl = it; chUrlError = false },
                        label = { Text("الرابط https://...", fontSize = 11.sp) }, singleLine = true,
                        isError = chUrlError, modifier = Modifier.fillMaxWidth()
                    )
                    if (chUrlError) Text("أدخل رابطاً صالحاً يبدأ بـ http", color = Color(0xFFE53935), fontSize = 11.sp, fontFamily = CairoFont)
                    if (!isFixed) {
                        OutlinedTextField(value = chDesc, onValueChange = { chDesc = it }, label = { Text("الوصف (اختياري)", fontSize = 11.sp) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val url = chUrl.trim()
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            chUrlError = true
                            return@Button
                        }
                        if (editingId.isBlank()) {
                            val id = "custom_" + System.currentTimeMillis()
                            SocialAccountManager.saveCustomChannel(
                                context,
                                OfficialChannelInfo(
                                    id = id,
                                    platformName = chPlatform.trim().ifBlank { "رابط" },
                                    handle = chHandle.trim(),
                                    displayName = chDisplay.trim().ifBlank { chPlatform.trim().ifBlank { "رابط رسمي" } },
                                    description = chDesc.trim(),
                                    url = url,
                                    followersDisplay = "رسمي ✦"
                                )
                            )
                            Toast.makeText(context, "أُضيف الرابط ✅", Toast.LENGTH_SHORT).show()
                        } else if (isFixed) {
                            SocialAccountManager.updateOfficialChannel(context, editingId, chHandle, url)
                            Toast.makeText(context, "حُفظ التعديل ✅", Toast.LENGTH_SHORT).show()
                        } else {
                            val old = officialChannels.firstOrNull { it.id == editingId }
                            if (old != null) {
                                SocialAccountManager.saveCustomChannel(
                                    context,
                                    old.copy(
                                        platformName = chPlatform.trim().ifBlank { old.platformName },
                                        displayName = chDisplay.trim().ifBlank { old.displayName },
                                        handle = chHandle.trim(),
                                        description = chDesc.trim(),
                                        url = url
                                    )
                                )
                                Toast.makeText(context, "حُفظ التعديل ✅", Toast.LENGTH_SHORT).show()
                            }
                        }
                        channelDraftId = null
                        refreshChannels()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                ) { Text("حفظ", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { channelDraftId = null }) {
                    Text("إلغاء", color = Color.Gray, fontFamily = CairoFont)
                }
            }
        )
    }

    // التنقل بين الشاشات الفرعية المستقلة
    AnimatedContent(
        targetState = currentSubScreen,
        transitionSpec = {
            if (targetState != SettingsSubScreen.MAIN) {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith slideOutHorizontally { width -> width } + fadeOut()
            } else {
                slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
            }
        },
        label = "SettingsSubScreenAnimation"
    ) { subScreen ->
        when (subScreen) {
            // ==========================================
            // 🏠 الشاشة الرئيسية للإعدادات (Main Settings)
            // ==========================================
            SettingsSubScreen.MAIN -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    Translator.tr("الإعدادات الحسابية والفنية"),
                                    color = GoldPrimary,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = GoldPrimary)
                                }
                            },
                            actions = {
                                IconButton(onClick = onLogout) {
                                    Icon(
                                        if (isGuest) Icons.Default.Login else Icons.Default.Logout,
                                        contentDescription = "تسجيل",
                                        tint = if (isGuest) GoldPrimary else Color(0xFFEF4444)
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = qabasBackground())
                        )
                    },
                    bottomBar = bottomBar,
                    containerColor = qabasBackground()
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. بطاقة الملف الشخصي الفاخرة
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .luxuryCardStyle(shapeRadius = 20.dp, borderAlpha = 0.35f, glowElevation = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = qabasCardSurface())
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(54.dp),
                                    shape = CircleShape,
                                    color = Color(0xFF151B2B),
                                    border = BorderStroke(1.5.dp, GoldPrimary)
                                ) {
                                    if (avatarUriString.isNotBlank()) {
                                        AsyncImage(
                                            model = avatarUriString,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(GoldPrimary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (selectedPresetAvatar) {
                                                    1 -> Icons.Default.AutoAwesome
                                                    2 -> Icons.Default.MovieFilter
                                                    3 -> Icons.Default.MenuBook
                                                    4 -> Icons.Default.Psychology
                                                    else -> Icons.Default.Person
                                                },
                                                contentDescription = null,
                                                tint = GoldPrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = savedDisplayName,
                                        color = Color.White,
                                        fontFamily = CairoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = if (isGuest) "guest@qabas.studio" else userEmail,
                                        color = Color.Gray,
                                        fontFamily = NotoSansFont,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isAdmin || isRelative) GoldPrimary else Color(0xFF2D3748))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isAdmin -> Translator.tr("مطور النظام (صلاحيات كاملة) 👑")
                                                isRelative -> Translator.tr("حساب الاكتفاء الذاتي (مفاتيح خاصة) 🔑")
                                                isGuest -> Translator.tr("حساب مجاني 🏷️")
                                                else -> Translator.tr("عضوية ذهبية (Pro) ⭐")
                                            },
                                            color = if (isAdmin || isRelative) DeepSlate else Color.White,
                                            fontFamily = CairoFont,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (!isAdmin && !isGuest && !isRelative) {
                                    IconButton(onClick = onNavigateToPremiumUpgrade) {
                                        Icon(Icons.Default.WorkspacePremium, contentDescription = "ترقية", tint = GoldPrimary)
                                    }
                                }
                            }
                        }

                        // ── مجموعة 1: الإنتاج والمحتوى الذكي ──
                        SettingsGroupHeader("الإنتاج والمحتوى الذكي ✨")

                        SettingsNavigationCard(
                            title = "هوية الاستوديو والذكاء الاصطناعي",
                            subtitle = "نبرة الذكاء الاصطناعي، التفضيلات الفنية، وجدولة النشر",
                            icon = Icons.Default.Psychology,
                            accentColor = Color(0xFF8B5CF6),
                            onClick = { currentSubScreen = SettingsSubScreen.AI_IDENTITY }
                        )

                        SettingsNavigationCard(
                            title = "التفضيلات والجودة والتصدير",
                            subtitle = "المظهر الداكن/الفاتح، جودة 4K/1080p، وتسريع العتاد",
                            icon = Icons.Default.Tune,
                            accentColor = GoldPrimary,
                            onClick = { currentSubScreen = SettingsSubScreen.PREFERENCES_QUALITY }
                        )

                        SettingsNavigationCard(
                            title = "العلامة المائية والمونتاج والتصدير",
                            subtitle = "تخصيص الشعار، العلامة المائية، والمؤثرات",
                            icon = Icons.Default.VideoSettings,
                            badge = "صناعة المحتوى 🎬",
                            accentColor = Color(0xFF0EA5E9),
                            onClick = { currentSubScreen = SettingsSubScreen.STUDIO_WATERMARK }
                        )

                        // ── مجموعة 2: التجربة الروحية والقرآن الكريم ──
                        SettingsGroupHeader("التجربة الروحية والقرآن الكريم 🕋")

                        SettingsNavigationCard(
                            title = "تخصيص القراءة والمصحف الشريف",
                            subtitle = "الخطوط، حجم النص، التجويد، والوضع الليلي",
                            icon = Icons.Default.MenuBook,
                            badge = "مريح للعين 👁️",
                            accentColor = Color(0xFF10B981),
                            onClick = { currentSubScreen = SettingsSubScreen.READING_CUSTOMIZATION }
                        )

                        SettingsNavigationCard(
                            title = "الأذان ومواقيت الصلاة والأذكار",
                            subtitle = "حساب المواقيت، أصوات الأذان، وتنبيهات الأذكار",
                            icon = Icons.Default.Mosque,
                            badge = "زاد إيماني 🕋",
                            accentColor = Color(0xFFF59E0B),
                            onClick = { currentSubScreen = SettingsSubScreen.SPIRITUAL_PREFERENCES }
                        )

                        // ── مجموعة 3: التخزين والاتصال ──
                        SettingsGroupHeader("التخزين والاتصال 💾")

                        SettingsNavigationCard(
                            title = "التخزين، توفير البيانات والنسخ الاحتياطي",
                            subtitle = "تنظيف الكاش، توفير البيانات، والنسخ الاحتياطي",
                            icon = Icons.Default.CloudSync,
                            badge = "حماية ونقل 💾",
                            accentColor = Color(0xFF14B8A6),
                            onClick = { currentSubScreen = SettingsSubScreen.STORAGE_BACKUP }
                        )

                        SettingsNavigationCard(
                            title = "الحسابات الرسمية المعتمدة",
                            subtitle = "قنوات ومنصات قبس الرسمية على مختلف الشبكات",
                            icon = Icons.Default.Verified,
                            badge = "معتمدة ✦",
                            accentColor = GoldPrimary,
                            onClick = { currentSubScreen = SettingsSubScreen.OFFICIAL_CHANNELS }
                        )

                        SettingsNavigationCard(
                            title = "ربط المنصات الاجتماعية والنشر",
                            subtitle = "يوتيوب، تيك توك، انستغرام، وإكس",
                            icon = Icons.Default.Share,
                            accentColor = Color(0xFFEC4899),
                            onClick = { currentSubScreen = SettingsSubScreen.SOCIAL_PLATFORMS }
                        )

                        // ── مجموعة 4: الاستدامة والدعم والمعلومات ──
                        SettingsGroupHeader("الاستدامة والدعم والمعلومات 🌟")

                        SettingsNavigationCard(
                            title = "دعم المنصة واستدامة الخوادم ومجتمع واتساب",
                            subtitle = "كفالة الخوادم، مجتمع واتساب، والتقييم",
                            icon = Icons.Default.VolunteerActivism,
                            badge = "شريك استدامة 🌟",
                            accentColor = GoldPrimary,
                            onClick = { currentSubScreen = SettingsSubScreen.SUPPORT_CONTRIBUTION }
                        )

                        SettingsNavigationCard(
                            title = "حول تطبيق قبس والمعلومات",
                            subtitle = "الإصدار، التحديثات التلقائية، والضوابط الشرعية",
                            icon = Icons.Default.Info,
                            badge = "v${BuildConfig.VERSION_NAME}",
                            accentColor = Color(0xFF38BDF8),
                            onClick = { currentSubScreen = SettingsSubScreen.ABOUT_APP }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // زر تسجيل الخروج السريع
                        Button(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A24)),
                            border = BorderStroke(1.dp, if (isGuest) GoldPrimary else Color(0xFFEF4444)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(if (isGuest) Icons.Default.Login else Icons.Default.Logout, contentDescription = null, tint = if (isGuest) GoldPrimary else Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isGuest) Translator.tr("تسجيل الدخول / إنشاء حساب") else Translator.tr("تسجيل الخروج من الحساب"),
                                color = if (isGuest) GoldPrimary else Color(0xFFEF4444),
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            // ==========================================
            // 🧠 1. شاشة هوية الاستوديو والذكاء الاصطناعي
            // ==========================================
            SettingsSubScreen.AI_IDENTITY -> {
                SubScreenScaffold(
                    title = "هوية الاستوديو والذكاء الاصطناعي",
                    icon = Icons.Default.Psychology,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    bottomBar = bottomBar
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().luxuryCardStyle(shapeRadius = 18.dp),
                        colors = CardDefaults.cardColors(containerColor = qabasCardSurface()),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF141C27))
                                    .clickable { onNavigateToTasteProfile() }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(Translator.tr("هوية الاستوديو (Taste Engine)"), color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(Translator.tr("تنسيق أسلوب ونبرة الذكاء الاصطناعي وفقاً لأسلوبك الخاص"), color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
                                    }
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            }

                            HorizontalDivider(color = Color(0xFF222B3D))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(Translator.tr("تذكيرات مواعيد النشر"), color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(Translator.tr("إشعار عند حلول وقت النشر المثالي الذي جدولته"), color = Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
                                }
                                Switch(
                                    checked = autoPublishing,
                                    onCheckedChange = {
                                        autoPublishing = it
                                        prefs.edit().putBoolean("auto_publish", it).apply()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = GoldPrimary,
                                        checkedTrackColor = GoldPrimary.copy(alpha = 0.5f),
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = Color(0xFF333333)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OfflineAiSettingsSection()
                }
            }

            // ==========================================
            // ⚙️ 2. شاشة التفضيلات والمظهر والجودة والتصدير
            // ==========================================
            SettingsSubScreen.PREFERENCES_QUALITY -> {
                SubScreenScaffold(
                    title = "التفضيلات والجودة والتصدير",
                    icon = Icons.Default.Tune,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    bottomBar = bottomBar
                ) {
                    val isDarkTheme by ThemeManager.isDarkTheme.collectAsState()

                    Card(
                        modifier = Modifier.fillMaxWidth().luxuryCardStyle(shapeRadius = 18.dp),
                        colors = CardDefaults.cardColors(containerColor = qabasCardSurface()),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(Translator.tr("المظهر العام للتطبيق"), color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f).clickable { ThemeManager.setDarkTheme(context, true) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDarkTheme) GoldPrimary.copy(alpha = 0.2f) else Color(0xFF151B2B)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, if (isDarkTheme) GoldPrimary else Color(0xFF2A3040))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = if (isDarkTheme) GoldPrimary else Color.Gray, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(Translator.tr("داكن (Dark Luxury)"), color = if (isDarkTheme) GoldPrimary else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = CairoFont)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f).clickable { ThemeManager.setDarkTheme(context, false) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!isDarkTheme) GoldPrimary.copy(alpha = 0.2f) else Color(0xFF151B2B)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, if (!isDarkTheme) GoldPrimary else Color(0xFF2A3040))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.LightMode, contentDescription = null, tint = if (!isDarkTheme) GoldPrimary else Color.Gray, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(Translator.tr("فاتح (Light Mode)"), color = if (!isDarkTheme) GoldPrimary else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = CairoFont)
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFF222B3D))

                            Text(Translator.tr("جودة التصدير الافتراضية"), color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(
                                    Triple("720p", "HD سريع", "720p"),
                                    Triple("1080p", "FHD قياسي", "1080p"),
                                    Triple("4k", "4K سينمائي", "4k")
                                ).forEach { (id, label, key) ->
                                    val isSel = defaultQuality == key
                                    Card(
                                        modifier = Modifier.weight(1f).clickable {
                                            defaultQuality = key
                                            prefs.edit().putString("default_export_quality", key).apply()
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSel) GoldPrimary.copy(alpha = 0.2f) else Color(0xFF151B2B)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.5.dp, if (isSel) GoldPrimary else Color(0xFF2A3040))
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(id, color = if (isSel) GoldPrimary else Color.Gray, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = NotoSansFont)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(label, color = if (isSel) GoldPrimary else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = CairoFont)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    ApiServerSettingsSection()
                }
            }

            // ==========================================
            // 📖 3. شاشة تخصيص القراءة والمصحف الشريف
            // ==========================================
            SettingsSubScreen.READING_CUSTOMIZATION -> {
                SubScreenScaffold(
                    title = "تخصيص القراءة والمصحف الشريف",
                    icon = Icons.Default.MenuBook,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    bottomBar = bottomBar
                ) {
                    ReadingCustomizationSection()
                }
            }

            // ==========================================
            // 🕌 4. شاشة الأذان ومواقيت الصلاة والأذكار
            // ==========================================
            SettingsSubScreen.SPIRITUAL_PREFERENCES -> {
                SubScreenScaffold(
                    title = "الأذان ومواقيت الصلاة والأذكار",
                    icon = Icons.Default.Mosque,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    bottomBar = bottomBar
                ) {
                    SpiritualPreferencesSection()
                }
            }

            // ==========================================
            // 🎬 5. شاشة العلامة المائية والمونتاج
            // ==========================================
            SettingsSubScreen.STUDIO_WATERMARK -> {
                SubScreenScaffold(
                    title = "العلامة المائية والمونتاج والتصدير",
                    icon = Icons.Default.VideoSettings,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    bottomBar = bottomBar
                ) {
                    StudioWatermarkPreferencesSection()
                }
            }

            // ==========================================
            // 💾 6. شاشة التخزين والنسخ الاحتياطي
            // ==========================================
            SettingsSubScreen.STORAGE_BACKUP -> {
                SubScreenScaffold(
                    title = "التخزين، توفير البيانات والنسخ الاحتياطي",
                    icon = Icons.Default.CloudSync,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    bottomBar = bottomBar
                ) {
                    StorageAndBackupSection(
                        cacheSizeBytes = cacheSizeBytes,
                        onClearCache = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val freed = runCatching {
                                    var total = 0L
                                    context.cacheDir.walkTopDown().forEach { f ->
                                        if (f.isFile) {
                                            total += f.length()
                                            f.delete()
                                        }
                                    }
                                    total
                                }.getOrDefault(0L)
                                cacheSizeBytes = 0L
                                val msg = "تم تنظيف الملفات المؤقتة (${UpdateManager.formatSize(freed)})"
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }

            // ==========================================
            // ✦ 7. شاشة الحسابات الرسمية المعتمدة
            // ==========================================
            SettingsSubScreen.OFFICIAL_CHANNELS -> {
                SubScreenScaffold(
                    title = "الحسابات الرسمية المعتمدة",
                    icon = Icons.Default.Verified,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    bottomBar = bottomBar
                ) {
                    Text(
                        Translator.tr("تابع وتواصل مع منصات وقنوات قبس الرسمية على مختلف شبكات التواصل."),
                        color = Color.Gray,
                        fontFamily = CairoFont,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    officialChannels.forEachIndexed { index, channel ->
                        if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                        OfficialAccountItem(
                            channel = channel,
                            onOpen = {
                                SocialAccountManager.openOfficialChannel(context, channel.url)
                            },
                            onCopy = {
                                SocialAccountManager.copyToClipboard(context, channel.url, channel.platformName)
                            },
                            showDevActions = canManageChannels,
                            onEdit = { openChannelEditor(channel) },
                            onDelete = if (channel.id !in SocialAccountManager.FIXED_CHANNEL_IDS) {
                                {
                                    SocialAccountManager.deleteCustomChannel(context, channel.id)
                                    refreshChannels()
                                    Toast.makeText(context, "حُذف الرابط", Toast.LENGTH_SHORT).show()
                                }
                            } else null
                        )
                    }
                    if (canManageChannels) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { openChannelEditor(null) },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddLink, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة رابط رسمي (مطور)", color = GoldPrimary, fontFamily = CairoFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ==========================================
            // 🌐 8. شاشة ربط المنصات الاجتماعية والنشر
            // ==========================================
            SettingsSubScreen.SOCIAL_PLATFORMS -> {
                SubScreenScaffold(
                    title = "ربط المنصات الاجتماعية والنشر",
                    icon = Icons.Default.Share,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    bottomBar = bottomBar
                ) {
                    accountsList.forEachIndexed { index, account ->
                        if (index > 0) Spacer(modifier = Modifier.height(10.dp))
                        val verified = account.isConnected && SocialAccountManager.isConnectionVerified(context, account.id)
                        val statusText = when {
                            !account.isConnected -> Translator.tr("غير مربوط ⚪")
                            verified -> "${account.handle} (مربوط ✅)"
                            else -> "${account.handle} (غير مؤكد ⚠️)"
                        }
                        val icon = when (account.id) {
                            "youtube" -> Icons.Default.OndemandVideo
                            "tiktok" -> Icons.Default.MusicVideo
                            "instagram" -> Icons.Default.CameraAlt
                            "twitter" -> Icons.Default.AlternateEmail
                            else -> Icons.Default.Share
                        }
                        val iconColor = when (account.id) {
                            "youtube" -> Color(0xFFF44336)
                            "tiktok" -> Color.White
                            "instagram" -> Color(0xFFE1306C)
                            "twitter" -> Color(0xFF1DA1F2)
                            else -> GoldPrimary
                        }
                        SocialAccountItem(
                            platform = account.name,
                            status = statusText,
                            isLinked = account.isConnected,
                            icon = icon,
                            iconColor = iconColor
                        ) {
                            linkVerifyMsg = null
                            linkDialogState = account.id
                        }
                    }
                }
            }

            // ==========================================
            // 🌟 9. شاشة دعم المنصة ومجتمع واتساب (Support & Sustainability)
            // ==========================================
            SettingsSubScreen.SUPPORT_CONTRIBUTION -> {
                SubScreenScaffold(
                    title = "دعم المنصة واستدامة الخوادم",
                    icon = Icons.Default.VolunteerActivism,
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    bottomBar = bottomBar
                ) {
                    SupportContributionSection()
                }
            }

            // ==========================================
            // ℹ️ 10. شاشة حول تطبيق قبس والمعلومات والتحديثات
            // ==========================================
            SettingsSubScreen.ABOUT_APP -> {
                AboutUsAndVersionScreen(
                    onBack = { currentSubScreen = SettingsSubScreen.MAIN },
                    bottomBar = bottomBar
                )
            }
        }
    }

    // Dialogs
    if (showAboutAppDialog) {
        AlertDialog(
            onDismissRequest = { showAboutAppDialog = false },
            containerColor = CardSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("منظومة قبس (Qabas Studio)", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "﴿ إِذْ رَأَىٰ نَارًا فَقَالَ لِأَهْلِهِ امْكُثُوا إِنِّي آنَسْتُ نَارًا لَّعَلِّي آتِيكُم مِّنْهَا بِقَبَسٍ ﴾",
                        color = Color(0xFFF8FAFC),
                        fontFamily = AmiriFont,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "قبس هو استوديو متكامل في جيبك لصناعة المحتوى الدعوي والإسلامي الهادف بالذكاء الاصطناعي، يجمع بين تلاوة وتدبر القرآن، الصحيحين، الإخراج، والتعليق الصوتي.",
                        color = TextSecondary,
                        fontFamily = CairoFont,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutAppDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)) {
                    Text("إغلاق", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            containerColor = CardSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("سجل تحديثات قبس v${BuildConfig.VERSION_NAME}", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val savedNotes = prefs.getString("last_release_notes", "").orEmpty()
                    Text(savedNotes.ifBlank { "تحديثات مستمرة في الأداء والمظهر والاستقرار." }, color = TextSecondary, fontFamily = CairoFont, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showChangelogDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)) {
                    Text("رائع ✦", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor = CardSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("الضوابط الشرعية وسياسة الخصوصية", color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ميثاق النزاهة والمحتوى الهادف:", color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("1. يلتزم تطبيق قبس بضمان خلو المحتوى من أي مخالفات شرعية.", color = TextSecondary, fontFamily = CairoFont, fontSize = 12.sp)
                    Text("2. جميع التلاوات والأناشيد والمؤثرات الصوتية خالية من المعازف.", color = TextSecondary, fontFamily = CairoFont, fontSize = 12.sp)
                    Text("3. مفاتيح الـ API والبيانات تحفظ محلياً ومشفرة.", color = TextSecondary, fontFamily = CairoFont, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(onClick = { showPrivacyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)) {
                    Text("أوافق وملتزم", color = DeepSlate, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showPassphraseDialog) {
        AlertDialog(
            onDismissRequest = { showPassphraseDialog = false },
            containerColor = Color(0xFF0D1117),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = AiViolet, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تفعيل وضع المطور", color = AiViolet, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل كلمة المرور السرية لتفعيل وضع المطور:", color = Color.White, fontFamily = CairoFont, fontSize = 13.sp)
                    OutlinedTextField(
                        value = passphraseInput,
                        onValueChange = { passphraseInput = it; passphraseError = false },
                        placeholder = { Text("كلمة المرور", color = Color.Gray, fontFamily = CairoFont) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passphraseError) {
                        Text("كلمة المرور غير صحيحة", color = Color(0xFFF97316), fontFamily = CairoFont, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val devPrefs = context.getSharedPreferences("qabas_prefs", Context.MODE_PRIVATE)
                        val salt = "Qbs" + "::DevGate::" + "v1"
                        val digest = java.security.MessageDigest.getInstance("SHA-256")
                            .digest((passphraseInput + salt).toByteArray())
                            .joinToString("") { "%02x".format(it) }
                        val expected = "284308ac" + "cd46b74003578263" + "92758c1e0d189e543" + "43d3777156a550cbb123bfa"
                        if (digest == expected) {
                            devPrefs.edit().putBoolean("is_developer", true).apply()
                            isDevMode = true
                            showPassphraseDialog = false
                            passphraseInput = ""
                            Toast.makeText(context, "تم تفعيل وضع المطور! 🛠️", Toast.LENGTH_LONG).show()
                        } else {
                            passphraseError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AiViolet)
                ) {
                    Text("تفعيل", color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPassphraseDialog = false }) {
                    Text("إلغاء", color = Color.Gray, fontFamily = CairoFont)
                }
            }
        )
    }
}

/**
 * حاوية الشاشة الفرعية المستقلة (SubScreen Scaffold)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreenScaffold(
    title: String,
    icon: ImageVector,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = title,
                            color = GoldPrimary,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = GoldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = qabasBackground())
            )
        },
        bottomBar = bottomBar,
        containerColor = qabasBackground()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            content = content
        )
    }
}

/**
 * بطاقة انتقال فاخرة للشاشات الفرعية (Navigation Tile)
 */
@Composable
fun SettingsNavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String? = null,
    accentColor: Color = GoldPrimary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .luxuryCardStyle(shapeRadius = 18.dp, borderAlpha = 0.3f, glowElevation = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = qabasCardSurface())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontFamily = CairoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (badge != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = accentColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = badge,
                                    color = accentColor,
                                    fontFamily = CairoFont,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = qabasTextSecondary(),
                        fontFamily = NotoSansFont,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF131B2C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "فتح الإعداد",
                    tint = GoldPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        color = GoldPrimary,
        fontFamily = CairoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
fun SocialAccountItem(platform: String, status: String, isLinked: Boolean, icon: ImageVector, iconColor: Color, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141C27))
            .border(1.dp, if (isLinked) GoldPrimary.copy(alpha = 0.4f) else Color(0xFF222222), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isLinked) iconColor.copy(alpha = 0.15f) else Color(0xFF222222)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (isLinked) iconColor else Color.Gray, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(platform, color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(status, color = if (isLinked) GoldPrimary else Color.Gray, fontFamily = NotoSansFont, fontSize = 12.sp)
            }
        }
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
            Text(
                if (isLinked) Translator.tr("إلغاء الربط") else Translator.tr("ربط الحساب"),
                color = if (isLinked) Color(0xFFE53935) else GoldPrimary,
                fontFamily = CairoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun OfficialAccountItem(
    channel: OfficialChannelInfo,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    showDevActions: Boolean = false,
    onEdit: () -> Unit = {},
    onDelete: (() -> Unit)? = null
) {
    val (icon, brandColor) = when (channel.id) {
        "youtube" -> Icons.Default.OndemandVideo to Color(0xFFFF0000)
        "facebook" -> Icons.Default.ThumbUp to Color(0xFF1877F2)
        "instagram" -> Icons.Default.CameraAlt to Color(0xFFE1306C)
        "threads" -> Icons.Default.AlternateEmail to Color(0xFFE2E8F0)
        "tiktok" -> Icons.Default.MusicVideo to Color(0xFF00F2FE)
        else -> Icons.Default.Public to GoldPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141C27)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(brandColor.copy(alpha = 0.15f))
                            .border(1.dp, brandColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = brandColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                channel.displayName,
                                color = Color.White,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = GoldPrimary.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.8.dp, GoldPrimary.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    channel.badge,
                                    color = GoldPrimary,
                                    fontFamily = CairoFont,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            channel.handle,
                            color = GoldPrimary,
                            fontFamily = NotoSansFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (showDevActions) {
                        Row {
                            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = GoldPrimary, modifier = Modifier.size(16.dp))
                            }
                            if (onDelete != null) {
                                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                channel.description,
                color = Color.LightGray,
                fontFamily = CairoFont,
                fontSize = 11.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpen,
                    modifier = Modifier.weight(1.4f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("زيارة القناة / الصفحة", color = DeepSlate, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldPrimary),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("نسخ الرابط", color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp)
                }
            }
        }
    }
}
