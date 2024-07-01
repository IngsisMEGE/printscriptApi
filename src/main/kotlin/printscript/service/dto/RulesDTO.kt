package printscript.service.dto

data class RulesDTO(
    val name: String,
    var value: String,
)

data class SnippetWithRuleDTO(
    val snippetId: Long,
    val formatRules: List<RulesDTO>,
    val lintingRules: List<RulesDTO>,
)
