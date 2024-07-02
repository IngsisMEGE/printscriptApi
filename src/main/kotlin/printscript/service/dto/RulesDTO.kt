package printscript.service.dto

data class RulesDTO(
    val name: String,
    var value: String,
)

data class FormatSnippetWithRulesDTO(
    val snippetId: Long,
    val formatRules: List<RulesDTO>,
    val lintingRules: List<RulesDTO>,
)

data class SCASnippetWithRulesDTO(
    val snippetId: Long,
    val scaRules: List<RulesDTO>,
    val lintingRules: List<RulesDTO>,
)
