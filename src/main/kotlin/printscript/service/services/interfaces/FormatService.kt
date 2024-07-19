package printscript.service.services.interfaces

import org.springframework.security.oauth2.jwt.Jwt
import printscript.service.dto.*
import reactor.core.publisher.Mono

interface FormatService {
    fun format(
        snippetData: SnippetData,
        userData: Jwt,
    ): Mono<String>

    fun formatWithRules(
        snippetDataWithRules: FormatSnippetWithRulesDTO,
        userData: Jwt,
    ): Mono<String>

    fun formatWithSnippet(
        snippet: SnippetDataWithSnippet,
        userData: Jwt,
    ): Mono<String>
}
