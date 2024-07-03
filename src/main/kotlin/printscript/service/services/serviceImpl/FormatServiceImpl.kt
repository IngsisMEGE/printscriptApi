package printscript.service.services.serviceImpl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.example.PrintScript
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import printscript.service.dto.*
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.FormatService
import printscript.service.services.interfaces.RuleService
import printscript.service.services.interfaces.SnippetManagerService
import printscript.service.services.interfaces.RuleManagerService
import printscript.service.utils.FileManagement
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class FormatServiceImpl(
    private val assetService: AssetService,
    private val ruleManagerService: RuleManagerService,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val snippetManagerService: SnippetManagerService,
) : FormatService {
    override fun format(
        snippetData: SnippetData,
        userData: Jwt,
    ): Mono<String> {
        return ruleManagerService.getLintingRules(userData).flatMap { lintingRules ->
            val lintingRulesFilePath = FileManagement.createLexerRuleFile(lintingRules)
            ruleManagerService.getFormatRules(userData).flatMap { formatRules ->
                val formatRulesFilePath = FileManagement.createTempFileWithContent(formatRules)
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
        snippetDataWithRules: FormatSnippetWithRulesDTO,
        userData: Jwt,
    ): Mono<String> {
        val formatRules = rulesToJSONString(snippetDataWithRules.formatRules)
        val formatRulesPath = FileManagement.createTempFileWithContent(formatRules)
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

    private fun formatSnippet(
        snippetId: Long,
        formatRulesFilePath: String,
        lintingRulesFilePath: String,
    ): Mono<String> {
        return assetService.getSnippet(snippetId)
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

    @Scheduled(fixedDelay = 1000)
    fun processFormatQueue() {
        val objectMapper = jacksonObjectMapper()
        val requestData = redisTemplate.opsForList().leftPop("snippet_formatting_queue")

        if (requestData != null) {
            val formatSnippetWithRulesDataRedis: FormatSnippetWithRulesRedisDTO =
                objectMapper.readValue(requestData.toString())
            val formatSnippetRules = formatSnippetWithRulesDataRedis.formatSnippetWithRules
            val userJWT = formatSnippetWithRulesDataRedis.userData
            val snippetID = formatSnippetRules.snippetId

            formatWithRules(formatSnippetRules, userJWT)
                .flatMap { formattedSnippet ->
                    assetService.saveSnippet(snippetID, formattedSnippet)
                        .then(Mono.just(formattedSnippet))
                }
                .map {
                    snippetManagerService.updateSnippetStatus(
                        StatusDTO(SnippetStatus.COMPLIANT, snippetID, userJWT.claims["email"].toString()),
                    )
                }
                .publishOn(Schedulers.boundedElastic())
                .doOnError {
                    snippetManagerService.updateSnippetStatus(
                        StatusDTO(SnippetStatus.NOT_COMPLIANT, snippetID, userJWT.claims["email"].toString()),
                    )
                }
                .subscribe()
        }
    }
}
