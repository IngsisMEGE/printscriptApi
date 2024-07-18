package printscript.service.services.serviceImpl

import org.example.PrintScript
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import printscript.service.dto.*
import printscript.service.exceptions.InputsAreEmptyException
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.ExecuteService
import printscript.service.services.interfaces.RuleManagerService
import printscript.service.utils.FileManagement
import reactor.core.publisher.Mono

@Service
class ExecuteServiceImpl(private val assetService: AssetService, private val ruleManagerService: RuleManagerService) : ExecuteService {
    override fun executeSnippet(
        snippet: SnippetDataTest,
        userData: Jwt,
    ): Mono<String> {
        val inputs: MutableList<String> = snippet.inputs.toMutableList()
        return getSnippet(snippet.snippetId).flatMap { snippetFile ->
            getLintingRules(userData).flatMap { rulesFile ->
                Mono.just(executeWithEnv(snippetFile, rulesFile, PrintScript { loadInputStatic(inputs) }, snippet.envs))
            }
        }
    }

    override fun liveExecuteSnippet(
        snippet: SnippetDataInputs,
        userData: Jwt,
    ): Mono<SnippetDataLiveResponse> {
        val inputs: MutableList<String> = snippet.inputs.toMutableList()
        var inputMessage = ""
        return getSnippet(snippet.snippetId).flatMap { snippetFile ->
            getLintingRules(userData).flatMap { rulesFile ->
                val printscript =
                    PrintScript {
                        inputMessage = it
                        loadInputDynamic(it, inputs)
                    }
                try {
                    val result = execute(snippetFile, rulesFile, printscript)
                    Mono.just(SnippetDataLiveResponse(result, false))
                } catch (e: InputsAreEmptyException) {
                    val outputs = printscript.getOutputs()
                    val result = StringBuilder()
                    for (output in outputs) {
                        result.append(output)
                    }
                    result.append(inputMessage)
                    Mono.just(SnippetDataLiveResponse(result.toString(), true))
                } catch (e: Exception) {
                    Mono.error(e)
                }
            }
        }
    }

    private fun getSnippet(snippetId: Long): Mono<String> {
        return assetService.getSnippet(snippetId).flatMap { snippet ->
            Mono.just(FileManagement.createTempFileWithContent(snippet))
        }
    }

    private fun getLintingRules(userData: Jwt): Mono<String> {
        return ruleManagerService.getLintingRules(userData).flatMap { rules ->
            Mono.just(FileManagement.createLexerRuleFile(rules))
        }
    }

    private fun execute(
        snippetPath: String,
        lintRulePath: String,
        printScript: PrintScript,
    ): String {
        printScript.updateRegexRules(lintRulePath)
        val result = printScript.start(snippetPath)
        FileManagement.deleteTempFile(snippetPath)
        FileManagement.deleteTempFile("linterConfig.json")
        FileManagement.deleteTempFile("lexerRules.json")
        FileManagement.deleteTempFile(lintRulePath)
        return result
    }

    private fun executeWithEnv(
        snippetPath: String,
        lintRulePath: String,
        printScript: PrintScript,
        envs: Map<String, String>,
    ): String {
        printScript.updateRegexRules(lintRulePath)
        val result = printScript.start(snippetPath, envs)
        FileManagement.deleteTempFile(snippetPath)
        FileManagement.deleteTempFile("linterConfig.json")
        FileManagement.deleteTempFile("lexerRules.json")
        FileManagement.deleteTempFile(lintRulePath)
        return result
    }

    private fun loadInputStatic(inputs: MutableList<String>): String {
        return inputs.removeAt(0)
    }

    private fun loadInputDynamic(
        message: String,
        inputs: MutableList<String>,
    ): String {
        if (inputs.isEmpty()) {
            throw InputsAreEmptyException(message)
        }
        return inputs.removeAt(0)
    }
}
