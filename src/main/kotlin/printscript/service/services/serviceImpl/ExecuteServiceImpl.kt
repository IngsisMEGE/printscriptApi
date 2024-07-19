package printscript.service.services.serviceImpl

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import printscript.service.dto.*
import printscript.service.exceptions.InputsAreEmptyException
import printscript.service.languagerunner.LanguageRunnerProvider
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.ExecuteService
import printscript.service.utils.FileManagement
import reactor.core.publisher.Mono

@Service
class ExecuteServiceImpl(private val assetService: AssetService) : ExecuteService {
    private val logger: Logger = LoggerFactory.getLogger(ExecuteServiceImpl::class.java)

    override fun executeSnippet(
        snippet: SnippetDataTest,
        userData: Jwt,
    ): Mono<String> {
        logger.info("Executing snippet with id: ${snippet.snippetId}")
        val inputs: MutableList<String> = snippet.inputs.toMutableList()
        val languageRunner =
            LanguageRunnerProvider.getLanguageRunner(snippet.language) {
                loadInputStatic(inputs)
            }
        return getSnippet(snippet.snippetId).flatMap { snippetFile ->
            val result =
                languageRunner.executeSnippet(
                    snippetFile,
                    snippet.envs,
                )
            cleanupFile(snippetFile)
            Mono.just(result)
        }
    }

    override fun liveExecuteSnippet(
        snippet: SnippetDataInputs,
        userData: Jwt,
    ): Mono<SnippetDataLiveResponse> {
        logger.info("Executing snippet with id: ${snippet.snippetId}")
        val inputs: MutableList<String> = snippet.inputs.toMutableList()
        var inputMessage = ""
        return getSnippet(snippet.snippetId).flatMap { snippetFile ->
            val languageRunner =
                LanguageRunnerProvider.getLanguageRunner(snippet.language) {
                    inputMessage = it
                    loadInputDynamic(it, inputs)
                }
            try {
                val result =
                    languageRunner.executeSnippet(
                        snippetFile,
                        mapOf(),
                    )
                cleanupFile(snippetFile)
                Mono.just(SnippetDataLiveResponse(result, false))
            } catch (e: InputsAreEmptyException) {
                val outputs = languageRunner.getOutputs()
                val result = StringBuilder()
                for (output in outputs) {
                    result.append(output)
                }
                result.append(inputMessage)
                cleanupFile(snippetFile)
                Mono.just(SnippetDataLiveResponse(result.toString(), true))
            } catch (e: Exception) {
                logger.error("Error executing snippet with id: ${snippet.snippetId}", e)
                cleanupFile(snippetFile)
                Mono.error(e)
            }
        }
    }

    private fun getSnippet(snippetId: Long): Mono<String> {
        val snippet = assetService.getSnippet(snippetId)
        return Mono.just(FileManagement.createTempFileWithContent(snippet))
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

    private fun cleanupFile(filePath: String) {
        try {
            FileManagement.deleteTempFile(filePath)
        } catch (ignored: Exception) {
            println("Error deleting temp file: $filePath")
        }
    }
}
