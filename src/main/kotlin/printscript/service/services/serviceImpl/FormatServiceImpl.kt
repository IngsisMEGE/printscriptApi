package printscript.service.services.serviceImpl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import printscript.service.dto.*
import printscript.service.languagerunner.LanguageRunnerProvider
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.FormatService
import printscript.service.services.interfaces.RuleManagerService
import printscript.service.services.interfaces.SnippetManagerService
import printscript.service.utils.FileManagement
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
@EnableScheduling
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
        return ruleManagerService.getFormatRules(userData).flatMap { formatRules ->
            var formatRulesFilePath = "src/main/resources/FormatterDefault.json"
            if (formatRules.isNotEmpty()) {
                formatRulesFilePath = FileManagement.createFormatRuleFile(formatRules)
            }
            formatSnippet(snippetData.snippetId, formatRulesFilePath, snippetData.language)
        }
            .onErrorResume { e ->
                println("Error encountered: ${e.message}")
                Mono.error(Exception(e.message, e))
            }
    }

    override fun formatWithRules(
        snippetDataWithRules: FormatSnippetWithRulesDTO,
        userData: Jwt,
    ): Mono<String> {
        val formatRulesPath = FileManagement.createFormatRuleFile(snippetDataWithRules.formatRules)
        return formatSnippet(snippetDataWithRules.snippetId, formatRulesPath, snippetDataWithRules.language)
            .onErrorResume { e -> Mono.error(Exception("Error formatting snippet with rules", e)) }
    }

    private fun formatSnippet(
        snippetId: Long,
        formatRulesFilePath: String,
        language: Language,
    ): Mono<String> {
        return getSnippet(snippetId)
            .flatMap { snippetPath ->
                val languageRunner =
                    LanguageRunnerProvider.getLanguageRunner(language) {
                        loadInput(it)
                    }

                val formatResult = languageRunner.formatSnippet(snippetPath, formatRulesFilePath)
                Mono.just(formatResult)
            }
    }

    private fun getSnippet(snippetId: Long): Mono<String> {
        return assetService.getSnippet(snippetId).flatMap { snippet ->
            Mono.just(FileManagement.createTempFileWithContent(snippet))
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

    private fun loadInput(message: String): String {
        return "input"
    }
}
