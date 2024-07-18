package printscript.service.services.interfaces

import org.springframework.security.oauth2.jwt.Jwt
import printscript.service.dto.FormatSnippetWithRulesDTO
import printscript.service.dto.SnippetData
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
}
