package com.example.notesfrontend

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
// PUBLIC_INTERFACE
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

// PUBLIC_INTERFACE
/**
 * A service for interacting with notes stored in Supabase via its REST API.
 * Supabase credentials must be supplied from secure storage.
 */
class SupabaseNotesService(
    private val supabaseUrl: String,
    private val supabaseKey: String
) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private fun getAuthHeaders(): Map<String, String> = mapOf(
        "apikey" to supabaseKey,
        "Authorization" to "Bearer $supabaseKey"
    )

    // PUBLIC_INTERFACE
    /**
     * Fetch all notes from Supabase notes table.
     */
    suspend fun fetchNotes(): List<SupabaseNote> {
        val url = "$supabaseUrl/rest/v1/notes?select=*"
        val response: HttpResponse = client.get(url) {
            headers { getAuthHeaders().forEach { (k, v) -> append(k, v) } }
        }
        if (response.status.value in 200..299) {
            return Json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(SupabaseNote.serializer()),
                response.bodyAsText()
            )
        }
        return emptyList()
    }
    // Add POST/PUT/DELETE methods as needed
}

// PUBLIC_INTERFACE
@Serializable
data class SupabaseNote(
    val id: Long,
    val title: String,
    val content: String,
    val timestamp: Long
)
