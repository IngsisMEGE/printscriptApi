package printscript.service.services.interfaces

import org.springframework.security.oauth2.jwt.Jwt
import printscript.service.dto.SCASnippetWithRulesDTO
import printscript.service.dto.SnippetData
import reactor.core.publisher.Mono

interface SCAService {
    fun analyzeCode(
        snippet: SnippetData,
        userData: Jwt,
    ): Mono<String>

    fun analyzeCodeWithRules(
        snippetRule: SCASnippetWithRulesDTO,
        userData: Jwt,
    ): Mono<String>
}
