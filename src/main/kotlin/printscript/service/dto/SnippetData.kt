package printscript.service.dto

import org.springframework.security.oauth2.jwt.Jwt

data class SnippetData(
    val snippetId: Long,
    val language: Language,
)

enum class SnippetStatus {
    COMPLIANT,
    NOT_COMPLIANT,
}

data class StatusDTO(
    val status: SnippetStatus,
    val id: Long,
    val ownerEmail: String,
)

data class SnippetDataInputs(
    val language: Language,
    val snippetId: Long,
    val inputs: List<String>,
)

data class SnippetDataLiveResponse(
    val output: String,
    val doesItNeedInput: Boolean,
)

data class SnippetDataTest(
    val language: Language,
    val snippetId: Long,
    val inputs: List<String>,
    val envs: Map<String, String>,
)

enum class Language {
    Printscript,
    Java,
    Python,
    Go,
}

data class SnippetDataWithSnippet(
    val snippet: String,
    val language: Language,
)

data class SCASnippetRedisDTO(
    val snippetId: Long,
    val userData: Jwt,
    val language: Language,
)
