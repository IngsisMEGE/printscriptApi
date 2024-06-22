package printscript.service.services.serviceImpl

import org.example.PrintScript
import org.springframework.stereotype.Service
import printscript.service.dto.SnippetData
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.PrintScriptService
import printscript.service.utils.FileManagement
import reactor.core.publisher.Mono

@Service
class PrintScriptImpl(private val assetService: AssetService) : PrintScriptService {
    override fun format(snippetData: SnippetData): Mono<String> {
        var snippetFilePath = ""
        return try {
            assetService.getSnippet("snippets", snippetData.snippetId)
                .map { code ->
                    snippetFilePath = FileManagement.createTempFileWithContent(code)
                    val formatResult = PrintScript().format(snippetFilePath)
                    FileManagement.deleteTempFile(snippetFilePath)
                    formatResult
                }
        } catch (e: Exception) {
            FileManagement.deleteTempFile(snippetFilePath)
            Mono.error(e)
        }
    }
}
