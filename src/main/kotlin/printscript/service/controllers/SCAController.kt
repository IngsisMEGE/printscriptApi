package printscript.service.controllers

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import printscript.service.dto.SCASnippetWithRulesDTO
import printscript.service.dto.SnippetData
import printscript.service.services.interfaces.SCAService
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/sca")
class SCAController(private val scaService: SCAService) {
    @PostMapping("/")
    fun analyzeCode(
        @RequestBody snippetData: SnippetData,
        @AuthenticationPrincipal userData: Jwt,
    ): ResponseEntity<Mono<String>> {
        return try {
            ResponseEntity.ok(scaService.analyzeCode(snippetData, userData))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message?.let { Mono.just(it) })
        }
    }

    @PostMapping("/withRules")
    fun analyzeCodeWithRules(
        @RequestBody snippetData: SCASnippetWithRulesDTO,
        @AuthenticationPrincipal userData: Jwt,
    ): ResponseEntity<Mono<String>> {
        return try {
            ResponseEntity.ok(scaService.analyzeCodeWithRules(snippetData, userData))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message?.let { Mono.just(it) })
        }
    }
}
