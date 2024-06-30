package printscript.service.controllers

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import printscript.service.dto.SnippetData
import printscript.service.dto.SnippetWithRuleDTO
import printscript.service.services.interfaces.FormatService
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/format")
class FormatController(private val printScriptService: FormatService) {
    @PostMapping("/")
    fun formatSnippet(
        @RequestBody snippetData: SnippetData,
        @AuthenticationPrincipal userData: Jwt,
    ): Mono<ResponseEntity<String>> {
        return printScriptService.format(snippetData, userData)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { e -> Mono.just(ResponseEntity.badRequest().body(e.message)) }
    }

    @PostMapping("/withRules")
    fun formatSnippetWithRules(
        @RequestBody snippetDataWithRules: SnippetWithRuleDTO,
        @AuthenticationPrincipal userData: Jwt,
    ): Mono<ResponseEntity<String>> {
        return printScriptService.formatWithRules(snippetDataWithRules, userData)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { e -> Mono.just(ResponseEntity.badRequest().body(e.message)) }
    }

    @PostMapping("/save")
    fun formatSnippetAndSave(
        @RequestBody snippetData: SnippetData,
        @AuthenticationPrincipal userData: Jwt,
    ): Mono<ResponseEntity<String>> {
        return printScriptService.formatAndSave(snippetData, userData)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { e -> Mono.just(ResponseEntity.badRequest().body(e.message)) }
    }
}
