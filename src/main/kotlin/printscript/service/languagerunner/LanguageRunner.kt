package printscript.service.languagerunner

import java.util.Queue

interface LanguageRunner {
    fun executeSnippet(
        snippetPath: String,
        envs: Map<String, String>,
    ): String

    fun formatSnippet(
        snippetPath: String,
        formatRulePath: String,
    ): String

    fun scaSnippet(
        snippetPath: String,
        scaRulePath: String,
    ): String

    fun getOutputs(): Queue<String>
}
