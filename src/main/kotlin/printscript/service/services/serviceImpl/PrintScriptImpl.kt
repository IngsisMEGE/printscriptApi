package printscript.service.services.serviceImpl

import org.example.PrintScript
import org.springframework.stereotype.Service
import printscript.service.dto.SnippetData
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.PrintScriptService
import printscript.service.services.interfaces.RuleService
import printscript.service.utils.FileManagement
import reactor.core.publisher.Mono

@Service
class PrintScriptImpl(
    private val assetService: AssetService,
    private val ruleService: RuleService,
) : PrintScriptService {
    override fun format(snippetData: SnippetData): Mono<String> {
        return ruleService.getFormatRules()
            .flatMap { formatRules ->
                val formatRulesFilePath = createTempFileForRules(formatRules)
                formatSnippet(snippetData, formatRulesFilePath)
                    .doFinally { cleanupFile(formatRulesFilePath) }
            }
            .onErrorResume { e -> Mono.error(Exception("Error getting format rules", e)) }
    }

    private fun createTempFileForRules(formatRules: String): String {
        try {
            return FileManagement.createTempFileWithContent(formatRules)
        } catch (e: Exception) {
            throw Exception("Error creating temp file for format rules", e)
        }
    }

    private fun formatSnippet(
        snippetData: SnippetData,
        formatRulesFilePath: String,
    ): Mono<String> {
        return assetService.getSnippet("snippets", snippetData.snippetId)
            .flatMap { code ->
                formatCode(code, formatRulesFilePath)
            }
    }

    private fun formatCode(
        code: String,
        formatRulesFilePath: String,
    ): Mono<String> {
        val snippetFilePath = FileManagement.createTempFileWithContent(code)
        val printscript = PrintScript()

        return try {
            printscript.changeFormatterConfig(formatRulesFilePath)
            val formatResult = printscript.format(snippetFilePath)
            Mono.just(formatResult)
        } catch (e: Exception) {
            Mono.error(e)
        } finally {
            cleanupFile(snippetFilePath)
        }
    }

    private fun cleanupFile(filePath: String) {
        try {
            FileManagement.deleteTempFile(filePath)
        } catch (ignored: Exception) {
            println("Error deleting temp file: $filePath")
        }
    }
}
