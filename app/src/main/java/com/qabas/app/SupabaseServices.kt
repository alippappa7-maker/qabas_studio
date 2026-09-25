package com.qabas.app

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import java.util.UUID

/**
 * High-level Supabase service module.
 *
 * Mirrors the shape of [CloudServices] so callers can fall back to the local
 * handler when Supabase is not configured. Every public method returns
 * `null` / `emptyList()` / `false` when Supabase is unavailable so the app
 * degrades gracefully.
 *
 * Requires:
 *   - SUPABASE_URL in .env (BuildConfig.SUPABASE_URL)
 *   - SUPABASE_ANON_KEY in .env (BuildConfig.SUPABASE_ANON_KEY)
 */
object SupabaseServices {

    private const val TAG = "SupabaseServices"

    val isSupabaseAvailable: Boolean
        get() = SupabaseConfig.isConfigured

    private val client get() = SupabaseConfig.client

    // ---------- Typed rows ----------

    @Serializable
    data class UserRow(
        val id: String,
        @SerialName("external_id") val externalId: String? = null,
        val email: String? = null,
        val name: String? = null,
        val type: String = "free",
        val status: String? = "active",
        val strikes: Int = 0,
        @SerialName("is_eligible") val isEligible: Boolean = true,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )

    @Serializable
    data class HadithRow(
        val id: String,
        val title: String,
        val narrator: String,
        val text: String,
        val status: String,
        val source: String,
        @SerialName("theme_bg") val themeBg: String = "black_gold",
        @SerialName("suggested_aspect") val suggestedAspect: String = "9:16",
        val category: String? = null,
        @SerialName("display_order") val displayOrder: Int = 0,
        @SerialName("is_active") val isActive: Boolean = true
    )

    @Serializable
    data class ProjectRow(
        val id: String,
        @SerialName("user_id") val userId: String? = null,
        @SerialName("external_id") val externalId: String? = null,
        val title: String,
        val description: String? = null,
        val status: String = "draft",
        val cost: Double = 0.0,
        val tags: JsonElement = JsonArray(emptyList()),
        val metadata: JsonElement = JsonObject(emptyMap()),
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )

    @Serializable
    data class StyleRow(
        val id: String,
        @SerialName("user_id") val userId: String? = null,
        val name: String,
        @SerialName("source_video") val sourceVideo: String? = null,
        val payload: JsonElement = JsonObject(emptyMap()),
        val score: Int = 0,
        val tags: JsonElement = JsonArray(emptyList()),
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )

    @Serializable
    data class MasterStyleRow(
        val id: String,
        @SerialName("user_id") val userId: String? = null,
        val name: String,
        val description: String? = null,
        val payload: JsonElement = JsonObject(emptyMap()),
        val compliance: Int = 95,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null
    )

    @Serializable
    data class PromoCodeRow(
        val code: String,
        val type: String = "PROMO",
        val value: Long = 0L,
        @SerialName("is_active") val isActive: Boolean = true,
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    data class GuardLogRow(
        val id: String,
        @SerialName("user_id") val userId: String? = null,
        val snippet: String = "",
        val verdict: String = "",
        val score: Int = 0,
        val reason: String = "",
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    data class TransactionRow(
        val id: String,
        @SerialName("user_id") val userId: String? = null,
        @SerialName("product_id") val productId: String = "",
        @SerialName("purchase_token") val purchaseToken: String = "",
        @SerialName("price_amount") val priceAmount: Double = 0.0,
        val currency: String = "USD",
        @SerialName("created_at") val createdAt: String? = null
    )

    @Serializable
    data class AudioTrackRow(
        val id: String,
        val title: String,
        val artist: String = "",
        val category: String = "تلاوات قرآنية",
        @SerialName("audio_url") val audioUrl: String,
        val duration: String = "0:00",
        val badge: String = "سحابي",
        val author: String = "",
        @SerialName("likes_count") val likesCount: Int = 0,
        @SerialName("is_active") val isActive: Boolean = true,
        @SerialName("user_id") val userId: String? = null,
        @SerialName("created_at") val createdAt: String? = null
    )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ---------- Authentication ----------

    object Auth {
        private val _currentUser = MutableStateFlow<UserRow?>(null)
        val currentUser: StateFlow<UserRow?> = _currentUser.asStateFlow()

        fun getCurrentUserId(): String? {
            if (!isSupabaseAvailable) return null
            return runCatching { client.auth.currentUserOrNull()?.id }.getOrNull()
        }

        suspend fun register(email: String, password: String): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
                true
            }.onFailure { Log.w(TAG, "Supabase register failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun login(email: String, password: String): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                true
            }.onFailure { Log.w(TAG, "Supabase login failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun resetPassword(email: String): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.auth.resetPasswordForEmail(email)
                true
            }.onFailure { Log.w(TAG, "Supabase reset password failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun updatePassword(newPassword: String): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.auth.updateUser {
                    password = newPassword
                }
                true
            }.onFailure { Log.w(TAG, "Supabase update password failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun logout() {
            if (!isSupabaseAvailable) return
            runCatching { client.auth.signOut() }
                .onFailure { Log.w(TAG, "Supabase logout failed: ${it.message}") }
        }
    }

    // ---------- Database ----------

    object Database {
        /** Lightweight health check used by dashboards and the developer screen. */
        suspend fun ping(): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.postgrest.from("users").select().decodeListOrEmpty<UserRow>()
                true
            }.onFailure { Log.w(TAG, "Supabase ping failed: ${it.message}") }
                .getOrDefault(false)
        }

        /**
         * Look up a user row by external_id (typically the Firebase Auth uid).
         * If no row is found, inserts a fresh row and returns the inserted
         * representation. Returns `null` when Supabase is unavailable.
         */
        suspend fun ensureUser(externalId: String, email: String?, name: String?): UserRow? {
            if (!isSupabaseAvailable) return null
            return runCatching {
                val existing = client.postgrest.from("users").select {
                    filter { eq("external_id", externalId) }
                    limit(1)
                }.decodeListOrEmpty<UserRow>().firstOrNull()
                existing ?: client.postgrest.from("users").upsert(
                    JsonArray(
                        listOf(
                            json.encodeToJsonElement(
                                UserRow(
                                    id = UUID.randomUUID().toString(),
                                    externalId = externalId,
                                    email = email,
                                    name = name
                                )
                            )
                        )
                    )
                ) {
                    onConflict = "external_id"
                }.decodeSingle<UserRow>()
            }.onFailure { Log.w(TAG, "ensureUser failed: ${it.message}") }
                .getOrNull()
        }

        suspend fun saveProject(
            title: String,
            description: String?,
            status: String = "draft",
            cost: Double = 0.0,
            tags: List<String> = emptyList(),
            metadata: Map<String, String> = emptyMap()
        ): ProjectRow? {
            if (!isSupabaseAvailable) return null
            return runCatching {
                val userId = Auth.getCurrentUserId()
                val row = ProjectRow(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    externalId = userId,
                    title = title,
                    description = description,
                    status = status,
                    cost = cost,
                    tags = JsonArray(tags.map { JsonPrimitive(it) }),
                    metadata = JsonObject(metadata.mapValues { (_, v) -> JsonPrimitive(v) })
                )
                client.postgrest.from("projects").insert(
                    JsonArray(listOf(json.encodeToJsonElement(row)))
                ).decodeSingle<ProjectRow>()
            }.onFailure { Log.w(TAG, "saveProject failed: ${it.message}") }
                .getOrNull()
        }

        suspend fun saveFullProject(
            id: String,
            title: String,
            description: String?,
            status: String = "draft",
            cost: Double = 0.0,
            tags: List<String> = emptyList(),
            metadata: Map<String, String> = emptyMap()
        ): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                val userId = Auth.getCurrentUserId()
                val row = ProjectRow(
                    id = id,
                    userId = userId,
                    externalId = userId,
                    title = title,
                    description = description,
                    status = status,
                    cost = cost,
                    tags = JsonArray(tags.map { JsonPrimitive(it) }),
                    metadata = JsonObject(metadata.mapValues { (_, v) -> JsonPrimitive(v) })
                )
                client.postgrest.from("projects").upsert(
                    JsonArray(listOf(json.encodeToJsonElement(row)))
                ) {
                    onConflict = "id"
                }
                true
            }.onFailure { Log.w(TAG, "saveFullProject failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun deleteProject(id: String): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.postgrest.from("projects").delete {
                    filter { eq("id", id) }
                }
                true
            }.onFailure { Log.w(TAG, "deleteProject failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun getUserProjects(userId: String? = null): List<ProjectRow> {
            if (!isSupabaseAvailable) return emptyList()
            val targetUid = userId ?: Auth.getCurrentUserId() ?: return emptyList()
            return runCatching {
                client.postgrest.from("projects").select {
                    filter {
                        or {
                            eq("user_id", targetUid)
                            eq("external_id", targetUid)
                        }
                    }
                    order("created_at", Order.DESCENDING)
                }.decodeList<ProjectRow>()
            }.onFailure { Log.w(TAG, "getUserProjects failed: ${it.message}") }
                .getOrDefault(emptyList())
        }

        suspend fun getFeed(limit: Long = 20L): List<ProjectRow> {
            if (!isSupabaseAvailable) return emptyList()
            return runCatching {
                client.postgrest.from("projects").select {
                    order("created_at", Order.DESCENDING)
                    limit(limit)
                }.decodeList<ProjectRow>()
            }.onFailure { Log.w(TAG, "getFeed failed: ${it.message}") }
                .getOrDefault(emptyList())
        }

        /**
         * Fetch the full, real user list from the Supabase `users` table for the
         * developer dashboard. Returns an empty list when Supabase is unavailable.
         */
        suspend fun getAllUsers(): List<UserRow> {
            if (!isSupabaseAvailable) return emptyList()
            return runCatching {
                client.postgrest.from("users").select {
                    order("created_at", Order.DESCENDING)
                }.decodeList<UserRow>()
            }.onFailure { Log.w(TAG, "getAllUsers failed: ${it.message}") }
                .getOrDefault(emptyList())
        }

        suspend fun saveUser(id: String, name: String, email: String, type: String = "مجاني"): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                val row = UserRow(
                    id = id,
                    externalId = id,
                    name = name,
                    email = email,
                    type = type,
                    status = "active"
                )
                client.postgrest.from("users").upsert(
                    JsonArray(listOf(json.encodeToJsonElement(row)))
                ) {
                    onConflict = "id"
                }
                true
            }.onFailure { Log.w(TAG, "saveUser failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun updateUserRole(userId: String, newType: String): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.postgrest.from("users").update(
                    JsonObject(mapOf("type" to JsonPrimitive(newType)))
                ) {
                    filter { eq("id", userId) }
                }
                true
            }.onFailure { Log.w(TAG, "updateUserRole failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun updateUserSuspension(userId: String, isSuspended: Boolean): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                val strikes = if (isSuspended) 5 else 0
                val status = if (isSuspended) "suspended" else "active"
                client.postgrest.from("users").update(
                    JsonObject(mapOf(
                        "strikes" to JsonPrimitive(strikes),
                        "status" to JsonPrimitive(status)
                    ))
                ) {
                    filter { eq("id", userId) }
                }
                true
            }.onFailure { Log.w(TAG, "updateUserSuspension failed: ${it.message}") }
                .getOrDefault(false)
        }

        // ==========================================
        // أنماط الأسلوب الفني (Styles in Supabase)
        // ==========================================

        suspend fun saveStyleObject(
            id: String,
            name: String,
            userId: String,
            sourceVideo: String?,
            score: Int,
            tags: List<String>,
            payloadJson: String
        ): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                val payloadElem = runCatching { json.parseToJsonElement(payloadJson) }.getOrDefault(JsonObject(emptyMap()))
                val row = StyleRow(
                    id = id,
                    userId = userId,
                    name = name,
                    sourceVideo = sourceVideo,
                    payload = payloadElem,
                    score = score,
                    tags = JsonArray(tags.map { JsonPrimitive(it) })
                )
                client.postgrest.from("styles").upsert(
                    JsonArray(listOf(json.encodeToJsonElement(row)))
                ) {
                    onConflict = "id"
                }
                true
            }.onFailure { Log.w(TAG, "saveStyleObject failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun getStyleObjects(userId: String? = null): List<StyleRow> {
            if (!isSupabaseAvailable) return emptyList()
            return runCatching {
                client.postgrest.from("styles").select {
                    if (!userId.isNullOrBlank()) {
                        filter { eq("user_id", userId) }
                    }
                    order("created_at", Order.DESCENDING)
                }.decodeList<StyleRow>()
            }.onFailure { Log.w(TAG, "getStyleObjects failed: ${it.message}") }
                .getOrDefault(emptyList())
        }

        suspend fun deleteStyleObject(styleId: String): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.postgrest.from("styles").delete {
                    filter { eq("id", styleId) }
                }
                true
            }.onFailure { Log.w(TAG, "deleteStyleObject failed: ${it.message}") }
                .getOrDefault(false)
        }

        // ==========================================
        // أنماط الماستر (Master Styles in Supabase)
        // ==========================================

        suspend fun saveMasterStyle(
            id: String,
            name: String,
            description: String?,
            userId: String,
            compliance: Int,
            payloadJson: String
        ): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                val payloadElem = runCatching { json.parseToJsonElement(payloadJson) }.getOrDefault(JsonObject(emptyMap()))
                val row = MasterStyleRow(
                    id = id,
                    userId = userId,
                    name = name,
                    description = description,
                    payload = payloadElem,
                    compliance = compliance
                )
                client.postgrest.from("master_styles").upsert(
                    JsonArray(listOf(json.encodeToJsonElement(row)))
                ) {
                    onConflict = "id"
                }
                true
            }.onFailure { Log.w(TAG, "saveMasterStyle failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun getMasterStyles(userId: String? = null): List<MasterStyleRow> {
            if (!isSupabaseAvailable) return emptyList()
            return runCatching {
                client.postgrest.from("master_styles").select {
                    if (!userId.isNullOrBlank()) {
                        filter { eq("user_id", userId) }
                    }
                    order("created_at", Order.DESCENDING)
                }.decodeList<MasterStyleRow>()
            }.onFailure { Log.w(TAG, "getMasterStyles failed: ${it.message}") }
                .getOrDefault(emptyList())
        }

        suspend fun deleteMasterStyle(masterStyleId: String): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.postgrest.from("master_styles").delete {
                    filter { eq("id", masterStyleId) }
                }
                true
            }.onFailure { Log.w(TAG, "deleteMasterStyle failed: ${it.message}") }
                .getOrDefault(false)
        }

        // ==========================================
        // أكواد الهدايا والبرومو (Promo Codes in Supabase)
        // ==========================================

        suspend fun savePromoCode(code: String, type: String, value: Long): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                val normalized = code.uppercase(java.util.Locale.ROOT)
                val row = PromoCodeRow(
                    code = normalized,
                    type = type,
                    value = value,
                    isActive = true
                )
                client.postgrest.from("promo_codes").upsert(
                    JsonArray(listOf(json.encodeToJsonElement(row)))
                ) {
                    onConflict = "code"
                }
                true
            }.onFailure { Log.w(TAG, "savePromoCode failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun getValidPromoCodes(): List<PromoCodeRow> {
            if (!isSupabaseAvailable) return emptyList()
            return runCatching {
                client.postgrest.from("promo_codes").select {
                    filter { eq("is_active", true) }
                }.decodeList<PromoCodeRow>()
            }.onFailure { Log.w(TAG, "getValidPromoCodes failed: ${it.message}") }
                .getOrDefault(emptyList())
        }

        // ==========================================
        // سجلات الحارس والمشتريات (Guard & Purchases)
        // ==========================================

        suspend fun recordContentGuardCheck(
            id: String,
            userId: String,
            snippet: String,
            verdict: String,
            score: Int,
            reason: String
        ): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                val row = GuardLogRow(
                    id = id,
                    userId = userId,
                    snippet = snippet,
                    verdict = verdict,
                    score = score,
                    reason = reason
                )
                client.postgrest.from("content_guard_logs").insert(
                    JsonArray(listOf(json.encodeToJsonElement(row)))
                )
                true
            }.onFailure { Log.w(TAG, "recordContentGuardCheck failed: ${it.message}") }
                .getOrDefault(false)
        }

        suspend fun recordTransaction(
            id: String,
            userId: String,
            productId: String,
            purchaseToken: String,
            priceAmount: Double,
            currency: String = "USD"
        ): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                val row = TransactionRow(
                    id = id,
                    userId = userId,
                    productId = productId,
                    purchaseToken = purchaseToken,
                    priceAmount = priceAmount,
                    currency = currency
                )
                client.postgrest.from("transactions").insert(
                    JsonArray(listOf(json.encodeToJsonElement(row)))
                )
                true
            }.onFailure { Log.w(TAG, "recordTransaction failed: ${it.message}") }
                .getOrDefault(false)
        }

        /** Build a JSON object for jsonb columns from vararg pairs. */
        fun jsonObject(vararg pairs: Pair<String, Any?>): String = JsonObject(
            pairs.associate { (k, v) ->
                k to when (v) {
                    null -> JsonNull
                    is Number -> JsonPrimitive(v)
                    is Boolean -> JsonPrimitive(v)
                    else -> JsonPrimitive(v.toString())
                }
            }
        ).toString()

        /**
         * Fetch the active hadith preset catalog from the cloud. Ordered by
         * `display_order` so admins control the sequence. Returns an empty
         * list when Supabase is unavailable so callers can fall back to a
         * hard-coded list without crashing.
         */
        suspend fun getHadithPresets(): List<HadithRow> {
            if (!isSupabaseAvailable) return emptyList()
            return runCatching {
                client.postgrest.from("hadith_presets").select {
                    filter { eq("is_active", true) }
                    order("display_order", Order.ASCENDING)
                }.decodeList<HadithRow>()
            }.onFailure { Log.w(TAG, "getHadithPresets failed: ${it.message}") }
                .getOrDefault(emptyList())
        }

        /**
         * Fetch all active audio tracks uploaded / managed in Supabase.
         */
        suspend fun getAudioTracks(): List<AudioTrackRow> {
            if (!isSupabaseAvailable) return emptyList()
            return runCatching {
                client.postgrest.from("audio_tracks").select {
                    filter { eq("is_active", true) }
                    order("created_at", Order.DESCENDING)
                }.decodeList<AudioTrackRow>()
            }.onFailure { Log.w(TAG, "getAudioTracks failed: ${it.message}") }
                .getOrDefault(emptyList())
        }

        /**
         * Insert a new audio track record in Supabase.
         */
        suspend fun addAudioTrack(track: AudioTrackRow): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.postgrest.from("audio_tracks").insert(
                    JsonArray(listOf(json.encodeToJsonElement(track)))
                )
                true
            }.onFailure { Log.w(TAG, "addAudioTrack failed: ${it.message}") }
                .getOrDefault(false)
        }

        /**
         * Delete an audio track from Supabase.
         */
        suspend fun deleteAudioTrack(trackId: String): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.postgrest.from("audio_tracks").delete {
                    filter { eq("id", trackId) }
                }
                true
            }.onFailure { Log.w(TAG, "deleteAudioTrack failed: ${it.message}") }
                .getOrDefault(false)
        }

        /**
         * Update likes count for an audio track in Supabase.
         */
        suspend fun updateAudioTrackLikes(trackId: String, newCount: Int): Boolean {
            if (!isSupabaseAvailable) return false
            return runCatching {
                client.postgrest.from("audio_tracks").update(
                    { set("likes_count", newCount) }
                ) {
                    filter { eq("id", trackId) }
                }
                true
            }.onFailure { Log.w(TAG, "updateAudioTrackLikes failed: ${it.message}") }
                .getOrDefault(false)
        }
    }

    // ---------- Storage ----------

    object Storage {
        /**
         * Upload audio bytes to Supabase Storage bucket (default bucket: 'audio-tracks').
         * Returns the public URL of the uploaded audio file, or null if failed.
         */
        suspend fun uploadAudioFile(
            bucketName: String = "audio-tracks",
            fileName: String,
            data: ByteArray
        ): String? {
            if (!isSupabaseAvailable) return null
            return runCatching {
                val bucket = client.storage.from(bucketName)
                bucket.upload(fileName, data) {
                    upsert = true
                }
                bucket.publicUrl(fileName)
            }.onFailure { Log.w(TAG, "uploadAudioFile failed: ${it.message}") }
                .getOrNull()
        }
    }
}

/** Best-effort decode that returns an empty list instead of throwing. */
private suspend inline fun <reified T> io.github.jan.supabase.postgrest.result.PostgrestResult
    .decodeListOrEmpty(): List<T> = runCatching { decodeList<T>() }.getOrDefault(emptyList())
