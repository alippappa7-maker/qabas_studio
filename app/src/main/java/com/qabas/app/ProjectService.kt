package com.qabas.app

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectService(private val context: Context) {
    @Immutable
    data class Project(
        val id: String = "",
        val title: String = "",
        val idea: String = "",
        val analysis: String = "",
        val resources: String = "",
        val settings: String = "",
        val script: String = "",
        val finalVideoPath: String = "",
        val cost: Double = 0.0,
        val createdAt: Long = 0L,
        val updatedAt: Long = 0L,
        val status: String = "",
        val lastScreen: String = ""
    )

    private val repository: ProjectRepository by lazy {
        ProjectRepository(AppDatabase.getDatabase(context).projectDao())
    }

    /** قائمة سريعة من Room فقط — لا شبكة */
    suspend fun getLocalProjects(): List<Project> = withContext(Dispatchers.IO) {
        repository.getAllProjectsOnce()
            .map { it.toProject() }
            .sortedByDescending { it.updatedAt }
    }

    /**
     * الافتراضي: محلي فقط.
     * syncCloud = true عند شاشة التحميل / سحب للتحديث فقط.
     * يعتمد على Supabase كخيار سحابي أساسي، مع الرجوع التلقائي لـ Firebase إذا لزم الأمر.
     */
    suspend fun getAllProjects(syncCloud: Boolean = false): List<Project> =
        withContext(Dispatchers.IO) {
            val localProjects = repository.getAllProjectsOnce().map { it.toProject() }

            if (!syncCloud) {
                return@withContext localProjects.sortedByDescending { it.updatedAt }
            }

            // 1. الأولوية الأولى: Supabase
            if (SupabaseServices.isSupabaseAvailable) {
                try {
                    val supabaseRows = SupabaseServices.Database.getUserProjects()
                    if (supabaseRows.isNotEmpty()) {
                        val merged = LinkedHashMap<String, Project>(localProjects.size + supabaseRows.size)
                        localProjects.forEach { merged[it.id] = it }

                        val toUpsert = ArrayList<ProjectEntity>()
                        supabaseRows.forEach { row ->
                            val meta = (row.metadata as? kotlinx.serialization.json.JsonObject)
                            val ideaStr = meta?.get("idea")?.toString()?.trim('"') ?: (row.description ?: "")
                            val scriptStr = meta?.get("script")?.toString()?.trim('"') ?: ""
                            val proj = Project(
                                id = row.id,
                                title = row.title,
                                idea = ideaStr,
                                analysis = meta?.get("analysis")?.toString()?.trim('"') ?: "",
                                resources = meta?.get("resources")?.toString()?.trim('"') ?: "",
                                settings = meta?.get("settings")?.toString()?.trim('"') ?: "",
                                script = scriptStr,
                                finalVideoPath = meta?.get("finalVideoPath")?.toString()?.trim('"') ?: "",
                                cost = row.cost,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis(),
                                status = row.status,
                                lastScreen = meta?.get("lastScreen")?.toString()?.trim('"') ?: ""
                            )
                            val existing = merged[proj.id]
                            if (existing == null || proj.updatedAt >= existing.updatedAt) {
                                merged[proj.id] = proj
                                toUpsert.add(proj.toEntity())
                            }
                        }
                        if (toUpsert.isNotEmpty()) {
                            repository.insertAll(toUpsert)
                        }
                        return@withContext merged.values.sortedByDescending { it.updatedAt }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("ProjectService", "Supabase sync error, checking Firebase: ${e.message}")
                }
            }

            // 2. الرجوع البديل: Firebase (إن وجد)
            if (CloudServices.isFirebaseInitialized && CloudServices.Auth.getCurrentUserId() != null) {
                try {
                    val cloudProjects = CloudServices.Database.getUserProjects()
                    val merged = LinkedHashMap<String, Project>(localProjects.size + cloudProjects.size)
                    localProjects.forEach { merged[it.id] = it }

                    val toUpsert = ArrayList<ProjectEntity>()
                    cloudProjects.forEach { cloudProj ->
                        val localProj = merged[cloudProj.id]
                        if (localProj == null || cloudProj.updatedAt > localProj.updatedAt) {
                            merged[cloudProj.id] = cloudProj
                            toUpsert.add(cloudProj.toEntity())
                        }
                    }
                    if (toUpsert.isNotEmpty()) {
                        repository.insertAll(toUpsert)
                    }
                    return@withContext merged.values.sortedByDescending { it.updatedAt }
                } catch (_: Exception) { }
            }

            localProjects.sortedByDescending { it.updatedAt }
        }

    suspend fun syncFromCloud(): List<Project> = getAllProjects(syncCloud = true)

    suspend fun getProject(id: String): Project? = withContext(Dispatchers.IO) {
        repository.getProject(id)?.toProject()
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        repository.insert(project.toEntity())

        // حفظ سحابي أساسي في Supabase
        if (SupabaseServices.isSupabaseAvailable) {
            runCatching {
                val metadata = mapOf(
                    "idea" to project.idea,
                    "analysis" to project.analysis,
                    "resources" to project.resources,
                    "settings" to project.settings,
                    "script" to project.script,
                    "finalVideoPath" to project.finalVideoPath,
                    "lastScreen" to project.lastScreen
                )
                SupabaseServices.Database.saveFullProject(
                    id = project.id,
                    title = project.title,
                    description = project.idea.take(200),
                    status = project.status.ifBlank { "draft" },
                    cost = project.cost,
                    tags = listOf("qabas_project"),
                    metadata = metadata
                )
            }
        }

        // حفظ تكراري إضافي في Firebase إذا كان متوفراً
        if (CloudServices.isFirebaseInitialized && CloudServices.Auth.getCurrentUserId() != null) {
            runCatching { CloudServices.Database.saveProjectToCloud(project) }
        }

        try {
            if (project.analysis.isNotEmpty()) {
                val obj = org.json.JSONObject(project.analysis)
                val style = obj.optString("suggestedStyle")
                if (style.isNotBlank()) TasteManager.registerPreference(context, "STYLE", style, 1)
                val tone = obj.optString("tone")
                if (tone.isNotBlank()) TasteManager.registerPreference(context, "TONE", tone, 1)
                val keywords = obj.optJSONArray("keywords")
                if (keywords != null && keywords.length() > 0) {
                    for (i in 0 until keywords.length()) {
                        TasteManager.registerPreference(context, "TOPIC", keywords.getString(i), 1)
                    }
                }
            }
        } catch (_: Exception) { }
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        repository.delete(id)
        if (SupabaseServices.isSupabaseAvailable) {
            runCatching { SupabaseServices.Database.deleteProject(id) }
        }
        if (CloudServices.isFirebaseInitialized && CloudServices.Auth.getCurrentUserId() != null) {
            runCatching { CloudServices.Database.deleteProjectFromCloud(id) }
        }
    }

    private fun ProjectEntity.toProject() = Project(
        id = id, title = title, idea = idea, analysis = analysis,
        resources = resources, settings = settings, script = script,
        finalVideoPath = finalVideoPath, cost = cost,
        createdAt = createdAt, updatedAt = updatedAt,
        status = status, lastScreen = lastScreen
    )

    private fun Project.toEntity() = ProjectEntity(
        id = id, title = title, idea = idea, analysis = analysis,
        resources = resources, settings = settings, script = script,
        finalVideoPath = finalVideoPath, cost = cost,
        createdAt = createdAt, updatedAt = updatedAt,
        status = status, lastScreen = lastScreen
    )
}
