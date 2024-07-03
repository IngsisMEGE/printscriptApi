package printscript.service.controllers

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import printscript.service.dto.RulesDTO
import printscript.service.services.interfaces.RuleService

@RestController
@RequestMapping("/verify")
class VerifyRuleController(private val ruleService: RuleService) {
    @PostMapping("/rule/lexer")
    fun verifyLexerRules(
        @AuthenticationPrincipal userData: Jwt,
        @RequestBody lintRule: List<RulesDTO>,
    ): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(ruleService.verifyLexerRules(lintRule))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @PostMapping("/rule/formatter")
    fun verifyFormatterRules(
        @AuthenticationPrincipal userData: Jwt,
        @RequestBody formatRules: List<RulesDTO>,
    ): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(ruleService.verifyFormatterRules(formatRules))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @PostMapping("/rule/sca")
    fun verifySCARules(
        @AuthenticationPrincipal userData: Jwt,
        @RequestBody scaRules: List<RulesDTO>,
    ): ResponseEntity<Any> {
        return try {
            ResponseEntity.ok(ruleService.verifySCARules(scaRules))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(e.message)
        }
    }
}
