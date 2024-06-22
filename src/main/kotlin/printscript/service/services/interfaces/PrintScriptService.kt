package printscript.service.services.interfaces

import printscript.service.dto.SnippetData
import reactor.core.publisher.Mono

interface PrintScriptService {
    fun format(snippetData: SnippetData): Mono<String>
}
