package printscript.service.services.serviceImpl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    private val objectMapper: ObjectMapper,
) : FormatService {
    private val logger: Logger = LoggerFactory.getLogger(FormatServiceImpl::class.java)

    override fun format(
        snippetData: SnippetData,
        userData: Jwt,
    ): Mono<String> {
        logger.debug("Entering format with snippetId: ${snippetData.snippetId}")
        return ruleManagerService.getFormatRules(userData).flatMap { formatRules ->
            var formatRulesFilePath = "src/main/resources/FormatterDefault.json"
            if (formatRules.isNotEmpty()) {
                formatRulesFilePath = FileManagement.createFormatRuleFile(formatRules)
            }
            formatSnippet(snippetData.snippetId, formatRulesFilePath, snippetData.language)
        }
            .doOnSuccess {
                logger.info("Successfully formatted snippet with snippetId: ${snippetData.snippetId}")
            }
            .doOnError { error ->
                logger.error("Error formatting snippet with snippetId: ${snippetData.snippetId}", error)
            }
            .onErrorResume { e ->
                logger.error("Error encountered during formatting: ${e.message}", e)
                Mono.error(Exception(e.message, e))
            }
            .doFinally {
                logger.debug("Exiting format with snippetId: ${snippetData.snippetId}")
            }
    }

    override fun formatWithRules(
        snippetDataWithRules: FormatSnippetWithRulesDTO,
        userData: Jwt,
    ): Mono<String> {
        logger.debug("Entering formatWithRules with snippetId: ${snippetDataWithRules.snippetId}")
        val formatRulesPath = FileManagement.createFormatRuleFile(snippetDataWithRules.formatRules)
        return formatSnippet(snippetDataWithRules.snippetId, formatRulesPath, snippetDataWithRules.language)
            .doOnSuccess {
                logger.info("Successfully formatted snippet with rules for snippetId: ${snippetDataWithRules.snippetId}")
            }
            .doOnError { error ->
                logger.error("Error formatting snippet with rules for snippetId: ${snippetDataWithRules.snippetId}", error)
            }
            .onErrorResume { e ->
                logger.error("Error formatting snippet with rules: ${e.message}", e)
                Mono.error(Exception("Error formatting snippet with rules", e))
            }
            .doFinally {
                logger.debug("Exiting formatWithRules with snippetId: ${snippetDataWithRules.snippetId}")
            }
    }

    override fun formatWithSnippet(
        snippet: SnippetDataWithSnippet,
        userData: Jwt,
    ): Mono<String> {
        logger.debug("Entering formatWithSnippet with snippet: ${snippet.snippet}")
        return ruleManagerService.getFormatRules(userData).flatMap { formatRules ->
            var formatRulesFilePath = "src/main/resources/FormatterDefault.json"
            if (formatRules.isNotEmpty()) {
                formatRulesFilePath = FileManagement.createFormatRuleFile(formatRules)
            }
            val snippetPath = FileManagement.createTempFileWithContent(snippet.snippet)

            val languageRunner =
                LanguageRunnerProvider.getLanguageRunner(snippet.language) {
                    loadInput(it)
                }
            val formatResult = languageRunner.formatSnippet(snippetPath, formatRulesFilePath)
            Mono.just(formatResult)
        }
    }

    private fun formatSnippet(
        snippetId: Long,
        formatRulesFilePath: String,
        language: Language,
    ): Mono<String> {
        logger.debug("Entering formatSnippet with snippetId: $snippetId")
        return getSnippet(snippetId)
            .flatMap { snippetPath ->
                val languageRunner =
                    LanguageRunnerProvider.getLanguageRunner(language) {
                        loadInput(it)
                    }

                val formatResult = languageRunner.formatSnippet(snippetPath, formatRulesFilePath)
                Mono.just(formatResult)
            }
            .doOnSuccess {
                logger.info("Successfully formatted snippet with id: $snippetId")
            }
            .doOnError { error ->
                logger.error("Error formatting snippet with id: $snippetId", error)
            }
            .doFinally {
                logger.debug("Exiting formatSnippet with snippetId: $snippetId")
            }
    }

    private fun getSnippet(snippetId: Long): Mono<String> {
        logger.debug("Entering getSnippet with snippetId: $snippetId")
        return assetService.getSnippet(snippetId).flatMap { snippet ->
            logger.info("Successfully retrieved snippet with id: $snippetId")
            Mono.just(FileManagement.createTempFileWithContent(snippet))
        }.doOnError { error ->
            logger.error("Error retrieving snippet with id: $snippetId", error)
        }.doFinally {
            logger.debug("Exiting getSnippet with snippetId: $snippetId")
        }
    }

    @Scheduled(fixedDelay = 1000)
    fun processFormatQueue() {
        logger.debug("Entering processFormatQueue")
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
                    logger.error("Error processing format queue for snippetId: $snippetID")
                }
                .doOnSuccess {
                    logger.info("Successfully processed format queue for snippetId: $snippetID")
                }
                .subscribe()
        }
        logger.debug("Exiting processFormatQueue")
    }

    private fun loadInput(message: String): String {
        return "input"
    }
}
