package printscript.service.services.serviceImpl

import org.example.PrintScript
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import printscript.service.dto.RulesDTO
import printscript.service.dto.SnippetData
import printscript.service.dto.SnippetWithRuleDTO
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.FormatService
import printscript.service.services.interfaces.RuleService
import printscript.service.utils.FileManagement
import reactor.core.publisher.Mono

@Service
class FormatServiceImpl(
    private val assetService: AssetService,
    private val ruleService: RuleService,
) : FormatService {
    override fun format(
        snippetData: SnippetData,
        userData: Jwt,
    ): Mono<String> {
        return ruleService.getFormatRules()
            .flatMap { formatRules ->
                val formatRulesFilePath = createTempFileForRules(formatRules)
                formatSnippet(snippetData.snippetId, formatRulesFilePath)
                    .doOnTerminate {
                        cleanupFile("formatterConfig.json")
                        cleanupFile(formatRulesFilePath)
                    }
            }
            .onErrorResume { e ->
                println("Error encountered: ${e.message}")
                Mono.error(Exception(e.message, e))
            }
    }

    override fun formatWithRules(
        snippetDataWithRules: SnippetWithRuleDTO,
        userData: Jwt,
    ): Mono<String> {
        val formatRules = rulesToJSONString(snippetDataWithRules.rules)
        val formatRulesPath = createTempFileForRules(formatRules)
        return formatSnippet(snippetDataWithRules.snippetId, formatRulesPath)
            .doOnTerminate {
                cleanupFile("formatterConfig.json")
                cleanupFile(formatRulesPath)
            }
            .onErrorResume { e -> Mono.error(Exception("Error formatting snippet with rules", e)) }
    }

    private fun rulesToJSONString(rules: List<RulesDTO>): String {
        val rulesMap = mutableMapOf<String, String>()
        rules.forEach { rule ->
            rulesMap[rule.name] = rule.value
        }
        return rulesMap.toString()
    }

    private fun createTempFileForRules(formatRules: String): String {
        try {
            return FileManagement.createTempFileWithContent(formatRules)
        } catch (e: Exception) {
            throw Exception("Error creating temp file for format rules", e)
        }
    }

    private fun formatSnippet(
        snippetId: Long,
        formatRulesFilePath: String,
    ): Mono<String> {
        return assetService.getSnippet("snippets", snippetId)
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
