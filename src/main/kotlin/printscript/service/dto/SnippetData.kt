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

data class SnippetDataInputs(
    val snippetId: Long,
    val inputs: List<String>,
)

data class SnippetDataLiveResponse(
    val output: String,
    val doesItNeedInput: Boolean,
)

data class SnippetDataTest(
    val snippetId: Long,
    val inputs: List<String>,
    val envs: Map<String, String>,
)
