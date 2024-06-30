package printscript.service.services.interfaces

import reactor.core.publisher.Mono

interface AssetService {
    fun getSnippet(
        container: String,
        key: Long,
    ): Mono<String>

    fun saveSnippet(
        container: String,
        snippetId: Long,
        snippet: String,
    ): Mono<String>
}
