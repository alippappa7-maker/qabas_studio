package com.qabas.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qabas.app.ui.theme.*
import com.qabas.app.ui.theme.CairoFont
import com.qabas.app.ui.theme.NotoSansFont
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    recentProjects: List<ProjectService.Project>,
    onNewProject: (String?) -> Unit,
    onViewProjects: () -> Unit,
    onViewSettings: () -> Unit,
    onViewProfile: () -> Unit = {},
    onViewNotifications: () -> Unit = {},
    onOpenProject: (ProjectService.Project) -> Unit,
    onViewApiDocs: () -> Unit = {},
    onTeleprompter: () -> Unit = {},
    onAudioLibrary: () -> Unit = {},
    onAiAssistant: () -> Unit = {},
    onYouTubeStudio: () -> Unit = {},
    onDeveloperDashboard: () -> Unit = {},
    onReels: () -> Unit = {},
    onLeaderboard: () -> Unit = {},
    onNavigateToTasteProfile: () -> Unit = {},
    onStudioGateway: () -> Unit = {},
    onPremiumUpgrade: () -> Unit = {},
    onRewards: () -> Unit = {},
    onKnowledgeHub: () -> Unit = {},
    onContentGuard: () -> Unit = {},
    onQuranHub: () -> Unit = {},
    onHadithStudio: () -> Unit = {},
    onVideoStyleCloner: () -> Unit = {},
    onPhotoStudio: () -> Unit = {},
    onStyleSelection: () -> Unit = {},
    onIslamicLibrary: () -> Unit = {},
    onAzkar: () -> Unit = {},
    onPrayerTimes: () -> Unit = {},
    onScholarBiographies: () -> Unit = {},
    onProEditor: () -> Unit = {},
    onRestoreWorkspaceDraft: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {}
) {
    val analyticsContext = LocalContext.current
    val prefs = analyticsContext.getSharedPreferences("qabas_prefs", android.content.Context.MODE_PRIVATE)
    val isAdmin = prefs.getBoolean("is_admin", false)
    val autoSaveStatus by WorkspaceAutoSaveManager.status.collectAsState()

    val hasCompletedTutorial = remember { prefs.getBoolean("has_completed_guided_tutorial", false) }
    var showGuidedTutorial by remember { mutableStateOf(!hasCompletedTutorial) }

    LaunchedEffect(Unit) { AppServices.getAnalyticsService(analyticsContext).logScreenView("StudioDashboard") }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) break
            ctx = ctx.baseContext
        }
        ctx as? android.app.Activity
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            showExitDialog = true
        }
    }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var isAnalyzingVoice by remember { mutableStateOf(false) }
    var voiceIdeaManualText by remember { mutableStateOf("") }
    val recorderHelper = remember { AudioRecorderHelper(context) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val ok = recorderHelper.startRecording()
            if (ok) {
                isRecordingVoice = true
                voiceIdeaManualText = ""
                Toast.makeText(context, Translator.tr("بدأ التسجيل... تحدث بفكرتك الدعوية الآن 🎙️"), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, Translator.tr("تعذر بدء الميكروفون"), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, Translator.tr("يلزم السماح بالميكروفون للتسجيل الصوتي"), Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorderHelper.release()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    var showSuspendedDialog by remember { mutableStateOf(false) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    val accountService = remember { AppServices.getAccountService(analyticsContext) }
    val handlePremiumAction: (() -> Unit) -> Unit = { action ->
        action()
    }
    val techGradient = remember { Brush.horizontalGradient(colors = listOf(AiCyan, AiViolet)) }

    var selectedCategoryTab by remember { mutableStateOf(0) }
    var showAutoSeriesDialog by remember { mutableStateOf(false) }

    var isStudioEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isStudioEntered = true
    }
    val studioEntryAlpha by animateFloatAsState(
        targetValue = if (isStudioEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "studioEntryAlpha"
    )
    val studioEntryScale by animateFloatAsState(
        targetValue = if (isStudioEntered) 1f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "studioEntryScale"
    )

    LaunchedEffect(Unit) {
        StyleBrain.init(context)
    }
    val currentCoreStyle by StyleBrain.coreStyle.collectAsState()
    val absorbedStylesList by StyleBrain.absorbedStyles.collectAsState()

    AppPermissionsEntryLauncher()

    var showPlaylistSheet by remember { mutableStateOf(false) }
    val currentAudioTrack by AudioPlaybackManager.currentTrack.collectAsState()
    val isAudioPlaying by AudioPlaybackManager.isPlaying.collectAsState()

    val avatarUriString = remember(prefs) { prefs.getString("user_avatar_uri", "") ?: "" }
    val selectedPresetAvatar = remember(prefs) { prefs.getInt("user_avatar_preset", 0) }

    if (showPlaylistSheet) {
        AudioPlaylistBottomSheet(
            onDismiss = { showPlaylistSheet = false }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(Translator.tr("تأكيد الخروج"), color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold) },
            text = { Text(Translator.tr("هل أنت متأكد أنك تريد الخروج من التطبيق؟"), color = Color.White, fontFamily = CairoFont) },
            confirmButton = {
                TextButton(onClick = {
                    activity?.finishAffinity() ?: activity?.finish()
                }) {
                    Text(Translator.tr("نعم"), color = Color(0xFFE53935), fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(Translator.tr("إلغاء"), color = Color.White, fontFamily = CairoFont)
                }
            },
            containerColor = Color(0xFF151B2B)
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DeepSlate,
                drawerContentColor = Color.White,
                modifier = Modifier.widthIn(max = 330.dp)
            ) {
                QabasNavigationDrawerContent(
                    isAdmin = isAdmin,
                    onCloseDrawer = { scope.launch { drawerState.close() } },
                    onNavigateTo = { action ->
                        scope.launch {
                            drawerState.close()
                            action()
                        }
                    },
                    onStudioGateway = onStudioGateway,
                    onProEditor = onProEditor,
                    onPhotoStudio = onPhotoStudio,
                    onTeleprompter = onTeleprompter,
                    onVideoStyleCloner = onVideoStyleCloner,
                    onYouTubeStudio = onYouTubeStudio,
                    onViewProjects = onViewProjects,
                    onQuranHub = onQuranHub,
                    onIslamicLibrary = onIslamicLibrary,
                    onAzkar = onAzkar,
                    onScholarBiographies = onScholarBiographies,
                    onPrayerTimes = onPrayerTimes,
                    onAudioLibrary = onAudioLibrary,
                    onReels = onReels,
                    onAiAssistant = onAiAssistant,
                    onContentGuard = onContentGuard,
                    onLeaderboard = onLeaderboard,
                    onNavigateToTasteProfile = onNavigateToTasteProfile,
                    onPremiumUpgrade = onPremiumUpgrade,
                    onViewProfile = onViewProfile,
                    onViewSettings = onViewSettings,
                    onViewApiDocs = onViewApiDocs,
                    onDeveloperDashboard = onDeveloperDashboard
                )
            }
        }
    ) {
        Scaffold(
            containerColor = qabasBackground(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column {
                    // Global Floating Mini Player Bar if media is playing
                    AnimatedVisibility(
                        visible = currentAudioTrack != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        currentAudioTrack?.let { track ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .clickable { showPlaylistSheet = true },
                                shape = RoundedCornerShape(16.dp),
                                color = qabasCardSurface(),
                                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                                shadowElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(GoldPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.title,
                                            color = qabasTextPrimary(),
                                            fontFamily = CairoFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${track.artist} • اضغط للتحكم والقائمة",
                                            color = EmeraldGreen,
                                            fontFamily = CairoFont,
                                            fontSize = 11.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { AudioPlaybackManager.playPause(analyticsContext) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            if (isAudioPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                            contentDescription = "تشغيل / إيقاف",
                                            tint = GoldPrimary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showPlaylistSheet = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.QueueMusic,
                                            contentDescription = "قائمة التشغيل",
                                            tint = qabasTextSecondary(),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    bottomBar()
                }
            },
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = Translator.tr("القائمة الجانبية"),
                                tint = GoldPrimary
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(techGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.MovieFilter, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                Translator.tr("استوديو قبس"),
                                color = AiCyanLight,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                        }
                    },
                actions = {
                    val notifCount = AppNotificationService.getNotifications(analyticsContext).size
                    val isDarkTheme by ThemeManager.isDarkTheme.collectAsState()
                    IconButton(onClick = { ThemeManager.toggleTheme(analyticsContext) }) {
                        Icon(
                            if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = Translator.tr("تبديل المظهر"),
                            tint = if (isDarkTheme) GoldPrimary else AiCyan
                        )
                    }
                    IconButton(onClick = onViewNotifications) {
                        BadgedBox(
                            badge = { if (notifCount > 0) Badge { Text("$notifCount") } }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "الإشعارات", tint = if (notifCount > 0) GoldPrimary else qabasTextSecondary())
                        }
                    }
                    IconButton(onClick = onLeaderboard) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = Translator.tr("لوحة الشرف"), tint = qabasTextSecondary())
                    }
                    IconButton(onClick = onViewSettings) {
                        Icon(Icons.Default.Settings, contentDescription = Translator.tr("الإعدادات"), tint = qabasTextSecondary())
                    }
                    IconButton(
                        onClick = onViewProfile,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            color = qabasCardSurface(),
                            border = BorderStroke(1.5.dp, GoldPrimary)
                        ) {
                            if (avatarUriString.isNotBlank()) {
                                AsyncImage(
                                    model = avatarUriString,
                                    contentDescription = Translator.tr("الملف الشخصي"),
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(GoldPrimary.copy(alpha = 0.2f)),
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
                                        contentDescription = Translator.tr("الملف الشخصي"),
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = qabasBackground())
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Workspace Auto-Save Recovery Banner
            if (autoSaveStatus.hasRecoverableDraft && onRestoreWorkspaceDraft != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF151B2B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .bouncingClickable { onRestoreWorkspaceDraft() }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(GoldPrimary.copy(alpha = 0.12f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(GoldPrimary.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "مسودة عمل محفوظة تلقائياً في Room 💾",
                                        color = GoldPrimary,
                                        fontFamily = CairoFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = autoSaveStatus.draftSnippet.ifBlank { "استعد آخر تعديلات الفيديو التي قمت بها" },
                                        color = TextSecondary,
                                        fontFamily = NotoSansFont,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = { onRestoreWorkspaceDraft() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = GoldPrimary)
                                ) {
                                    Text("استعادة", fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                IconButton(
                                    onClick = { scope.launch { WorkspaceAutoSaveManager.clearAiDraft() } },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "تجاهل", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 1. Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .scale(studioEntryScale)
                    .alpha(studioEntryAlpha)
                    .luxuryCardStyle(shapeRadius = 24.dp, borderAlpha = 0.35f, glowElevation = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF162032), Color(0xFF0B0F19))
                        )
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bg_hero_pattern),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.35f
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val readinessReport = remember { OperationalReadinessManager.calculateReadiness(analyticsContext) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onViewApiDocs,
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF151B2B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.bouncingClickable(onClick = onViewApiDocs)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (readinessReport.percentage >= 80) Icons.Default.CheckCircle else Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = if (readinessReport.percentage >= 80) Color(0xFF10B981) else GoldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "الجاهزية: ${readinessReport.percentage}%",
                                    color = GoldPrimary,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                onClick = onRewards,
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF151B2B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.bouncingClickable(onClick = onRewards)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.CardGiftcard, contentDescription = "المكافآت", tint = GoldPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("المكافآت", color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp)
                                }
                            }

                            Surface(
                                onClick = { showGuidedTutorial = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF151B2B),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.bouncingClickable(onClick = { showGuidedTutorial = true })
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.HelpOutline, contentDescription = "دليل", tint = GoldPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("دليل", color = Color.White, fontFamily = NotoSansFont, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                Translator.tr("« ادْعُ إِلَىٰ سَبِيلِ رَبِّكَ بِالْحِكْمَةِ وَالْمَوْعِظَةِ الْحَسَنَةِ »"),
                                color = GoldPrimary,
                                fontSize = 17.sp,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 25.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                Translator.tr("اجعل نيتك خالصة، وابدأ في صناعة محتوى يترك أثراً طيباً."),
                                color = Color.White.copy(alpha = 0.85f),
                                fontFamily = NotoSansFont,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Lottie Studio Entry Portal Animation
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(GoldPrimary.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            LottieStudioEntryHero(size = 76.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                handlePremiumAction {
                                    onStudioGateway()
                                }
                            },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .shadow(6.dp, RoundedCornerShape(14.dp), spotColor = GoldPrimary.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DeepSlate, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Translator.tr("استوديو الإنتاج"), color = DeepSlate, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Button(
                            onClick = { handlePremiumAction { onReels() } },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .border(1.5.dp, GoldPrimary, RoundedCornerShape(14.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Translator.tr("ريلز فوري"), color = GoldPrimary, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Button(
                            onClick = { handlePremiumAction { onStudioGateway() } },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(Translator.tr("مشروع جديد"), color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        onClick = { handlePremiumAction { onVideoStyleCloner() } },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF151B2B),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(GoldPrimary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                Translator.tr("مستنسخ الفيديوهات وStyleBrain 🪄"),
                                                color = GoldPrimary,
                                                fontFamily = CairoFont,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = GoldPrimary,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    if (absorbedStylesList.isNotEmpty()) "${absorbedStylesList.size} ${Translator.tr("أساليب")}" else Translator.tr("ذكي"),
                                                    color = DeepSlate,
                                                    fontFamily = CairoFont,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            Translator.tr("تفكيك وامتصاص هوية أي فيديو وإضافتها لعقلك الإخراجي التراكمي."),
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontFamily = NotoSansFont,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0B0F19),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "${Translator.tr("الهوية المعتمدة:")} ${currentCoreStyle.visualTraits.firstOrNull() ?: Translator.tr("قبس سينمائي")}",
                                            color = TextSecondary,
                                            fontFamily = CairoFont,
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        "${Translator.tr("قوة الأسلوب:")} ${currentCoreStyle.strengthScore}%",
                                        color = GoldSecondary,
                                        fontFamily = NotoSansFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. أحدث المشاريع أولاً (حسب طلبك)
            SectionContainer(
                title = Translator.tr("أحدث المشاريع الدعوية"),
                subtitle = Translator.tr("مشاريعك المحفوظة مؤخراً"),
                icon = Icons.Default.FolderSpecial,
                actionText = Translator.tr("عرض الكل"),
                onActionClick = onViewProjects,
                sectionKey = "recent_projects",
                defaultExpanded = true
            ) {
                if (recentProjects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0B0F19))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MovieFilter, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(Translator.tr("لا توجد مشاريع سابقة"), color = Color.White, fontFamily = NotoSansFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(Translator.tr("ابدأ مشروعك الهادف الأول الآن"), color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(
                            items = recentProjects.take(5),
                            key = { project -> project.id.ifEmpty { project.title } }
                        ) { project ->
                            val formattedDate = if (project.updatedAt > 0) {
                                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(project.updatedAt))
                            } else {
                                Translator.tr("مسودة حديثة")
                            }
                            val statusLabel = project.status.ifBlank { Translator.tr("مسودة") }
                            RecentProjectCard(
                                title = project.title.ifBlank { Translator.tr("مشروع بدون عنوان") },
                                date = formattedDate,
                                duration = statusLabel,
                                onClick = { onOpenProject(project) }
                            )
                        }
                    }
                }
            }

            // 3. Category Tabs
            val categoryTabs = remember {
                listOf(
                    "🌟 " + Translator.tr("الكل"),
                    "📖 " + Translator.tr("الاستوديوهات"),
                    "🛠️ " + Translator.tr("أدوات الإنتاج"),
                    "⚡ " + Translator.tr("المهمات والقوالب")
                )
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    count = categoryTabs.size,
                    key = { index -> categoryTabs[index] }
                ) { index ->
                    val isSelected = selectedCategoryTab == index
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryTab = index },
                        label = {
                            Text(
                                categoryTabs[index],
                                fontFamily = CairoFont,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isSelected) DeepSlate else Color.White
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldPrimary,
                            containerColor = Color(0xFF151B2B)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) GoldPrimary else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 4. الاستوديوهات (مطوية افتراضياً)
            if (selectedCategoryTab == 0 || selectedCategoryTab == 1) {
                SectionContainer(
                    title = Translator.tr("مراكز الإنتاج الرئيسية"),
                    subtitle = Translator.tr("استوديوهات دعوية وقرآنية متكاملة"),
                    icon = Icons.Default.AutoAwesome,
                    sectionKey = "main_studios",
                    defaultExpanded = false
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        StudioTileItem(
                            title = Translator.tr("استوديو الصور (سريع جداً) ✨"),
                            desc = Translator.tr("حول صورك لفيديو إسلامي (بدون موسيقى) في 3 خطوات"),
                            icon = Icons.Default.PhotoLibrary,
                            accentColor = Color(0xFF10B981),
                            onClick = { handlePremiumAction { onPhotoStudio() } }
                        )
                        StudioTileItem(
                            title = Translator.tr("طريق الحسنات وموسوعة الخير 🌟"),
                            desc = Translator.tr("القرآن الكريم، الأذكار، التفاسير، وسيرة الأكابر من علماء أهل السنة"),
                            icon = Icons.Default.AutoAwesome,
                            accentColor = GoldPrimary,
                            onClick = { handlePremiumAction { onQuranHub() } }
                        )
                        StudioTileItem(
                            title = Translator.tr("استوديو بطاقات الحديث الشريف 📜"),
                            desc = Translator.tr("تخريج وتصحيح وتصميم بطاقات دعوية سينمائية بالذكاء الاصطناعي"),
                            icon = Icons.Default.FormatQuote,
                            accentColor = Color(0xFFE2E8F0),
                            onClick = { handlePremiumAction { onHadithStudio() } }
                        )
                        StudioTileItem(
                            title = Translator.tr("استوديو اليوتيوب والدروس الطويلة 🔴"),
                            desc = Translator.tr("مونتاج الفيديوهات الطويلة (فواصل زمنية، إزالة الصمت، تحسين الصوت)"),
                            icon = Icons.Default.PlayCircle,
                            accentColor = Color(0xFFEF4444),
                            onClick = { handlePremiumAction { onYouTubeStudio() } }
                        )
                        StudioTileItem(
                            title = Translator.tr("محرك السلاسل الدعوية الآلي 🚀"),
                            desc = Translator.tr("توليد سلاسل حلقات مترابطة (حتى 10 حلقات) مع خطاف وختام وجدولة آلية"),
                            icon = Icons.Default.ViewTimeline,
                            accentColor = GoldPrimary,
                            onClick = { handlePremiumAction { showAutoSeriesDialog = true } }
                        )
                    }
                }
            }

            // 5. مهمة الأسبوع (مطوية)
            if (selectedCategoryTab == 0 || selectedCategoryTab == 3) {
                val currentMission = remember { PointsManager.getCurrentWeeklyMission() }
                var isCompleted by remember { mutableStateOf(PointsManager.isWeeklyMissionCompleted(context, currentMission.id)) }

                SectionContainer(
                    title = Translator.tr("مهمة الأسبوع الطارئة 🚨"),
                    subtitle = if (isCompleted) Translator.tr("تم إنجاز التحدي وكسب النقاط 🎉") else Translator.tr("تحدي الأسبوع لكسب +${currentMission.points} نقطة"),
                    icon = Icons.Default.LocalPolice,
                    sectionKey = "weekly_mission",
                    defaultExpanded = false
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B0F19), RoundedCornerShape(14.dp))
                            .border(1.dp, if (isCompleted) Color(0xFF10B981) else GoldPrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isCompleted) Color(0xFF10B981) else GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "${Translator.tr("الموضوع:")} ${currentMission.topic}",
                                    color = if (isCompleted) Color(0xFF10B981) else GoldPrimary,
                                    fontFamily = CairoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isCompleted) Color(0xFF10B981).copy(alpha = 0.2f) else GoldPrimary.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    if (isCompleted) Translator.tr("مكتملة ✓") else "+${currentMission.points} ${Translator.tr("نقطة")}",
                                    color = if (isCompleted) Color(0xFF10B981) else GoldPrimary,
                                    fontFamily = NotoSansFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            currentMission.description,
                            color = Color.White.copy(alpha = 0.85f),
                            fontFamily = NotoSansFont,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                handlePremiumAction {
                                    if (!isCompleted) {
                                        PointsManager.markWeeklyMissionCompleted(context, currentMission.id)
                                        isCompleted = true
                                        Toast.makeText(context, Translator.tr("تم تسجيل انطلاقك في التحدي وإضافة +${currentMission.points} نقطة لرصيدك!"), Toast.LENGTH_LONG).show()
                                    }
                                    onNewProject(currentMission.prompt)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompleted) Color(0xFF10B981) else GoldPrimary
                            )
                        ) {
                            Icon(
                                if (isCompleted) Icons.Default.Check else Icons.Default.AddTask,
                                contentDescription = null,
                                tint = DeepSlate,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isCompleted) Translator.tr("إنشاء فيديو إضافي للمهمة") else Translator.tr("انطلق في المهمة واكسب النقاط"),
                                color = DeepSlate,
                                fontFamily = NotoSansFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 6. أدوات الإنتاج (مطوية)
            if (selectedCategoryTab == 0 || selectedCategoryTab == 2) {
                SectionContainer(
                    title = Translator.tr("أدوات الإنتاج المساعدة"),
                    subtitle = Translator.tr("مجموعة المساعدات المتقدمة للمونتاج والإعداد"),
                    icon = Icons.Default.BuildCircle,
                    sectionKey = "production_tools",
                    defaultExpanded = false
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProToolCompactCard(title = Translator.tr("استوديو الريلز"), desc = Translator.tr("صناعة سريعة"), icon = Icons.Default.Movie, tintColor = Color(0xFFE11D48), modifier = Modifier.weight(1f), onClick = onReels)
                            ProToolCompactCard(title = Translator.tr("المُلقن الذكي"), desc = Translator.tr("سجل بثقة"), icon = Icons.Default.ClosedCaption, tintColor = Color(0xFF34D399), modifier = Modifier.weight(1f), onClick = onTeleprompter)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProToolCompactCard(title = Translator.tr("المكتبة الصوتية"), desc = Translator.tr("أصوات إسلامية"), icon = Icons.Default.LibraryMusic, tintColor = Color(0xFF8B5CF6), modifier = Modifier.weight(1f), onClick = onAudioLibrary)
                            ProToolCompactCard(title = Translator.tr("المساعد الدعوي"), desc = Translator.tr("أفكار وخطط"), icon = Icons.Default.SupportAgent, tintColor = AiCyan, modifier = Modifier.weight(1f), onClick = onAiAssistant)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProToolCompactCard(title = Translator.tr("استوديو التجويد"), desc = Translator.tr("تلاوات ومصاحف"), icon = Icons.Default.MenuBook, tintColor = Color(0xFF10B981), modifier = Modifier.weight(1f), onClick = onQuranHub)
                            ProToolCompactCard(title = Translator.tr("ذوق صانع المحتوى"), desc = Translator.tr("تخصيص الهوية"), icon = Icons.Default.Psychology, tintColor = Color(0xFF6366F1), modifier = Modifier.weight(1f), onClick = onNavigateToTasteProfile)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProToolCompactCard(title = Translator.tr("مختبر صهر الأنماط"), desc = Translator.tr("صهر وتوليد الماستر"), icon = Icons.Default.Style, tintColor = Color(0xFFF59E0B), modifier = Modifier.weight(1f), onClick = onStyleSelection)
                            ProToolCompactCard(title = Translator.tr("مستنسخ الأسلوب"), desc = Translator.tr("تحليل الفيديوهات"), icon = Icons.Default.AutoFixHigh, tintColor = Color(0xFFA78BFA), modifier = Modifier.weight(1f), onClick = onVideoStyleCloner)
                        }
                    }
                }
            }

            // 7. قوالب سريعة (مطوية)
            if (selectedCategoryTab == 0 || selectedCategoryTab == 3) {
                SectionContainer(
                    title = Translator.tr("قوالب الانطلاق السريع"),
                    subtitle = Translator.tr("ابدأ مباشرة بقوالب جاهزة"),
                    icon = Icons.Default.FlashOn,
                    sectionKey = "quick_templates",
                    defaultExpanded = false
                ) {
                    val templates = listOf(
                        Triple(Translator.tr("قصص الأنبياء"), Icons.AutoMirrored.Filled.MenuBook, GoldPrimary),
                        Triple(Translator.tr("تدبر آية"), Icons.Default.Lightbulb, Color(0xFF38BDF8)),
                        Triple(Translator.tr("تزكية نفس"), Icons.Default.SelfImprovement, Color(0xFF34D399)),
                        Triple(Translator.tr("أحاديث نبوية"), Icons.Default.HistoryEdu, Color(0xFFF59E0B))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickStartCard(title = templates[0].first, icon = templates[0].second, accentColor = templates[0].third, onClick = { handlePremiumAction { onNewProject(templates[0].first) } }, modifier = Modifier.weight(1f))
                            QuickStartCard(title = templates[1].first, icon = templates[1].second, accentColor = templates[1].third, onClick = { handlePremiumAction { onNewProject(templates[1].first) } }, modifier = Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickStartCard(title = templates[2].first, icon = templates[2].second, accentColor = templates[2].third, onClick = { handlePremiumAction { onNewProject(templates[2].first) } }, modifier = Modifier.weight(1f))
                            QuickStartCard(title = templates[3].first, icon = templates[3].second, accentColor = templates[3].third, onClick = { handlePremiumAction { onNewProject(templates[3].first) } }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 8. لوحة المطور
            if (isAdmin && (selectedCategoryTab == 0 || selectedCategoryTab == 2)) {
                SectionContainer(
                    title = Translator.tr("لوحة تحكم المطور"),
                    subtitle = Translator.tr("إدارة المنصة والإحصائيات"),
                    icon = Icons.Default.Analytics,
                    sectionKey = "developer_dashboard",
                    defaultExpanded = false
                ) {
                    ProToolCompactCard(
                        title = Translator.tr("الإحصائيات والتحكم"),
                        desc = Translator.tr("إدارة السحابة والمفاتيح"),
                        icon = Icons.Default.DeveloperMode,
                        tintColor = Color(0xFFEF4444),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onDeveloperDashboard
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    }

    // ========== Dialogs ==========

    if (isRecordingVoice) {
        AlertDialog(
            onDismissRequest = {
                if (!isAnalyzingVoice) {
                    recorderHelper.stopRecording()
                    isRecordingVoice = false
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(if (isRecordingVoice) pulseScale else 1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isAnalyzingVoice) Translator.tr("جاري التحليل...") else Translator.tr("جاري التسجيل..."),
                        color = GoldPrimary,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        Translator.tr("تحدث بفكرتك الدعوية بوضوح. اضغط إيقاف عند الانتهاء."),
                        color = Color.White,
                        fontFamily = CairoFont
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = voiceIdeaManualText,
                        onValueChange = { voiceIdeaManualText = it },
                        label = { Text(Translator.tr("أو اكتب الفكرة يدوياً"), color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isAnalyzingVoice) {
                            isAnalyzingVoice = true
                            scope.launch {
                                val path = recorderHelper.stopRecording()
                                // هنا يمكن إضافة منطق التحليل
                                isAnalyzingVoice = false
                                isRecordingVoice = false
                                if (voiceIdeaManualText.isNotBlank()) {
                                    onNewProject(voiceIdeaManualText)
                                } else if (path != null) {
                                    onNewProject(null)
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        if (isAnalyzingVoice) Translator.tr("جاري...") else Translator.tr("إيقاف وبدء"),
                        color = GoldPrimary,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (!isAnalyzingVoice) {
                            recorderHelper.stopRecording()
                            isRecordingVoice = false
                        }
                    }
                ) {
                    Text(Translator.tr("إلغاء"), color = Color.White, fontFamily = CairoFont)
                }
            },
            containerColor = Color(0xFF151B2B)
        )
    }

    if (showLimitDialog) {
        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text(Translator.tr("تم الوصول للحد"), color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold) },
            text = { Text(Translator.tr("لقد استنفدت الحد اليومي. قم بالترقية للمتابعة."), color = Color.White, fontFamily = CairoFont) },
            confirmButton = {
                TextButton(onClick = {
                    showLimitDialog = false
                    onPremiumUpgrade()
                }) {
                    Text(Translator.tr("ترقية"), color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLimitDialog = false }) {
                    Text(Translator.tr("لاحقاً"), color = Color.White, fontFamily = CairoFont)
                }
            },
            containerColor = Color(0xFF151B2B)
        )
    }

    if (showSuspendedDialog) {
        AlertDialog(
            onDismissRequest = { showSuspendedDialog = false },
            title = { Text(Translator.tr("الحساب موقوف"), color = Color(0xFFE53935), fontFamily = CairoFont, fontWeight = FontWeight.Bold) },
            text = { Text(Translator.tr("حسابك موقوف حالياً. تواصل مع الدعم."), color = Color.White, fontFamily = CairoFont) },
            confirmButton = {
                TextButton(onClick = { showSuspendedDialog = false }) {
                    Text(Translator.tr("حسناً"), color = Color.White, fontFamily = CairoFont)
                }
            },
            containerColor = Color(0xFF151B2B)
        )
    }

    if (showMaintenanceDialog) {
        AlertDialog(
            onDismissRequest = { showMaintenanceDialog = false },
            title = { Text(Translator.tr("صيانة"), color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold) },
            text = { Text(Translator.tr("الخدمة تحت الصيانة حالياً. حاول لاحقاً."), color = Color.White, fontFamily = CairoFont) },
            confirmButton = {
                TextButton(onClick = { showMaintenanceDialog = false }) {
                    Text(Translator.tr("حسناً"), color = Color.White, fontFamily = CairoFont)
                }
            },
            containerColor = Color(0xFF151B2B)
        )
    }
}

// ==================== Helper Composables ====================

@Composable
fun SectionContainer(
    title: String,
    subtitle: String = "",
    icon: ImageVector,
    sectionKey: String = "",
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    defaultExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "sectionArrowRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF151B2B),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (expanded) GoldPrimary.copy(alpha = 0.45f) else GoldPrimary.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth().bouncingClickable { expanded = !expanded }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                GoldPrimary.copy(alpha = if (expanded) 0.08f else 0.03f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GoldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(title, color = GoldPrimary, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            if (subtitle.isNotBlank()) {
                                Text(subtitle, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 11.sp)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (actionText != null && onActionClick != null) {
                            TextButton(onClick = onActionClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text(actionText, color = GoldSecondary, fontFamily = CairoFont, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer(rotationZ = arrowRotation)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(240)),
            exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(180))
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ProToolCompactCard(
    title: String,
    desc: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tintColor: Color = GoldPrimary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF151B2B),
        border = androidx.compose.foundation.BorderStroke(1.dp, tintColor.copy(alpha = 0.28f)),
        modifier = modifier.bouncingClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tintColor.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(13.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(tintColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tintColor, modifier = Modifier.size(23.dp))
                }
                Spacer(modifier = Modifier.height(9.dp))
                Text(title, color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(desc, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 10.5.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun RecentProjectCard(
    title: String,
    date: String,
    duration: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF151B2B),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.35f)),
        modifier = Modifier
            .width(165.dp)
            .bouncingClickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MovieFilter,
                    contentDescription = null,
                    tint = GoldPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(30.dp)
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                ) {
                    Text(
                        duration,
                        color = GoldSecondary,
                        fontFamily = NotoSansFont,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(9.dp))
            Text(title, color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(date, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 10.sp)
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun StudioTileItem(
    title: String,
    desc: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF151B2B),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth().bouncingClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(desc, color = TextSecondary, fontFamily = NotoSansFont, fontSize = 12.sp, maxLines = 2)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun QuickStartCard(
    title: String,
    icon: ImageVector,
    accentColor: Color = GoldPrimary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF151B2B),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
        modifier = modifier.bouncingClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(13.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(title, color = Color.White, fontFamily = CairoFont, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
            }
        }
    }
}

// ==================== Navigation Drawer Content ====================

@Composable
fun QabasNavigationDrawerContent(
    isAdmin: Boolean,
    onCloseDrawer: () -> Unit,
    onNavigateTo: (() -> Unit) -> Unit,
    onStudioGateway: () -> Unit,
    onProEditor: () -> Unit,
    onPhotoStudio: () -> Unit,
    onTeleprompter: () -> Unit,
    onVideoStyleCloner: () -> Unit,
    onYouTubeStudio: () -> Unit,
    onViewProjects: () -> Unit,
    onQuranHub: () -> Unit,
    onIslamicLibrary: () -> Unit,
    onAzkar: () -> Unit,
    onScholarBiographies: () -> Unit,
    onPrayerTimes: () -> Unit,
    onAudioLibrary: () -> Unit,
    onReels: () -> Unit,
    onAiAssistant: () -> Unit,
    onContentGuard: () -> Unit,
    onLeaderboard: () -> Unit,
    onNavigateToTasteProfile: () -> Unit,
    onPremiumUpgrade: () -> Unit,
    onViewProfile: () -> Unit,
    onViewSettings: () -> Unit,
    onViewApiDocs: () -> Unit,
    onDeveloperDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSlate)
    ) {
        // Drawer Header with Branding & Identity
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(GoldPrimary, GoldSecondary)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieFilter,
                                contentDescription = null,
                                tint = DeepSlate,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "قَبَس",
                                color = GoldPrimary,
                                fontFamily = CairoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "منصة صناعة المحتوى الدعوي",
                                color = Color(0xFF94A3B8),
                                fontFamily = NotoSansFont,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onCloseDrawer,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق القائمة",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Role & Status Badge
                Surface(
                    color = if (isAdmin) Color(0xFFEF4444).copy(alpha = 0.15f) else GoldPrimary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isAdmin) Color(0xFFEF4444).copy(alpha = 0.4f) else GoldPrimary.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isAdmin) Icons.Default.Shield else Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isAdmin) Color(0xFFEF4444) else GoldPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAdmin) "وضع المسؤول والمطور 🛡️" else "حساب صانع محتوى متميز ✨",
                            color = if (isAdmin) Color(0xFFFCA5A5) else GoldPrimary,
                            fontFamily = CairoFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.5f))

        // Scrollable Drawer Items
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Group 1: الاستوديو والإنتاج المرئي
            QabasDrawerSectionHeader(
                title = "الاستوديو والإنتاج المرئي",
                icon = Icons.Default.VideoLibrary
            )
            QabasDrawerItem(
                title = "استوديو قبس الشامل",
                subtitle = "البوابة المركزية ومسارات الإنتاج الذكية",
                icon = Icons.Default.AutoAwesome,
                iconTint = GoldPrimary,
                badgeText = "الرئيسي",
                badgeColor = GoldPrimary,
                onClick = { onNavigateTo(onStudioGateway) }
            )
            QabasDrawerItem(
                title = "المحرر اليدوي الاحترافي",
                subtitle = "تايم لاين، مسارات، ومونتاج دقيق CapCut",
                icon = Icons.Default.MovieFilter,
                iconTint = Color(0xFF38BDF8),
                badgeText = "PRO",
                badgeColor = Color(0xFF38BDF8),
                onClick = { onNavigateTo(onProEditor) }
            )
            QabasDrawerItem(
                title = "استوديو الصور والمصغرات",
                subtitle = "تصميم أغلفة ومصغرات جذابة للفيديوهات",
                icon = Icons.Default.AddPhotoAlternate,
                iconTint = Color(0xFFF472B6),
                onClick = { onNavigateTo(onPhotoStudio) }
            )
            QabasDrawerItem(
                title = "ملقن الكاميرا الذكي",
                subtitle = "تسجيل الفيديو مع قراءة النصوص والتحكم بالسرعة",
                icon = Icons.Default.Videocam,
                iconTint = Color(0xFF34D399),
                onClick = { onNavigateTo(onTeleprompter) }
            )
            QabasDrawerItem(
                title = "مستنسخ الأنماط البصرية",
                subtitle = "تحليل وتكرار أساليب المونتاج الناجحة",
                icon = Icons.Default.Palette,
                iconTint = Color(0xFFA78BFA),
                onClick = { onNavigateTo(onVideoStyleCloner) }
            )
            QabasDrawerItem(
                title = "استوديو يوتيوب والشورتس",
                subtitle = "أدوات نشر وتحسين لقنوات الفيديو والريلز",
                icon = Icons.Default.Subscriptions,
                iconTint = Color(0xFFF87171),
                onClick = { onNavigateTo(onYouTubeStudio) }
            )
            QabasDrawerItem(
                title = "مشاريعي ومسوداتي",
                subtitle = "جميع الأعمال السابقة والمسودات المحفوظة",
                icon = Icons.Default.FolderOpen,
                iconTint = Color(0xFFFBBF24),
                onClick = { onNavigateTo(onViewProjects) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Group 2: واحة مِشكاة الهُدى والعلوم الشرعية
            QabasDrawerSectionHeader(
                title = "مِشكاة الهُدى والعلوم الشرعية",
                icon = Icons.Default.Mosque
            )
            QabasDrawerItem(
                title = "مِشكاة الهُدى العامة",
                subtitle = "واحة النور والقرآن والزاد الإيماني",
                icon = Icons.Default.Mosque,
                iconTint = GoldPrimary,
                onClick = { onNavigateTo(onQuranHub) }
            )
            QabasDrawerItem(
                title = "القرآن الكريم والتجويد",
                subtitle = "المصحف الشريف، أحكام التجويد، والتلاوات الخاشعة",
                icon = Icons.Default.MenuBook,
                iconTint = Color(0xFF10B981),
                onClick = { onNavigateTo(onQuranHub) }
            )
            QabasDrawerItem(
                title = "المكتبة الإسلامية",
                subtitle = "أمهات كتب التفسير، الحديث، وعقيدة أهل السنة",
                icon = Icons.Default.LibraryBooks,
                iconTint = Color(0xFFF59E0B),
                onClick = { onNavigateTo(onIslamicLibrary) }
            )
            QabasDrawerItem(
                title = "الأذكار وحصن المسلم",
                subtitle = "أذكار الصباح والمساء، الرقية، والسبحة الذكية",
                icon = Icons.Default.Favorite,
                iconTint = Color(0xFFEC4899),
                onClick = { onNavigateTo(onAzkar) }
            )
            QabasDrawerItem(
                title = "سيرة الأكابر وتراجم الأئمة",
                subtitle = "مواقف صمود علماء وسلف الأمة المباركة",
                icon = Icons.Default.PersonSearch,
                iconTint = Color(0xFF06B6D4),
                onClick = { onNavigateTo(onScholarBiographies) }
            )
            QabasDrawerItem(
                title = "الأذان ومواقيت الصلاة",
                subtitle = "مواقيت دقيقة، أصوات الأذان، وغرفة التحكم",
                icon = Icons.Default.NotificationsActive,
                iconTint = Color(0xFF3B82F6),
                onClick = { onNavigateTo(onPrayerTimes) }
            )
            QabasDrawerItem(
                title = "مكتبة الصوتيات الدعوية",
                subtitle = "خطب، محاضرات، ومؤثرات صوتية مباحة",
                icon = Icons.Default.Audiotrack,
                iconTint = Color(0xFF8B5CF6),
                onClick = { onNavigateTo(onAudioLibrary) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Group 3: الأدوات الذكية والمجتمع
            QabasDrawerSectionHeader(
                title = "الأدوات الذكية والانتشار",
                icon = Icons.Default.AutoAwesome
            )
            QabasDrawerItem(
                title = "ريلز قبس الدعوي",
                subtitle = "معرض الإنتاجات والفيديوهات القصيرة الملهمة",
                icon = Icons.Default.PlayCircleFilled,
                iconTint = Color(0xFFE11D48),
                onClick = { onNavigateTo(onReels) }
            )
            QabasDrawerItem(
                title = "مستشار الذكاء الاصطناعي",
                subtitle = "توليد نصوص دعوية وأفكار إخراجية مبتكرة",
                icon = Icons.Default.SmartToy,
                iconTint = AiCyan,
                badgeText = "AI",
                badgeColor = AiCyan,
                onClick = { onNavigateTo(onAiAssistant) }
            )
            QabasDrawerItem(
                title = "حارس المحتوى الشرعي",
                subtitle = "تدقيق ومراجعة المحتوى لضمان السلامة الشرعية",
                icon = Icons.Default.VerifiedUser,
                iconTint = Color(0xFF10B981),
                onClick = { onNavigateTo(onContentGuard) }
            )
            QabasDrawerItem(
                title = "لوحة الشرف وميدان الحسنات",
                subtitle = "رصيد النقاط والتنافس المبارك في نشر الخير",
                icon = Icons.Default.EmojiEvents,
                iconTint = GoldPrimary,
                onClick = { onNavigateTo(onLeaderboard) }
            )
            QabasDrawerItem(
                title = "الذوق الفني والأسلوب",
                subtitle = "تخصيص الهوية البصرية ونمطك المفضل في المونتاج",
                icon = Icons.Default.Tune,
                iconTint = Color(0xFF6366F1),
                onClick = { onNavigateTo(onNavigateToTasteProfile) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Group 4: الحساب والإدارة
            QabasDrawerSectionHeader(
                title = "الحساب والإدارة",
                icon = Icons.Default.Settings
            )
            QabasDrawerItem(
                title = "ترقية الحساب المميز (VIP)",
                subtitle = "تصدير فائق الدقة 4K، خوادم سريعة، وميزات حصرية",
                icon = Icons.Default.WorkspacePremium,
                iconTint = GoldPrimary,
                badgeText = "VIP",
                badgeColor = GoldPrimary,
                onClick = { onNavigateTo(onPremiumUpgrade) }
            )
            QabasDrawerItem(
                title = "الملف الشخصي",
                subtitle = "إدارة الحساب، الإحصائيات، وسجل الإنجازات",
                icon = Icons.Default.AccountCircle,
                iconTint = Color(0xFF94A3B8),
                onClick = { onNavigateTo(onViewProfile) }
            )
            QabasDrawerItem(
                title = "الإعدادات العامة",
                subtitle = "المظهر، التنبيهات، مفاتيح الـ API، واللغة",
                icon = Icons.Default.Settings,
                iconTint = Color(0xFFCBD5E1),
                onClick = { onNavigateTo(onViewSettings) }
            )
            QabasDrawerItem(
                title = "توثيق الواجهات البرمجية (API)",
                subtitle = "دليل الربط البرمجي للمطورين والمنصات الخارجية",
                icon = Icons.Default.Code,
                iconTint = Color(0xFF38BDF8),
                onClick = { onNavigateTo(onViewApiDocs) }
            )

            if (isAdmin) {
                QabasDrawerItem(
                    title = "لوحة تحكم المطورين",
                    subtitle = "تشخيص الخوادم، مفاتيح الذكاء، ومراقبة النظام",
                    icon = Icons.Default.AdminPanelSettings,
                    iconTint = Color(0xFFEF4444),
                    badgeText = "ROOT",
                    badgeColor = Color(0xFFEF4444),
                    onClick = { onNavigateTo(onDeveloperDashboard) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Footer Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "﴿ ادْعُ إِلَىٰ سَبِيلِ رَبِّكَ بِالْحِكْمَةِ وَالْمَوْعِظَةِ الْحَسَنَةِ ﴾",
                        color = GoldPrimary.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "قَبَس v2.5 • إصدار الإنتاج المبارك",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontFamily = NotoSansFont
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QabasDrawerItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    badgeText: String? = null,
    badgeColor: Color = GoldPrimary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontFamily = CairoFont,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = badgeColor.copy(alpha = 0.18f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = badgeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = CairoFont,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    color = Color(0xFF94A3B8),
                    fontFamily = NotoSansFont,
                    fontSize = 10.5.sp,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = Color(0xFF475569),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun QabasDrawerSectionHeader(title: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            color = GoldPrimary,
            fontFamily = CairoFont,
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        HorizontalDivider(
            color = Color(0xFF334155).copy(alpha = 0.4f),
            modifier = Modifier.weight(1f)
        )
    }
}

