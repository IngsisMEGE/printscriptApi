package printscript.service.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.jwt.Jwt
import printscript.service.dto.*
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.ExecuteService
import printscript.service.services.interfaces.RuleManagerService
import printscript.service.services.serviceImpl.ExecuteServiceImpl
import reactor.core.publisher.Mono

class ExecuteServiceTest {
    private val assetService: AssetService = mock()
    private val ruleManagerService: RuleManagerService = mock()
    private val executeService: ExecuteService = ExecuteServiceImpl(assetService, ruleManagerService)

    val lintRules =
        listOf(
            RulesDTO("STRING_VALUE", "\"(?:\\\\.|[^\"])*\""),
            RulesDTO("STRING_VALUE", "'(?:\\\\.|[^'])*'"),
            RulesDTO("DECLARATION_VARIABLE", "\\blet\\b"),
            RulesDTO("DECLARATION_IMMUTABLE", "\\bconst\\b"),
            RulesDTO("IF_STATEMENT", "\\bif\\b"),
            RulesDTO("ELSE_STATEMENT", "\\}\\s*else"),
            RulesDTO("OPERATOR_PLUS", "\\+"),
            RulesDTO("OPERATOR_MINUS", "-"),
            RulesDTO("OPERATOR_MULTIPLY", "\\*"),
            RulesDTO("OPERATOR_DIVIDE", "/"),
            RulesDTO("DOUBLE_DOTS", ":"),
            RulesDTO("SEPARATOR", ";"),
            RulesDTO("ASSIGNATION", "="),
            RulesDTO("LEFT_PARENTHESIS", "\\("),
            RulesDTO("RIGHT_PARENTHESIS", "\\)"),
            RulesDTO("LEFT_BRACKET", "\\{"),
            RulesDTO("RIGHT_BRACKET", "\\}"),
            RulesDTO("METHOD_CALL", "\\b\\w+\\s*\\((?:[^()]*|\\([^()]*\\))*\\)"),
            RulesDTO("COMA", ","),
            RulesDTO("NUMBER_TYPE", "\\bnumber\\b"),
            RulesDTO("STRING_TYPE", "\\bstring\\b"),
            RulesDTO("BOOLEAN_TYPE", "\\bboolean\\b"),
            RulesDTO("BOOLEAN_VALUE", "\\btrue\\b"),
            RulesDTO("BOOLEAN_VALUE", "\\bfalse\\b"),
            RulesDTO("NUMBER_VALUE", "\\b\\d+\\.?\\d*\\b"),
            RulesDTO("VARIABLE_NAME", "(?<!\")\\b[a-zA-Z_][a-zA-Z0-9_]*\\b(?!\")"),
        )

    val testJwt = "test"
    val jwt =
        Jwt.withTokenValue(testJwt)
            .header("alg", "RS256") // Add the algorithm header (you may adjust this based on your JWT)
            .claim("email", "test@test.com") // Extract other claims as needed
            .build()

    @Test
    fun test001ExecuteSnippetSuccessfully() {
        whenever(ruleManagerService.getLintingRules(jwt)).thenReturn(Mono.just(lintRules))

        whenever(
            assetService.getSnippet(1),
        ).thenReturn(Mono.just("let a : number = 1;  let b : number = 2; let c : number= a + b; println(c);"))

        val result = executeService.executeSnippet(SnippetDataInputs(1L, listOf()), jwt).block()

        assertEquals("3\n", result)
    }

    @Test
    fun test002ExecuteSnippetWithWrongStructureShouldError() {
        whenever(ruleManagerService.getLintingRules(jwt)).thenReturn(Mono.just(lintRules))

        whenever(
            assetService.getSnippet(1),
        ).thenReturn(Mono.just("let a | NOEXISTO = 1;  let b : number = 2; let c : number= a + b; println(c"))

        val result = assertThrows<Exception> { executeService.executeSnippet(SnippetDataInputs(1L, listOf()), jwt).block() }

        assertEquals("exceptions.SyntacticError: Unexpected structure at Line 1", result.message)
    }

    @Test
    fun test003ExecuteTestSnippetWithInputsAndReadInput() {
        whenever(ruleManagerService.getLintingRules(jwt)).thenReturn(Mono.just(lintRules))

        whenever(
            assetService.getSnippet(1),
        ).thenReturn(Mono.just("let x :boolean = true;"))

        val result = executeService.executeSnippet(SnippetDataInputs(1L, listOf("1", "2")), jwt).block()

        assertEquals("1\n", result)
    }
}
