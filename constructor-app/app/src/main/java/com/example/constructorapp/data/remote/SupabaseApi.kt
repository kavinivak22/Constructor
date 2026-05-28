package com.example.constructorapp.data.remote

import retrofit2.Response
import retrofit2.http.*

interface SupabaseApi {
    // Auth Endpoints
    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    // Projects Endpoints
    @GET("rest/v1/projects")
    suspend fun getProjects(
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*"
    ): Response<List<ProjectDto>>

    // Daily Worklogs
    @GET("rest/v1/daily_worklogs")
    suspend fun getDailyWorklogs(
        @Header("Authorization") authHeader: String,
        @Query("project_id") projectIdFilter: String, // e.g. eq.UUID
        @Query("select") select: String = "*"
    ): Response<List<DailyWorklogDto>>

    @POST("rest/v1/daily_worklogs")
    @Headers("Prefer: return=representation")
    suspend fun createDailyWorklog(
        @Header("Authorization") authHeader: String,
        @Body worklog: DailyWorklogDto
    ): Response<List<DailyWorklogDto>>

    // Labor Entries
    @GET("rest/v1/worklog_labor_entries")
    suspend fun getLaborEntries(
        @Header("Authorization") authHeader: String,
        @Query("worklog_id") worklogIdFilter: String, // e.g. eq.UUID
        @Query("select") select: String = "*"
    ): Response<List<LaborEntryDto>>

    @POST("rest/v1/worklog_labor_entries")
    @Headers("Prefer: return=representation")
    suspend fun createLaborEntry(
        @Header("Authorization") authHeader: String,
        @Body entry: LaborEntryDto
    ): Response<List<LaborEntryDto>>

    // Worker Counts
    @POST("rest/v1/worklog_worker_counts")
    @Headers("Prefer: return=representation")
    suspend fun createWorkerCount(
        @Header("Authorization") authHeader: String,
        @Body workerCount: WorkerCountDto
    ): Response<List<WorkerCountDto>>

    // Worklog Materials
    @POST("rest/v1/worklog_materials")
    @Headers("Prefer: return=representation")
    suspend fun createWorklogMaterial(
        @Header("Authorization") authHeader: String,
        @Body material: WorklogMaterialDto
    ): Response<List<WorklogMaterialDto>>

    // Project Materials
    @GET("rest/v1/project_materials")
    suspend fun getProjectMaterials(
        @Header("Authorization") authHeader: String,
        @Query("project_id") projectIdFilter: String, // e.g. eq.UUID
        @Query("select") select: String = "*"
    ): Response<List<ProjectMaterialDto>>

    // Personal Notes / Reminders
    @GET("rest/v1/personal_notes")
    suspend fun getPersonalNotes(
        @Header("Authorization") authHeader: String,
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<PersonalNoteDto>>

    @POST("rest/v1/personal_notes")
    @Headers("Prefer: return=representation")
    suspend fun createPersonalNote(
        @Header("Authorization") authHeader: String,
        @Body note: PersonalNoteDto
    ): Response<List<PersonalNoteDto>>

    @PATCH("rest/v1/personal_notes")
    @Headers("Prefer: return=representation")
    suspend fun updatePersonalNote(
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String, // e.g. eq.UUID
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PersonalNoteDto>>

    @DELETE("rest/v1/personal_notes")
    suspend fun deletePersonalNote(
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String // e.g. eq.UUID
    ): Response<Unit>
}
