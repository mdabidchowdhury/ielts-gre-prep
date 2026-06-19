package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.WordEntity
import com.example.data.UserStatsEntity
import com.example.utils.NotificationHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: VocabViewModel) {
    val context = LocalContext.current
    val selectedSegment by viewModel.selectedType.collectAsState()
    val words by viewModel.currentWords.collectAsState()
    val currentWordIndex by viewModel.currentWordIndex.collectAsState()
    val isFlipped by viewModel.isCardFlipped.collectAsState()
    val stats by viewModel.userStats.collectAsState()

    // 0 = Practice Flashcards, 1 = Word Dictionary, 2 = Stats & Settings
    var activeTab by remember { mutableStateOf(0) }

    // State for Custom Word Form
    var customWordText by remember { mutableStateOf("") }
    var customPhoneticText by remember { mutableStateOf("") }
    var customDefinitionText by remember { mutableStateOf("") }
    var customExampleText by remember { mutableStateOf("") }
    var customImportance by remember { mutableStateOf(2) }
    var customSegmentSelect by remember { mutableStateOf("IELTS") }
    var isFormValid by remember { mutableStateOf(false) }

    // Search filter for Dictionary
    var searchQuery by remember { mutableStateOf("") }

    // Notification Permission Launcher (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleNotifications(true)
            Toast.makeText(context, "Notifications enabled! You will receive daily goal alerts.", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.toggleNotifications(false)
            Toast.makeText(context, "Notification permission denied. In-app progress tracking is active.", Toast.LENGTH_SHORT).show()
        }
    }

    // Effect to update validity
    LaunchedEffect(customWordText, customDefinitionText) {
        isFormValid = customWordText.isNotBlank() && customDefinitionText.isNotBlank()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = "Vocab Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                "LexiPrep",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "IELTS & GRE Master",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    val activeStreak = stats?.streakCount ?: 0
                    Surface(
                        color = if (activeStreak > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("streak_badge")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "Streak",
                                tint = if (activeStreak > 0) Color(0xFFFF9800) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Streak: $activeStreak Day${if (activeStreak != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (activeStreak > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Quick simulated reminder toggle or trigger
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val checkedPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                                if (checkedPermission != PackageManager.PERMISSION_GRANTED) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.triggerReminderNotification(context)
                                    Toast.makeText(context, "Goal reminder push notification triggered!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewModel.triggerReminderNotification(context)
                                Toast.makeText(context, "Goal reminder push notification triggered!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .testTag("notification_trigger")
                            .padding(end = 4.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Reminders"
                        )
                    }

                    IconButton(
                        onClick = {
                            val activeDark = stats?.darkModePreferred ?: true
                            viewModel.toggleDarkMode(!activeDark)
                        },
                        modifier = Modifier.testTag("theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (stats?.darkModePreferred != false) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Filled.School, "Flashcards") },
                    label = { Text("Practice") },
                    modifier = Modifier.testTag("practice_nav")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Filled.Search, "Dictionary") },
                    label = { Text("Explorer") },
                    modifier = Modifier.testTag("dictionary_nav")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Filled.BarChart, "Dashboard") },
                    label = { Text("Dashboard") },
                    modifier = Modifier.testTag("dashboard_nav")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Segment Header for IELTS vs GRE
            if (activeTab != 2) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        SegmentedButton(
                            selected = selectedSegment == "IELTS",
                            onClick = { viewModel.changeSegment("IELTS") },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("IELTS Segment", fontWeight = FontWeight.Bold)
                            }
                        }
                        SegmentedButton(
                            selected = selectedSegment == "GRE",
                            onClick = { viewModel.changeSegment("GRE") },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GRE Segment", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = activeTab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "TabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> {
                        PracticeTab(
                            words = words,
                            currentIndex = currentWordIndex,
                            isFlipped = isFlipped,
                            onFlip = { viewModel.flipCard() },
                            onMarkCorrect = { word -> viewModel.recordReview(word, true) },
                            onMarkWrong = { word -> viewModel.recordReview(word, false) },
                            onWordSelect = { index -> viewModel.setWordIndex(index) },
                            stats = stats,
                            viewModel = viewModel,
                            selectedSegment = selectedSegment
                        )
                    }
                    1 -> {
                        DictionaryTab(
                            words = words,
                            searchQuery = searchQuery,
                            onSearchQueryChanged = { searchQuery = it },
                            onDelete = { word -> viewModel.deleteWord(word) }
                        )
                    }
                    2 -> {
                        DashboardTab(
                            stats = stats,
                            viewModel = viewModel,
                            customWordText = customWordText,
                            onWordChanged = { customWordText = it },
                            customPhoneticText = customPhoneticText,
                            onPhoneticChanged = { customPhoneticText = it },
                            customDefinitionText = customDefinitionText,
                            onDefinitionChanged = { customDefinitionText = it },
                            customExampleText = customExampleText,
                            onExampleChanged = { customExampleText = it },
                            customImportance = customImportance,
                            onImportanceChanged = { customImportance = it },
                            customSegmentSelect = customSegmentSelect,
                            onSegmentChanged = { customSegmentSelect = it },
                            isFormValid = isFormValid,
                            onAddWord = {
                                viewModel.addNewWord(
                                    word = customWordText,
                                    phonetic = customPhoneticText,
                                    definition = customDefinitionText,
                                    example = customExampleText,
                                    type = customSegmentSelect,
                                    importance = customImportance
                                )
                                // Clear inputs
                                customWordText = ""
                                customPhoneticText = ""
                                customDefinitionText = ""
                                customExampleText = ""
                                Toast.makeText(context, "New Vocab Word Saved!", Toast.LENGTH_SHORT).show()
                            },
                            permissionLauncher = permissionLauncher
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeTab(
    words: List<WordEntity>,
    currentIndex: Int,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onMarkCorrect: (WordEntity) -> Unit,
    onMarkWrong: (WordEntity) -> Unit,
    onWordSelect: (Int) -> Unit,
    stats: UserStatsEntity?,
    viewModel: VocabViewModel,
    selectedSegment: String
) {
    var practiceSubTab by remember { mutableStateOf(0) } // 0 = Flashcards, 1 = March Game, 2 = Exam Prep Mock

    if (words.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No Words Available",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add your custom words on the Dashboard tab, or let the database seed initialize.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        return
    }

    val currentWord = words.getOrNull(currentIndex) ?: words.first()

    // Top Level Tabs inside Practice View
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sub-navigation bar
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            SegmentedButton(
                selected = practiceSubTab == 0,
                onClick = { practiceSubTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cards", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            SegmentedButton(
                selected = practiceSubTab == 1,
                onClick = { practiceSubTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Games, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Match Game", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            SegmentedButton(
                selected = practiceSubTab == 2,
                onClick = { practiceSubTab = 2 },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Quiz, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mock Prep", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // Learning Progress Mastery Card (Unified Progress)
        val masteredCount = words.count { it.correctCount >= 1 }
        val totalCount = words.size
        val masteryPercent = if (totalCount > 0) (masteredCount.toFloat() / totalCount.toFloat()) else 0f

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(42.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { masteryPercent },
                        strokeWidth = 4.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "${(masteryPercent * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "📈 Mastered Vocabulary Progress",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "$masteredCount of $totalCount words on this list learned successfully!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Render Active Mode
        if (practiceSubTab == 0) {
            // FLASHCARDS MODE
            // Linear Progress bar towards meeting Daily Goal
            val goal = stats?.dailyGoalCount ?: 10
            val progressCount = stats?.reviewedTodayCount ?: 0
            val percentage = (progressCount.toFloat() / goal.toFloat()).coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📅 Focus Progress Today",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "$progressCount / $goal words",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (percentage >= 1f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
                if (percentage >= 1f) {
                    Text(
                        "🔥 Daily study goal achieved! Excellent job!",
                        color = Color(0xFF4CAF50),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Horizontal Selection Carousel
            Text(
                "Selected Segment Words (${words.size} available):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )

            ScrollableTabRow(
                selectedTabIndex = currentIndex,
                edgePadding = 0.dp,
                indicator = {},
                divider = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                words.forEachIndexed { idx, item ->
                    val isSelected = idx == currentIndex
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = if (item.isMistakenLastTime) BorderStroke(1.5.dp, MaterialTheme.colorScheme.error) else null,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                            .clickable { onWordSelect(idx) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.word,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (item.isMistakenLastTime) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Mistaken Last Time",
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // FLASHCARD WORKSPACE
            CardContainer(
                word = currentWord,
                isFlipped = isFlipped,
                onFlip = onFlip
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Review Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { onMarkWrong(currentWord) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF3F2B2B),
                        contentColor = Color(0xFFF2B8B5)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF8C1D18)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(end = 6.dp)
                        .testTag("mark_wrong_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Cancel, 
                            contentDescription = "Mistook",
                            tint = Color(0xFFF2B8B5),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mistook", 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                OutlinedButton(
                    onClick = { onMarkCorrect(currentWord) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF213021),
                        contentColor = Color(0xFFB5F2B5)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF3E513E)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(start = 6.dp)
                        .testTag("mark_correct_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle, 
                            contentDescription = "Know It",
                            tint = Color(0xFFB5F2B5),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Know It", 
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Tip: Click the flashcard above to flip and view the full definition, phonetic spelling, and examples before making your decision!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else if (practiceSubTab == 1) {
            // MATCHING GAME MODE
            var gameScore by remember { mutableStateOf(0) }
            var timerSeconds by remember { mutableStateOf(40) }
            var isGameRunning by remember { mutableStateOf(false) }
            var showGameOver by remember { mutableStateOf(false) }

            var shuffledWords by remember { mutableStateOf<List<WordEntity>>(emptyList()) }
            var shuffledDefs by remember { mutableStateOf<List<WordEntity>>(emptyList()) }
            var selectedWordId by remember { mutableStateOf<Int?>(null) }
            var selectedDefId by remember { mutableStateOf<Int?>(null) }
            var matchedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

            val loadInitialMatchState = {
                val subset = words.shuffled().take(4)
                shuffledWords = subset.shuffled()
                shuffledDefs = subset.shuffled()
                selectedWordId = null
                selectedDefId = null
                matchedIds = emptySet()
            }

            val startGame = {
                gameScore = 0
                timerSeconds = 40
                isGameRunning = true
                showGameOver = false
                loadInitialMatchState()
            }

            LaunchedEffect(isGameRunning) {
                if (isGameRunning) {
                    while (timerSeconds > 0) {
                        delay(1000)
                        timerSeconds -= 1
                    }
                    isGameRunning = false
                    showGameOver = true
                    viewModel.submitMatchScore(gameScore)
                }
            }

            if (!isGameRunning && !showGameOver) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Games,
                        contentDescription = "Match game logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Speed Match Arena",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Test your vocabulary memory with a 40-second matching challenge! Match concepts to definitions correctly.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { startGame() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("start_match_game")
                    ) {
                        Text("🎮 Launch Game Arena", fontWeight = FontWeight.Black)
                    }
                }
            } else if (showGameOver) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "⏰ Time Over!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "You matched $gameScore pairs successfully!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Gems Earned: +${gameScore * 2} 💎",
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Matching High Score: ${stats?.maxMatchGameScore ?: 0} pts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { startGame() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("🎮 Replay Challenge")
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⌛ Time remaining: ${timerSeconds}s",
                        fontWeight = FontWeight.Black,
                        color = if (timerSeconds < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Score: $gameScore pts",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Column Left: Words
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Words", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        shuffledWords.forEach { word ->
                            val isMatched = matchedIds.contains(word.id)
                            val isSelected = selectedWordId == word.id
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clickable(enabled = !isMatched) {
                                        selectedWordId = word.id
                                        // Evaluate instant match
                                        if (selectedDefId != null) {
                                            if (selectedWordId == selectedDefId) {
                                                matchedIds = matchedIds + word.id
                                                gameScore += 1
                                                selectedWordId = null
                                                selectedDefId = null
                                                if (matchedIds.size == 4) {
                                                    loadInitialMatchState()
                                                }
                                            } else {
                                                selectedWordId = null
                                                selectedDefId = null
                                            }
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMatched) Color(0xFF1E351E)
                                                    else if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isMatched) BorderStroke(1.dp, Color(0xFF4CAF50))
                                         else if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary)
                                         else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = word.word,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isMatched) Color(0xFF81C784)
                                                else if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Column Right: Meanings
                    Column(
                        modifier = Modifier.weight(1.3f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Definitions", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        shuffledDefs.forEach { def ->
                            val isMatched = matchedIds.contains(def.id)
                            val isSelected = selectedDefId == def.id
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clickable(enabled = !isMatched) {
                                        selectedDefId = def.id
                                        // Evaluate instant match
                                        if (selectedWordId != null) {
                                            if (selectedWordId == selectedDefId) {
                                                matchedIds = matchedIds + def.id
                                                gameScore += 1
                                                selectedWordId = null
                                                selectedDefId = null
                                                if (matchedIds.size == 4) {
                                                    loadInitialMatchState()
                                                }
                                            } else {
                                                selectedWordId = null
                                                selectedDefId = null
                                            }
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMatched) Color(0xFF1E351E)
                                                    else if (isSelected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = if (isMatched) BorderStroke(1.dp, Color(0xFF4CAF50))
                                         else if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.onPrimary)
                                         else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = def.definition,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        maxLines = 3,
                                        color = if (isMatched) Color(0xFF81C784)
                                                else if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // EXAM MOCK PREP
            if (selectedSegment == "IELTS") {
                // IELTS WRITING ESSAY PREP
                val prompts = listOf(
                    "Many natural environments are being deleteriously impacted by rapid industrial expansion. Some think preserving nature is key, others prefer industrial growth. Discuss both sides.",
                    "The ubiquitous availability of educational technology enables personalized online classrooms. Speak about benefits or adverse challenges of electronic learning."
                )
                var activePromptIdx by remember { mutableStateOf(0) }
                var essayContentText by remember { mutableStateOf("") }
                var scoringAnalysis by remember { mutableStateOf("") }
                var bandScoreResult by remember { mutableStateOf("") }

                val targetVocabsToCheck = listOf("Ubiquitous", "Deleterious", "Ameliorate", "Anomalous", "Adverse")

                val evaluateEssay = {
                    val typed = essayContentText.lowercase()
                    val used = targetVocabsToCheck.filter { word -> typed.contains(word.lowercase()) }
                    viewModel.triggerAchievement("IELTS_ESSAY_COMPLETED", 15) // reward 15 gems
                    
                    bandScoreResult = when (used.size) {
                        0 -> "Band 5.5 (Insufficient Lexical Integration)"
                        1 -> "Band 6.5 (Standard Usage of Curated Vocabulary)"
                        2 -> "Band 7.5 (Advanced Lexical Variety Demonstrated)"
                        else -> "Band 8.5 (Mastery-Level Contextual Integration!)"
                    }
                    
                    scoringAnalysis = "Analysis: Detected the following IELTS vocab cards in your paragraph: ${used.joinToString().ifEmpty { "None" }}. " +
                            "Using words correctly demonstrates range and structural richness. Essay word count: ${typed.split("\\s+".toRegex()).filter { it.isNotBlank() }.size} words."
                }

                LazyColumn(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                    item {
                        Text(
                            "📝 IELTS Writing Task 2 Essay Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Incorporate advanced vocab cards from your active segment list into your essay responses to secure band scores of 7.5+.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Select Essay Prompt Topic:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { activePromptIdx = 0; scoringAnalysis = ""; bandScoreResult = "" },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (activePromptIdx == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    ) { Text("Preservation", fontSize = 11.sp) }
                                    Button(
                                        onClick = { activePromptIdx = 1; scoringAnalysis = ""; bandScoreResult = "" },
                                        colors = ButtonDefaults.buttonColors(containerColor = if (activePromptIdx == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    ) { Text("Tech & School", fontSize = 11.sp) }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(prompts[activePromptIdx], style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Curated helpful vocabulary hints row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            targetVocabsToCheck.forEach { hint ->
                                val wasUsed = essayContentText.lowercase().contains(hint.lowercase())
                                Surface(
                                    color = if (wasUsed) Color(0xFF1E351E) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (wasUsed) "✓ $hint" else "+ $hint",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (wasUsed) Color(0xFF81C784) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = essayContentText,
                            onValueChange = { essayContentText = it },
                            label = { Text("Draft your essay paragraph outline...") },
                            modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { evaluateEssay() },
                                enabled = essayContentText.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🔍 Score Paragraph", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (bandScoreResult.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Estimated Essay score:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text(bandScoreResult, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(scoringAnalysis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("🏆 +15 Gems credited for writing practice!", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                                }
                            }
                        }
                    }
                }
            } else {
                // GRE READING PASSAGE COMPREHENSION PREP
                var isOptionAnswered by remember { mutableStateOf(false) }
                var answerFeedbackName by remember { mutableStateOf("") }
                var selectedQuizAnswerOpt by remember { mutableStateOf<Int?>(null) }

                LazyColumn(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                    item {
                        Text(
                            "📖 GRE Context Reading Passages",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Understand obscure prose and detect the definitions of advanced GRE cards in scientific or philosophical context.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "GRE Reading Comprehension Passage:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "The board of governors remained stubbornly obdurate despite the overwhelming statistical metrics presented. " +
                                    "To historical onlookers, selecting feather ink pens in an automated technological age felt like an amusing anachronism. " +
                                    "The meeting was characterized by esoteric algorithms which remain completely unknown to standard public systems.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Question: Based on content context, what is the best explanation for the governors being 'obdurate'?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )

                        val choices = listOf(
                            "Eager to reform their archaic system",
                            "Uncompromisingly stubborn and resisting adaptation",
                            "Adopting highly concise decision policies",
                            "Fascinated by advanced tech paradigms"
                        )

                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            choices.forEachIndexed { optIndex, content ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!isOptionAnswered) {
                                                selectedQuizAnswerOpt = optIndex
                                            }
                                        }
                                ) {
                                    RadioButton(
                                        selected = selectedQuizAnswerOpt == optIndex,
                                        onClick = { if (!isOptionAnswered) selectedQuizAnswerOpt = optIndex }
                                    )
                                    Text(content, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        Button(
                            onClick = {
                                isOptionAnswered = true
                                if (selectedQuizAnswerOpt == 1) {
                                    answerFeedbackName = "Correct! Obdurate means stubborn and unyielding. Excellent vocabulary retrieval. +10 Gems credited!"
                                    viewModel.triggerAchievement("GRE_MOCK_SUCCESS", 10)
                                } else {
                                    answerFeedbackName = "Incorrect. The passage states they remained 'obdurate despite statistical metrics', which implies they were stubborn and resisted adaptions."
                                }
                            },
                            enabled = selectedQuizAnswerOpt != null && !isOptionAnswered,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Exam Selection", fontWeight = FontWeight.Bold)
                        }

                        if (isOptionAnswered) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (selectedQuizAnswerOpt == 1) Color(0xFF1E351E) else Color(0xFF3F2B2B))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = if (selectedQuizAnswerOpt == 1) "✓ Correct Answer Choice" else "✗ Review Required",
                                        color = if (selectedQuizAnswerOpt == 1) Color(0xFF81C784) else Color(0xFFF2B8B5),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = answerFeedbackName,
                                        color = if (selectedQuizAnswerOpt == 1) Color(0xFFC8E6C9) else Color(0xFFFFCDD2),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = {
                                            isOptionAnswered = false
                                            selectedQuizAnswerOpt = null
                                            answerFeedbackName = ""
                                        }
                                    ) {
                                        Text("🔄 Try Again", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.CardContainer(
    word: WordEntity,
    isFlipped: Boolean,
    onFlip: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12 * density
                clip = false
            }
            .clickable { onFlip() }
            .testTag("word_flashcard"),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rotation > 90f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = if (word.isMistakenLastTime) 2.5.dp else 1.dp,
            color = if (word.isMistakenLastTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (rotation <= 90f) {
                // Front visual context
                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    // Top Row
                    Row(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = word.type,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(word.importance) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            repeat(3 - word.importance) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Middle Section (Word & Phonetic)
                    Column(
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (word.isMistakenLastTime) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = "Mistake Logo",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Mistaken Last Time!",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        } else if (word.wasMistakenEver) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Mistake Resolved",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Corrected Past Mistake",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        Text(
                            text = word.word,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = word.phonetic,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Bottom Hint
                    Surface(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flip,
                                contentDescription = "Flip Card",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Tap Card to Flip",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Back visual context (rotated 180 degrees)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .graphicsLayer {
                            rotationY = 180f
                        },
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Definition & Details",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (word.isCustom) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Custom Word",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = word.word,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = word.phonetic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Divider(color = MaterialTheme.colorScheme.outlineVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "DEFINITION",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = word.definition,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    Text(
                        text = "SAMPLE SENTENCE",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = "\"${word.example}\"",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Stats on this specific card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatsMetric(label = "Appearances", value = "${word.appearedCount}")
                        StatsMetric(label = "Correct", value = "${word.correctCount}", color = Color(0xFF2E7D32))
                        StatsMetric(label = "Incorrect", value = "${word.wrongCount}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsMetric(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryTab(
    words: List<WordEntity>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onDelete: (WordEntity) -> Unit
) {
    val filteredWords = words.filter {
        it.word.contains(searchQuery, ignoreCase = true) ||
                it.definition.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Search words or definitions...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Filled.Cancel, contentDescription = "Clear Search")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("dictionary_search"),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        if (filteredWords.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No matching words found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredWords, key = { it.id }) { word ->
                    WordItemRow(word = word, onDelete = onDelete)
                }
            }
        }
    }
}

@Composable
fun WordItemRow(word: WordEntity, onDelete: (WordEntity) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("word_row_${word.word.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        border = if (word.isMistakenLastTime) BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = word.phonetic,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (word.type == "IELTS") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = word.type,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (word.type == "IELTS") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (word.isCustom) {
                        IconButton(
                            onClick = { onDelete(word) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Custom Word",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = word.definition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (word.example.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "\"${word.example}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Priority: ${if (word.importance == 3) "High" else if (word.importance == 2) "Medium" else "Low"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (word.isMistakenLastTime) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "Mistaken Previously",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Reviewed: ${word.appearedCount} times (${word.correctCount} correct)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DashboardTab(
    stats: UserStatsEntity?,
    viewModel: VocabViewModel,
    customWordText: String,
    onWordChanged: (String) -> Unit,
    customPhoneticText: String,
    onPhoneticChanged: (String) -> Unit,
    customDefinitionText: String,
    onDefinitionChanged: (String) -> Unit,
    customExampleText: String,
    onExampleChanged: (String) -> Unit,
    customImportance: Int,
    onImportanceChanged: (Int) -> Unit,
    customSegmentSelect: String,
    onSegmentChanged: (String) -> Unit,
    isFormValid: Boolean,
    onAddWord: () -> Unit,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PERFORMANCE OVERVIEW CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🏆 Personalized Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DashboardMetricBox(
                            label = "Daily Reviews",
                            value = "${stats?.reviewedTodayCount ?: 0}/${stats?.dailyGoalCount ?: 10}",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        DashboardMetricBox(
                            label = "Current Streak",
                            value = "${stats?.streakCount ?: 0} Days",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Tip: Make your in-app decisions accurate! Mastered words move out of immediate frequency, while mistaken words are prioritized automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // CHANNELS & SETTINGS FOR DAILY GOALS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "⚙️ Learning Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Daily Goals Slider
                    val currentGoal = stats?.dailyGoalCount ?: 10
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Daily Vocabulary Goal:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "$currentGoal words/day",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = currentGoal.toFloat(),
                            onValueChange = { viewModel.changeDailyGoal(it.toInt()) },
                            valueRange = 5f..30f,
                            steps = 5,
                            modifier = Modifier.testTag("goal_slider")
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    // Notification Config
                    val remindersOn = stats?.notificationsEnabled ?: true
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Daily Goal Push Reminders",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Provides timely system reminders on your goals",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = remindersOn,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val status = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        )
                                        if (status != PackageManager.PERMISSION_GRANTED) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.toggleNotifications(true)
                                        }
                                    } else {
                                        viewModel.toggleNotifications(true)
                                    }
                                } else {
                                    viewModel.toggleNotifications(false)
                                }
                            },
                            modifier = Modifier.testTag("notification_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.triggerReminderNotification(context)
                            Toast.makeText(context, "Goal reminder notification sent!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simulate Goal Alert Notification", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.syncManhattanWords { added ->
                                if (added > 0) {
                                    Toast.makeText(context, "Successfully synchronized & loaded $added new Manhattan Prep GRE words!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "All Manhattan Prep GRE words are already initialized and up-to-date!", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sync_manhattan_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync 100+ Manhattan Prep Words", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val aiSyncState by viewModel.aiSyncState.collectAsState()
                    val aiSyncProgress by viewModel.aiSyncProgress.collectAsState()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        when (aiSyncState) {
                            null -> {
                                Button(
                                    onClick = {
                                        viewModel.triggerGeminiIeltsExpansion { added ->
                                            if (added > 0) {
                                                Toast.makeText(context, "Sourcing Complete! Sourced and added $added premium IELTS words via Gemini API!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("ai_expand_ielts_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("✨ Sourced IELTS AI Expansion (500 words)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sourced directly using the Gemini 3.5 Flash API in a smart background coroutine batch task. Real IPAs and contextual models are automatically injected.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                            "LOADING" -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "🤖 Sourcing IELTS Words (Batch)...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                "$aiSyncProgress / 500",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = { (aiSyncProgress.toFloat() / 500f).coerceIn(0f, 1f) },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Downloading premium definitions and sample sentences from Gemini to make flashcards extremely rich...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                            "SUCCESS" -> {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Sourcing Complete!",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Successfully expanded the active IELTS study lists by $aiSyncProgress high-frequency vocabulary words curated directly by the Gemini API!",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF1B5E20),
                                            fontSize = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        TextButton(
                                            onClick = { viewModel.resetAiSyncState() },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Expand Again", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            "ERROR_KEY" -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Gemini API Key Required",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Please enter your Gemini API Key in the AI Studio Secrets panel. This prevents decompiling exposure.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { viewModel.resetAiSyncState() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Text("Dismiss", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            else -> {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Sourcing Failed",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "An error occurred while calling the Gemini API. Please make sure you have internet access and valid credentials.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 10.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { viewModel.resetAiSyncState() }) {
                                                Text("Reset", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ADD CUSTOM VOCAB CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "✍️ Add New Vocabulary Word",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Expand lists with words you discover.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = customWordText,
                        onValueChange = onWordChanged,
                        label = { Text("Vocabulary Word") },
                        placeholder = { Text("e.g., Obsequious") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("input_custom_word"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = customPhoneticText,
                        onValueChange = onPhoneticChanged,
                        label = { Text("Phonetic Pronunciation (Optional)") },
                        placeholder = { Text("e.g., /əbˈsiːkwiəs/") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = customDefinitionText,
                        onValueChange = onDefinitionChanged,
                        label = { Text("Word Definition") },
                        placeholder = { Text("e.g., Obedient or attentive to an excessive degree.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("input_custom_def"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = customExampleText,
                        onValueChange = onExampleChanged,
                        label = { Text("Example Sentence (Optional)") },
                        placeholder = { Text("e.g., The servants were obsequious, bowing low at every transaction.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Target segment segment Row
                    Text(
                        "Preparation Segment:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onSegmentChanged("IELTS") }
                        ) {
                            RadioButton(
                                selected = customSegmentSelect == "IELTS",
                                onClick = { onSegmentChanged("IELTS") }
                            )
                            Text("IELTS Prep", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onSegmentChanged("GRE") }
                        ) {
                            RadioButton(
                                selected = customSegmentSelect == "GRE",
                                onClick = { onSegmentChanged("GRE") }
                            )
                            Text("GRE Prep", style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Importance level selector
                    Text(
                        "Word Priority (Importance / Appearance Weight):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(1, 2, 3).forEach { rating ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onImportanceChanged(rating) }
                            ) {
                                RadioButton(
                                    selected = customImportance == rating,
                                    onClick = { onImportanceChanged(rating) }
                                )
                                Text(
                                    text = if (rating == 3) "🔥 High" else if (rating == 2) "⚡ Med" else "🌱 Low",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onAddWord,
                        enabled = isFormValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_custom_word_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add to Master Vocabulary", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMetricBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
