package printscript.service.controllers

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import printscript.service.dto.*
import printscript.service.services.interfaces.ExecuteService
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/execute")
class ExecuteController(private val executeService: ExecuteService) {
    @PostMapping("/test")
    fun executeSnippet(
        @RequestBody snippetData: SnippetDataTest,
        @AuthenticationPrincipal userData: Jwt,
    ): Mono<ResponseEntity<String>> {
        return executeService.executeSnippet(snippetData, userData)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { e -> Mono.just(ResponseEntity.badRequest().body(e.message)) }
    }

    @PostMapping("/live")
    fun executeLiveController(
        @RequestBody snippetDataLive: SnippetDataInputs,
        @AuthenticationPrincipal userData: Jwt,
    ): Mono<ResponseEntity<SnippetDataLiveResponse>> {
        return executeService.liveExecuteSnippet(snippetDataLive, userData)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { e ->
                Mono.just(
                    ResponseEntity.badRequest().body(SnippetDataLiveResponse(e.message ?: "", false)),
                )
            }
    }
}
