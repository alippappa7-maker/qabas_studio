package com.qabas.app

import androidx.compose.runtime.Immutable

@Immutable
data class IdeaAnalysis(
    val summary: String,
    val suggestedStyle: String = "",
    val goal: String = "",
    val targetAudience: String = "",
    val tone: String = "",
    val keywords: List<String> = emptyList(),
    val proposedScenes: List<String> = emptyList(),
    val viralityScore: Int = 0,
    val hookSuggestions: List<String> = emptyList(),
    val ctaSuggestions: List<String> = emptyList(),
    val confidence: Double = 0.0,
    val themes: List<String> = emptyList(),
    val source: String = "MODEL"
)

@Immutable
data class VideoStyleAnalysis(
    val detectedStyle: String = "",
    val dominantColors: String = "",
    val transitionSpeed: String = "",
    val movementPatterns: String = "",
    val overallRhythm: String = "",
    val audioStyle: String = "",
    val typographyStyle: String = "",
    val contentTone: String = "",
    val targetAudience: String = "",
    val keywords: List<String> = emptyList()
)

@Immutable
data class TrendingIdea(
    val title: String,
    val description: String,
    val viralityScore: Int = 0,
    val tags: List<String> = emptyList(),
    val source: String = "MODEL"
)

@Immutable
data class Template(
    val id: String = "",
    val name: String = "",
    val parameters: Map<String, String> = emptyMap(),
    val colors: String = "",
    val transitions: String = "",
    val textAnimation: String = ""
)

@Immutable
data class ScholarBiography(
    val id: String,
    val name: String,
    val brief: String,
    val imageRes: Int,
    val audioUrl: String? = null,
    val period: String? = null,
    val tags: List<String> = emptyList(),
    val sacrificeSummary: String? = null,
    val enduranceQuote: String? = null,
    val travelDistance: String? = null,
    val globalImpact: String? = null,
    val messageToGrandson: String? = null,
    val modernChallenge: String? = null,
    val whoHarmedHimAndHow: String? = null, // من الذين آذوه وكيف؟
    val howHeEndured: String? = null,       // كيف تحمّل وسر الصمود؟
    val whyHeEndured: String? = null,       // لماذا تحمّل؟ القضية والغاية الكبرى
    val deathScene: String? = null,         // مشهد الوفاة واللحظات الأخيرة
    val lastWords: String? = null,          // آخر كلماته قبل خروج الروح
    val funeralImpact: String? = null       // مشهد الجنازة وتأثر الأمة
)
