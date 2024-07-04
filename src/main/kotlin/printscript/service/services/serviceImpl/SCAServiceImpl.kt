package printscript.service.services.serviceImpl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.example.PrintScript
import org.jetbrains.annotations.Async.Schedule
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import printscript.service.dto.*
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.RuleManagerService
import printscript.service.services.interfaces.SCAService
import printscript.service.services.interfaces.SnippetManagerService
import printscript.service.utils.FileManagement
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class SCAServiceImpl(private val assetService: AssetService, private val ruleManagerService: RuleManagerService, private val redisTemplate: RedisTemplate<String, Any>, private val snippetManagerService: SnippetManagerService, ) : SCAService {
    override fun analyzeCode(
        snippet: SnippetData,
        userData: Jwt,
    ): Mono<String> {
        return getSnippet(snippet.snippetId).flatMap { snippetFile ->
            getSCARules(userData).flatMap { scaRuleFile ->
                getLintingRules(userData).flatMap { lintingRulesFile ->
                    Mono.just(analyze(snippetFile, scaRuleFile, lintingRulesFile))
                }
            }
        }
    }

    override fun analyzeCodeWithRules(
        snippetRule: SCASnippetWithRulesDTO,
        userData: Jwt,
    ): Mono<String> {
        val scaRules = rulesToJSONString(snippetRule.scaRules)
        val scaRulesPath = FileManagement.createTempFileWithContent(scaRules)
        val lintingRulesPath = FileManagement.createLexerRuleFile(snippetRule.lintingRules)
        return getSnippet(snippetRule.snippetId).flatMap { snippetFile ->
            Mono.just(analyze(snippetFile, scaRulesPath, lintingRulesPath))
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

    private fun getLintingRules(userData: Jwt): Mono<String> {
        return ruleManagerService.getLintingRules(userData).flatMap { rules ->
            Mono.just(FileManagement.createLexerRuleFile(rules))
        }
    }

    private fun analyze(
        snippetPath: String,
        scaRulePath: String,
        lintingRulesPath: String,
    ): String {
        val printScript = PrintScript()
        printScript.changeSCAConfig(scaRulePath)
        printScript.updateRegexRules(lintingRulesPath)
        val result = printScript.analyze(snippetPath)
        FileManagement.deleteTempFile(snippetPath)
        FileManagement.deleteTempFile("scaConfig.json")
        FileManagement.deleteTempFile(scaRulePath)
        FileManagement.deleteTempFile("lexerRules.json")
        FileManagement.deleteTempFile(lintingRulesPath)
        return result
    }

    @Scheduled(fixedDelay = 1000)
    fun processQueueSCA() {
        val objectMapper = jacksonObjectMapper()
        val requestData = redisTemplate.opsForList().leftPop("snippet_sca_queue")

        if (requestData != null) {
            val scaSnippetWithRulesDataRedis : SCASnippetWithRulesRedisDTO = objectMapper.readValue(requestData.toString())
            val scaSnippetRules = scaSnippetWithRulesDataRedis.scaSnippet
            val userJWT = scaSnippetWithRulesDataRedis.userData
            val snippetId = scaSnippetRules.snippetId

            analyzeCodeWithRules(scaSnippetRules , userJWT)
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
}
