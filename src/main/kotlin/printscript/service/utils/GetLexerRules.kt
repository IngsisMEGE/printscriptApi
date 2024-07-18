package printscript.service.utils

import printscript.service.dto.*

class GetLexerRules() {
    private val languageLexerRules: Map<Language, String> =
        mapOf(
            Language.Printscript to "src/main/resources/LexerRules/PrintscriptLexerRules.json",
        )

    fun getLexerRulesPath(language: Language): String {
        return languageLexerRules[language] ?: throw Exception("Language not supported")
    }
}
