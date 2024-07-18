package printscript.service.services.serviceImpl

import org.example.PrintScript
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    private val logger: Logger = LoggerFactory.getLogger(ExecuteServiceImpl::class.java)

    override fun executeSnippet(
        snippet: SnippetDataTest,
        userData: Jwt,
    ): Mono<String> {
        logger.debug("Entering executeSnippet with snippetId: ${snippet.snippetId}")
        val inputs: MutableList<String> = snippet.inputs.toMutableList()
        return getSnippet(snippet.snippetId).flatMap { snippetFile ->
            getLintingRules(userData).flatMap { rulesFile ->
                Mono.just(executeWithEnv(snippetFile, rulesFile, PrintScript { loadInputStatic(inputs) }, snippet.envs))
            }
        }.doOnSuccess {
            logger.info("Successfully executed snippet with snippetId: ${snippet.snippetId}")
        }.doOnError { error ->
            logger.error("Error executing snippet with snippetId: ${snippet.snippetId}", error)
        }.doFinally {
            logger.debug("Exiting executeSnippet with snippetId: ${snippet.snippetId}")
        }
    }

    override fun liveExecuteSnippet(
        snippet: SnippetDataInputs,
        userData: Jwt,
    ): Mono<SnippetDataLiveResponse> {
        logger.debug("Entering liveExecuteSnippet with snippetId: ${snippet.snippetId}")
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
                    logger.error("Error live executing snippet with snippetId: ${snippet.snippetId}", e)
                    Mono.error(e)
                }
            }
        }.doOnSuccess {
            logger.info("Successfully live executed snippet with snippetId: ${snippet.snippetId}")
        }.doOnError { error ->
            logger.error("Error live executing snippet with snippetId: ${snippet.snippetId}", error)
        }.doFinally {
            logger.debug("Exiting liveExecuteSnippet with snippetId: ${snippet.snippetId}")
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

    private fun getLintingRules(userData: Jwt): Mono<String> {
        logger.debug("Entering getLintingRules")
        return ruleManagerService.getLintingRules(userData).flatMap { rules ->
            logger.info("Successfully retrieved linting rules")
            Mono.just(FileManagement.createLexerRuleFile(rules))
        }.doOnError { error ->
            logger.error("Error retrieving linting rules", error)
        }.doFinally {
            logger.debug("Exiting getLintingRules")
        }
    }

    private fun execute(
        snippetPath: String,
        lintRulePath: String,
        printScript: PrintScript,
    ): String {
        logger.debug("Executing snippet with paths: snippetPath = $snippetPath, lintRulePath = $lintRulePath")
        printScript.updateRegexRules(lintRulePath)
        val result = printScript.start(snippetPath)
        FileManagement.deleteTempFile(snippetPath)
        FileManagement.deleteTempFile("linterConfig.json")
        FileManagement.deleteTempFile("lexerRules.json")
        FileManagement.deleteTempFile(lintRulePath)
        logger.info("Snippet executed successfully")
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
