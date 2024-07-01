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
        return ruleService.getLintingRules(userData).flatMap { lintingRules ->
            val lintingRulesFilePath = createTempFileForRules(lintingRules)
            ruleService.getFormatRules(userData).flatMap { formatRules ->
                val formatRulesFilePath = createTempFileForRules(formatRules)
                formatSnippet(snippetData.snippetId, formatRulesFilePath, lintingRulesFilePath)
                    .doOnTerminate {
                        cleanupFile("formatterConfig.json")
                        cleanupFile(formatRulesFilePath)
                    }
            }
                .doOnTerminate {
                    cleanupFile("linterConfig.json")
                    cleanupFile(lintingRulesFilePath)
                }
        }.onErrorResume { e ->
            println("Error encountered: ${e.message}")
            Mono.error(Exception(e.message, e))
        }
    }

    override fun formatWithRules(
        snippetDataWithRules: SnippetWithRuleDTO,
        userData: Jwt,
    ): Mono<String> {
        val formatRules = rulesToJSONString(snippetDataWithRules.formatRules)
        val formatRulesPath = createTempFileForRules(formatRules)
        val lintingRulesPath = FileManagement.createLexerRuleFile(snippetDataWithRules.lintingRules)
        return formatSnippet(snippetDataWithRules.snippetId, formatRulesPath, lintingRulesPath)
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
        lintingRulesFilePath: String,
    ): Mono<String> {
        return assetService.getSnippet("snippets", snippetId)
            .flatMap { code ->
                formatCode(code, formatRulesFilePath, lintingRulesFilePath)
            }
    }

    private fun formatCode(
        code: String,
        formatRulesFilePath: String,
        lintingRulesFilePath: String,
    ): Mono<String> {
        val snippetFilePath = FileManagement.createTempFileWithContent(code)
        val printscript = PrintScript()

        return try {
            printscript.changeFormatterConfig(formatRulesFilePath)
            printscript.updateRegexRules(lintingRulesFilePath)
            val formatResult = printscript.format(snippetFilePath)
            Mono.just(formatResult)
        } catch (e: Exception) {
            Mono.error(e)
        } finally {
            cleanupFile("lexerRules.json")
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
