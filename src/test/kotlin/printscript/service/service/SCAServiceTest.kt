package printscript.service.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.oauth2.jwt.Jwt
import printscript.service.dto.RulesDTO
import printscript.service.dto.SCASnippetWithRulesDTO
import printscript.service.dto.SnippetData
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.RuleManagerService
import printscript.service.services.interfaces.SnippetManagerService
import printscript.service.services.serviceImpl.SCAServiceImpl
import reactor.core.publisher.Mono

class SCAServiceTest {
    private var assetService: AssetService = mock()
    private val ruleManagerService: RuleManagerService = mock()
    private val redisTemplate: RedisTemplate<String, Any> = mock()
    private val snippetManagerService: SnippetManagerService = mock()
    private val scaService = SCAServiceImpl(assetService, ruleManagerService, redisTemplate, snippetManagerService)

    val testJwt = "test"
    val jwt =
        Jwt.withTokenValue(testJwt)
            .header("alg", "RS256") // Add the algorithm header (you may adjust this based on your JWT)
            .claim("email", "test@test.com") // Extract other claims as needed
            .build()

    val scaRules =
        listOf(
            RulesDTO("CamelCaseFormat", "true"),
            RulesDTO("SnakeCaseFormat", "false"),
            RulesDTO("MethodNoExpression", "false"),
            RulesDTO("InputNoExpression", "false"),
        )

    val lintRules =
        listOf(
            RulesDTO("STRING_VALUE", "\\\"(?:\\\\.|[^\"])*\\\""),
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
            RulesDTO("VARIABLE_NAME", """(?<!")\b[a-zA-Z_][a-zA-Z0-9_]*\b(?!")"""),
        )

    @Test
    fun test001AnalyzeCodeShouldWorkCorrectly() {
        whenever(assetService.getSnippet(1L)).thenReturn(Mono.just("let abcedario : number = 1;"))
        whenever(ruleManagerService.getSCARules(jwt)).thenReturn(Mono.just(scaRules))
        whenever(ruleManagerService.getLintingRules(jwt)).thenReturn(Mono.just(lintRules))

        val result = scaService.analyzeCode(SnippetData(1L), jwt).block()

        assertEquals("", result)
    }

    @Test
    fun test002AnalyzeCodeViolatingRulesShouldThrowException() {
        whenever(assetService.getSnippet(1L)).thenReturn(Mono.just("let CSAfa_fdasf : number = 1;"))
        whenever(ruleManagerService.getSCARules(jwt)).thenReturn(Mono.just(scaRules))
        whenever(ruleManagerService.getLintingRules(jwt)).thenReturn(Mono.just(lintRules))

        assertEquals("Invalid typing format in line 4 row 1", scaService.analyzeCode(SnippetData(1L), jwt).block())
    }

    @Test
    fun test003AnalyzeCodeOpposingRulesShouldNowWork() {
        val cammelRules =
            listOf(
                RulesDTO("CamelCaseFormat", "true"),
                RulesDTO("SnakeCaseFormat", "true"),
                RulesDTO("MethodNoExpression", "false"),
                RulesDTO("InputNoExpression", "false"),
            )

        whenever(assetService.getSnippet(1L)).thenReturn(Mono.just("let abcedario : number = 1;"))

        whenever(ruleManagerService.getSCARules(jwt)).thenReturn(Mono.just(cammelRules))
        whenever(ruleManagerService.getLintingRules(jwt)).thenReturn(Mono.just(lintRules))

        assertThrows<Exception> { scaService.analyzeCode(SnippetData(1L), jwt).block() }
    }

    @Test
    fun test004AnalyzeCodeWithRulesShouldWorkCorrectly() {
        whenever(assetService.getSnippet(1L)).thenReturn(Mono.just("let abcedario : number = 1;"))

        val scaRules =
            listOf(
                RulesDTO("CamelCaseFormat", "true"),
                RulesDTO("SnakeCaseFormat", "false"),
                RulesDTO("MethodNoExpression", "false"),
                RulesDTO("InputNoExpression", "false"),
            )

        val result = scaService.analyzeCodeWithRules(SCASnippetWithRulesDTO(1L, scaRules, lintRules), jwt).block()

        assertEquals("", result)
    }
}
