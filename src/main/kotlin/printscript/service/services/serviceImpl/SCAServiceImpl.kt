package printscript.service.services.serviceImpl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import printscript.service.dto.*
import printscript.service.languagerunner.LanguageRunner
import printscript.service.languagerunner.LanguageRunnerProvider
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.RuleManagerService
import printscript.service.services.interfaces.SCAService
import printscript.service.services.interfaces.SnippetManagerService
import printscript.service.utils.FileManagement
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
@EnableScheduling
class SCAServiceImpl(
    private val assetService: AssetService,
    private val ruleManagerService: RuleManagerService,
    private val redisTemplate: RedisTemplate<String, Any>,
    private val snippetManagerService: SnippetManagerService,
    private val objectMapper: ObjectMapper,
) : SCAService {
    private val logger: Logger = LoggerFactory.getLogger(SCAServiceImpl::class.java)

    override fun analyzeCode(
        snippet: SnippetData,
        userData: Jwt,
    ): Mono<String> {
        return getSnippet(snippet.snippetId).flatMap { snippetFile ->
            getSCARules(userData).flatMap { scaRuleFile ->

                val result = analyze(snippetFile, scaRuleFile, snippet.language)

                cleanupFile(snippetFile)
                cleanupFile(scaRuleFile)

                Mono.just(result)
            }
        }
    }

    override fun analyzeCodeWithRules(
        snippetRule: SCASnippetWithRulesDTO,
        userData: Jwt,
    ): Mono<String> {
        val scaRules = rulesToJSONString(snippetRule.scaRules)
        val scaRulesPath = FileManagement.createTempFileWithContent(scaRules)
        return getSnippet(snippetRule.snippetId).flatMap { snippetFile ->
            val result = analyze(snippetFile, scaRulesPath, snippetRule.language)
            cleanupFile(snippetFile)
            cleanupFile(scaRulesPath)
            Mono.just(result)
        }
    }

    private fun rulesToJSONString(rules: List<RulesDTO>): String {
        val rulesMap = mutableMapOf<String, String>()
        rules.forEach { rule ->
            rulesMap[rule.name] = rule.value
        }
        return rulesMap.toString()
    }

    private fun getSnippet(snippetId: Long): Mono<String> {
        return assetService.getSnippet(snippetId).flatMap { snippet ->
            Mono.just(FileManagement.createTempFileWithContent(snippet))
        }
    }

    private fun getSCARules(userData: Jwt): Mono<String> {
        return ruleManagerService.getSCARules(userData).flatMap { rules ->
            Mono.just(FileManagement.creteSCARuleFile(rules))
        }
    }

    private fun analyze(
        snippetPath: String,
        scaRulePath: String,
        language: Language,
    ): String {
        val languageRunner: LanguageRunner = LanguageRunnerProvider.getLanguageRunner(language) { message(it) }

        return languageRunner.scaSnippet(snippetPath, scaRulePath)
    }

    private fun message(m: String): String {
        return "hello"
    }

    private fun cleanupFile(filePath: String) {
        try {
            FileManagement.deleteTempFile(filePath)
        } catch (ignored: Exception) {
            println("Error deleting temp file: $filePath")
        }
    }

    @Scheduled(fixedDelay = 1000)
    fun processQueueSCA() {
        val objectMapper = jacksonObjectMapper()
        val requestData = redisTemplate.opsForList().leftPop("snippet_sca_queue")

        if (requestData != null) {
            val scaSnippetWithRulesDataRedis: SCASnippetWithRulesRedisDTO = objectMapper.readValue(requestData.toString())
            val scaSnippetRules = scaSnippetWithRulesDataRedis.scaSnippet
            val userJWT = scaSnippetWithRulesDataRedis.userData
            val snippetId = scaSnippetRules.snippetId

            analyzeCodeWithRules(scaSnippetRules, userJWT)
                .map {
                    snippetManagerService.updateSnippetStatus(
                        StatusDTO(SnippetStatus.COMPLIANT, snippetId, userJWT.claims["email"].toString()),
                    )
                }
                .publishOn(Schedulers.boundedElastic())
                .doOnError {
                    snippetManagerService.updateSnippetStatus(
                        StatusDTO(SnippetStatus.NOT_COMPLIANT, snippetId, userJWT.claims["email"].toString()),
                    )
                }
                .subscribe()
        }
    }

    @Scheduled(fixedDelay = 1000)
    fun processUniqueQueueSCA() {
        val requestData = redisTemplate.opsForList().leftPop("snippet_sca_unique_queue")

        if (requestData != null) {
            logger.debug("Processing SCA for snippet")
            val scaSnippetRedis: SCASnippetRedisDTO = objectMapper.readValue(requestData.toString())
            val userJWT = scaSnippetRedis.userData
            val snippetId = scaSnippetRedis.snippetId
            val language = scaSnippetRedis.language
            logger.debug("Processing SCA for snippet with id: $snippetId")

            analyzeCode(
                SnippetData(snippetId, language),
                userJWT,
            )
                .map {
                    logger.info("SCA for: $snippetId has been processed")
                    println("SCA for: $snippetId has been processed")
                    snippetManagerService.updateSnippetStatus(
                        StatusDTO(SnippetStatus.COMPLIANT, snippetId, userJWT.claims["email"].toString()),
                    )
                }
                .publishOn(Schedulers.boundedElastic())
                .doOnError {
                    snippetManagerService.updateSnippetStatus(
                        StatusDTO(SnippetStatus.NOT_COMPLIANT, snippetId, userJWT.claims["email"].toString()),
                    )
                }
                .subscribe()
        }
    }
}
