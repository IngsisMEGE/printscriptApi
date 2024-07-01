package printscript.service.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.security.oauth2.jwt.Jwt
import printscript.service.dto.RulesDTO
import printscript.service.dto.SnippetData
import printscript.service.dto.SnippetWithRuleDTO
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.RuleService
import printscript.service.services.serviceImpl.FormatServiceImpl
import reactor.core.publisher.Mono

class FormatServiceTest {
    private var assetService: AssetService = mock()
    private val ruleService: RuleService = mock()
    private val printScriptService = FormatServiceImpl(assetService, ruleService)
    private val testJwt = "test"

    val jwt =
        Jwt.withTokenValue(testJwt)
            .header("alg", "RS256") // Add the algorithm header (you may adjust this based on your JWT)
            .claim("email", "test@test.com") // Extract other claims as needed
            .build()

    val objectMapper = ObjectMapper().registerModule(KotlinModule())

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

    @Test
    fun test001FormatShouldWorkCorrectly() {
        whenever(ruleService.getFormatRules(jwt)).thenReturn(
            Mono.just(
                "{\n" +
                    "  \"DotFront\": \"1\",\n" +
                    "  \"DotBack\": \"1\",\n" +
                    "  \"EqualFront\": \"1\",\n" +
                    "  \"EqualBack\": \"1\",\n" +
                    "  \"amountOfLines\" : \"1\",\n" +
                    "  \"Indentation\": \"4\"\n" +
                    "}",
            ),
        )

        val lintRules = getLintRulesAsString()
        whenever(ruleService.getLintingRules(jwt)).thenReturn(Mono.just(getLintRulesAsString()))
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("let x:number =     1;\nprintln(x);"))

        val result = printScriptService.format(SnippetData(1), jwt).block()

        assertEquals(
            "let x : number = 1;\n\nprintln(x);\n",
            result,
        )
    }

    @Test
    fun test002FormatShouldNotWorkIfLexerTokensAreNotFollowed() {
        whenever(ruleService.getFormatRules(jwt)).thenReturn(Mono.just("format rules"))
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("let x:NoExisto =     1;\nprintln(x)"))
        whenever(ruleService.getLintingRules(jwt)).thenReturn(Mono.just(getLintRulesAsString()))

        assertThrows<Exception> {
            printScriptService.format(SnippetData(1), jwt).block()
        }
    }

    @Test
    fun test003ShouldThrowErrorWhenFormatRulesNotRetrieved() {
        whenever(ruleService.getFormatRules(jwt)).thenReturn(Mono.error(Exception("Error getting format rules")))
        whenever(ruleService.getLintingRules(jwt)).thenReturn(Mono.just(getLintRulesAsString()))

        assertThrows<Exception> {
            printScriptService.format(SnippetData(1), jwt).block()
        }
    }

    @Test
    fun test004ShouldCleanupTempFilesOnExceptionDuringFormatting() {
        whenever(ruleService.getFormatRules(jwt)).thenReturn(Mono.just("format rules"))
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("invalid code"))

        assertThrows<Exception> {
            printScriptService.format(SnippetData(1), jwt).block()
        }
    }

    @Test
    fun test005ShouldCleanupTempFilesAfterSuccessfulFormatting() {
        whenever(ruleService.getFormatRules(jwt)).thenReturn(
            Mono.just(
                "{\n" +
                    "  \"DotFront\": \"1\",\n" +
                    "  \"DotBack\": \"1\",\n" +
                    "  \"EqualFront\": \"1\",\n" +
                    "  \"EqualBack\": \"1\",\n" +
                    "  \"amountOfLines\" : \"1\",\n" +
                    "  \"Indentation\": \"4\"\n" +
                    "}",
            ),
        )

        whenever(ruleService.getLintingRules(jwt)).thenReturn(Mono.just(getLintRulesAsString()))

        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("let x:number = 1;\nprintln(x);"))

        val result = printScriptService.format(SnippetData(1), jwt).block()

        assertEquals(
            "let x : number = 1;\n\nprintln(x);\n",
            result,
        )
    }

    @Test
    fun test006FormatWithRulesShouldWorkCorrectly() {
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("let x:number = 1;\nprintln(x);"))

        val result =
            printScriptService.formatWithRules(
                SnippetWithRuleDTO(
                    1,
                    listOf(
                        RulesDTO("DotFront", "1"),
                        RulesDTO("DotBack", "1"),
                        RulesDTO("EqualFront", "1"),
                        RulesDTO("EqualBack", "1"),
                        RulesDTO("amountOfLines", "1"),
                    ),
                    lintRules,
                ),
                jwt,
            ).block()

        assertEquals(
            "let x : number = 1;\n\nprintln(x);\n",
            result,
        )
    }

    @Test
    fun test007FormatWithRulesShouldNotWorkIfLexerTokensAreNotFollowed() {
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("let x:NoExisto = 1;\nprintln(x)"))

        assertThrows<Exception> {
            printScriptService.formatWithRules(
                SnippetWithRuleDTO(
                    1,
                    listOf(
                        RulesDTO("DotFront", "1"),
                        RulesDTO("DotBack", "1"),
                        RulesDTO("EqualFront", "1"),
                        RulesDTO("EqualBack", "1"),
                        RulesDTO("amountOfLines", "1"),
                    ),
                    lintRules,
                ),
                jwt,
            ).block()
        }
    }

    @Test
    fun test008FormatWithRulesThatViolateValueShouldNotWork() {
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("let x:number = 1;\nprintln(x);"))

        val exception =
            assertThrows<Exception> {
                printScriptService.formatWithRules(
                    SnippetWithRuleDTO(
                        1,
                        listOf(
                            RulesDTO("DotFront", "-1"),
                            RulesDTO("DotBack", "1"),
                            RulesDTO("EqualFront", "1"),
                            RulesDTO("EqualBack", "1"),
                            RulesDTO("amountOfLines", "2"),
                        ),
                        lintRules,
                    ),
                    jwt,
                ).block()
            }

        assertEquals("java.lang.Exception: Error formatting snippet with rules", exception.message)
    }

    @Test
    fun test009FormatWithOtherLexerRulesShouldWork() {
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("var x:int = 1;\nprintln(x);"))

        for (rule in lintRules) {
            rule.value =
                when (rule.name) {
                    "NUMBER_TYPE" -> "\\bint\\b"
                    "DECLARATION_VARIABLE" -> "\\bvar\\b"
                    else -> rule.value
                }
        }

        val result =
            printScriptService.formatWithRules(
                SnippetWithRuleDTO(
                    1,
                    listOf(
                        RulesDTO("DotFront", "1"),
                        RulesDTO("DotBack", "1"),
                        RulesDTO("EqualFront", "1"),
                        RulesDTO("EqualBack", "1"),
                        RulesDTO("amountOfLines", "1"),
                    ),
                    lintRules,
                ),
                jwt,
            ).block()

        assertEquals(
            "let x : int = 1;\n\nprintln(x);\n",
            result,
        )

        for (rule in lintRules) {
            rule.value =
                when (rule.name) {
                    "NUMBER_TYPE" -> "\\bnumber\\b"
                    "DECLARATION_VARIABLE" -> "\\blet\\b"
                    else -> rule.value
                }
        }
    }

    private fun getLintRulesAsString(): String {
        val rulesMap =
            lintRules.associate { rule ->
                rule.name to
                    mapOf(
                        "pattern" to rule.value,
                        "type" to rule.name,
                    )
            }
        return objectMapper.writeValueAsString(rulesMap)
    }
}
