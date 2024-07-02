package printscript.service.controllers

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import printscript.service.dto.SnippetData
import printscript.service.services.interfaces.ExecuteService
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/execute")
class ExecuteController(private val executeService: ExecuteService) {
    @PostMapping("/")
    fun executeSnippet(
        @RequestBody snippetData: SnippetData,
        @AuthenticationPrincipal userData: Jwt,
    ): ResponseEntity<Mono<String>> {
        return try {
            ResponseEntity.ok(executeService.executeSnippet(snippetData, userData))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message?.let { Mono.just(it) })
        }
    }
}
