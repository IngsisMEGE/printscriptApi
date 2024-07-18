package printscript.service.services.interfaces

import reactor.core.publisher.Mono

interface AssetService {
    fun getSnippet(snippetId: Long): Mono<String>

    fun saveSnippet(
        snippetId: Long,
        snippet: String,
    ): Mono<String>
}
