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
import printscript.service.dto.SnippetStatus
import printscript.service.dto.StatusDTO
import printscript.service.services.interfaces.SCAService
import printscript.service.services.interfaces.SnippetManagerService
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/sca")
class SCAController(private val scaService: SCAService, private val snippetManagerService: SnippetManagerService) {
    @PostMapping("/")
    fun analyzeCode(
        @RequestBody snippetData: SnippetData,
        @AuthenticationPrincipal userData: Jwt,
    ): ResponseEntity<Mono<String>> {
        return try {
            val result = scaService.analyzeCode(snippetData, userData)
            snippetManagerService.updateSnippetStatus(
                StatusDTO(SnippetStatus.COMPLIANT, snippetData.snippetId, userData.claims["email"].toString()),
            )
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            snippetManagerService.updateSnippetStatus(
                StatusDTO(SnippetStatus.NOT_COMPLIANT, snippetData.snippetId, userData.claims["email"].toString()),
            )
            ResponseEntity.badRequest().body(e.message?.let { Mono.just(it) })
        }
    }

    @PostMapping("/withRules")
    fun analyzeCodeWithRules(
        @RequestBody snippetData: SCASnippetWithRulesDTO,
        @AuthenticationPrincipal userData: Jwt,
    ): ResponseEntity<Mono<String>> {
        return try {
            val result = scaService.analyzeCodeWithRules(snippetData, userData)
            snippetManagerService.updateSnippetStatus(
                StatusDTO(SnippetStatus.COMPLIANT, snippetData.snippetId, userData.claims["email"].toString()),
            )
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            snippetManagerService.updateSnippetStatus(
                StatusDTO(SnippetStatus.NOT_COMPLIANT, snippetData.snippetId, userData.claims["email"].toString()),
            )
            ResponseEntity.badRequest().body(e.message?.let { Mono.just(it) })
        }
    }
}
