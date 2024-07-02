package printscript.service.services.interfaces

import org.springframework.security.oauth2.jwt.Jwt
import printscript.service.dto.SnippetData
import reactor.core.publisher.Mono

interface ExecuteService {
    fun executeSnippet(
        snippet: SnippetData,
        userData: Jwt,
    ): Mono<String>
}
