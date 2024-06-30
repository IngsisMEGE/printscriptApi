package printscript.service.dto

data class RulesDTO(
    val name: String,
    val value: String,
)

data class SnippetWithRuleDTO(
    val snippetId: Long,
    val rules: List<RulesDTO>,
)
