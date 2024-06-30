package printscript.service.services.interfaces

import org.springframework.security.oauth2.jwt.Jwt
import printscript.service.dto.SnippetData
import printscript.service.dto.SnippetWithRuleDTO
import reactor.core.publisher.Mono

interface FormatService {
    fun format(
        snippetData: SnippetData,
        userData: Jwt,
    ): Mono<String>

    fun formatWithRules(
        snippetDataWithRules: SnippetWithRuleDTO,
        userData: Jwt,
    ): Mono<String>

    fun formatAndSave(
        snippetData: SnippetData,
        userData: Jwt,
    ): Mono<String>
}
