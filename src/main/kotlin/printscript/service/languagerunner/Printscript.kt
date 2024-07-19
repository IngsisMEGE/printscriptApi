package printscript.service.languagerunner

import org.example.PrintScript
import printscript.service.dto.*
import printscript.service.utils.FileManagement
import printscript.service.utils.GetLexerRules
import java.util.Queue

class Printscript(loadInput: (String) -> String) : LanguageRunner {
    private val printScript = PrintScript(loadInput)

    override fun executeSnippet(
        snippetPath: String,
        envs: Map<String, String>,
    ): String {
        try {
            printScript.updateRegexRules(GetLexerRules().getLexerRulesPath(Language.Printscript))
            val result = printScript.start(snippetPath, envs)
            cleanupFile(snippetPath)
            return result
        } catch (e: Exception) {
            cleanupFile(snippetPath)
            throw e
        }
    }

    override fun formatSnippet(
        snippetPath: String,
        formatRulePath: String,
    ): String {
        try {
            printScript.updateRegexRules(GetLexerRules().getLexerRulesPath(Language.Printscript))
            printScript.changeFormatterConfig(formatRulePath)
            val result = printScript.format(snippetPath)
            cleanupFile(snippetPath)
            if (formatRulePath != "src/main/resources/FormatterDefault.json") {
                cleanupFile(formatRulePath)
            }
            cleanupFile("formatterConfig.json")
            return result
        } catch (e: Exception) {
            cleanupFile(snippetPath)
            if (formatRulePath != "src/main/resources/FormatterDefault.json") {
                cleanupFile(formatRulePath)
            }
            cleanupFile("formatterConfig.json")
            cleanupFile("lexerRules.json")
            throw e
        }
    }

    override fun scaSnippet(
        snippetPath: String,
        scaRulePath: String,
    ): String {
        try {
            printScript.updateRegexRules(GetLexerRules().getLexerRulesPath(Language.Printscript))
            printScript.changeSCAConfig(scaRulePath)
            val result = printScript.analyze(snippetPath)
            cleanupFile(snippetPath)
            if (scaRulePath != "src/main/resources/SCADefault.json") {
                cleanupFile(scaRulePath)
            }
            cleanupFile("scaConfig.json")
            return result
        } catch (e: Exception) {
            cleanupFile(snippetPath)
            if (scaRulePath != "src/main/resources/SCADefault.json") {
                cleanupFile(scaRulePath)
            }
            cleanupFile("scaConfig.json")
            cleanupFile("lexerRules.json")
            throw e
        }
    }

    override fun getOutputs(): Queue<String> {
        return printScript.getOutputs()
    }

    private fun cleanupFile(filePath: String) {
        try {
            FileManagement.deleteTempFile(filePath)
        } catch (ignored: Exception) {
            println("Error deleting temp file: $filePath")
        }
    }
}
