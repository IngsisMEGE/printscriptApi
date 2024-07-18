package printscript.service.services.interfaces

import org.springframework.security.oauth2.jwt.Jwt
import printscript.service.dto.*
import reactor.core.publisher.Mono

interface ExecuteService {
    fun executeSnippet(
        snippet: SnippetDataTest,
        userData: Jwt,
    ): Mono<String>

    fun liveExecuteSnippet(
        snippet: SnippetDataInputs,
        userData: Jwt,
    ): Mono<SnippetDataLiveResponse>
}
