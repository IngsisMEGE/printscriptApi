package printscript.service.services.serviceImpl

import org.example.PrintScript
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import printscript.service.dto.SnippetData
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.ExecuteService
import printscript.service.services.interfaces.RuleService
import printscript.service.utils.FileManagement
import reactor.core.publisher.Mono

@Service
class ExecuteServiceImpl(private val assetService: AssetService, private val ruleService: RuleService) : ExecuteService {
    override fun executeSnippet(
        snippet: SnippetData,
        userData: Jwt,
    ): Mono<String> {
        return getSnippet(snippet.snippetId).flatMap { snippetFile ->
            getLintingRules(userData).flatMap { rulesFile ->
                Mono.just(execute(snippetFile, rulesFile))
            }
        }
    }

    private fun getSnippet(snippetId: Long): Mono<String> {
        return assetService.getSnippet(snippetId).flatMap { snippet ->
            Mono.just(FileManagement.createTempFileWithContent(snippet))
        }
    }

    private fun getLintingRules(userData: Jwt): Mono<String> {
        return ruleService.getLintingRules(userData).flatMap { rules ->
            Mono.just(FileManagement.createLexerRuleFile(rules))
        }
    }

    private fun execute(
        snippetPath: String,
        lintRulePath: String,
    ): String {
        val printScript = PrintScript()
        printScript.updateRegexRules(lintRulePath)
        val result = printScript.start(snippetPath)
        FileManagement.deleteTempFile(snippetPath)
        FileManagement.deleteTempFile("linterConfig.json")
        FileManagement.deleteTempFile(lintRulePath)
        return result
    }
}
