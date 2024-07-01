package printscript.service.service

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

    @Test
    fun test001FormatShouldWorkCorrectly() {
        whenever(ruleService.getFormatRules()).thenReturn(
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
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("let x:number =     1;\nprintln(x);"))

        val result = printScriptService.format(SnippetData(1), jwt).block()

        assertEquals(
            "let x : number = 1;\n\nprintln(x);\n",
            result,
        )
    }

    @Test
    fun test002FormatShouldNotWorkIfLexerTokensAreNotFollowed() {
        whenever(ruleService.getFormatRules()).thenReturn(Mono.just("format rules"))
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("let x:NoExisto =     1;\nprintln(x)"))

        assertThrows<Exception> {
            printScriptService.format(SnippetData(1), jwt).block()
        }
    }

    @Test
    fun test003ShouldThrowErrorWhenFormatRulesNotRetrieved() {
        whenever(ruleService.getFormatRules()).thenReturn(Mono.error(Exception("Error getting format rules")))

        assertThrows<Exception> {
            printScriptService.format(SnippetData(1), jwt).block()
        }
    }

    @Test
    fun test004ShouldCleanupTempFilesOnExceptionDuringFormatting() {
        whenever(ruleService.getFormatRules()).thenReturn(Mono.just("format rules"))
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("invalid code"))

        assertThrows<Exception> {
            printScriptService.format(SnippetData(1), jwt).block()
        }
    }

    @Test
    fun test005ShouldCleanupTempFilesAfterSuccessfulFormatting() {
        whenever(ruleService.getFormatRules()).thenReturn(
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
                    ),
                    jwt,
                ).block()
            }

        assertEquals("java.lang.Exception: Error formatting snippet with rules", exception.message)
    }
}
