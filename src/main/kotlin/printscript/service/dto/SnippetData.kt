package printscript.service.dto

data class SnippetData(
    val snippetId: Long,
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
