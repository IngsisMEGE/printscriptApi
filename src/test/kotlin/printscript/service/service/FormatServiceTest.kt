package printscript.service.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.oauth2.jwt.Jwt
import printscript.service.dto.*
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.RuleManagerService
import printscript.service.services.interfaces.SnippetManagerService
import printscript.service.services.serviceImpl.FormatServiceImpl
import reactor.core.publisher.Mono

class FormatServiceTest {
    private var assetService: AssetService = mock()
    private val ruleManagerService: RuleManagerService = mock()
    private val snippetManagerService: SnippetManagerService = mock()
    private val printScriptService = FormatServiceImpl(assetService, ruleManagerService, RedisTemplate(), snippetManagerService)
    private val testJwt = "test"

    val jwt =
        Jwt.withTokenValue(testJwt)
            .header("alg", "RS256") // Add the algorithm header (you may adjust this based on your JWT)
            .claim("email", "test@test.com") // Extract other claims as needed
            .build()

    @Test
    fun test001FormatShouldWorkCorrectly() {
        whenever(ruleManagerService.getFormatRules(jwt)).thenReturn(
            Mono.just(
                listOf(
                    RulesDTO("DotFront", "1"),
                    RulesDTO("DotBack", "1"),
                    RulesDTO("EqualFront", "1"),
                    RulesDTO("EqualBack", "1"),
                    RulesDTO("amountOfLines", "1"),
                    RulesDTO("Indentation", "4"),
                ),
            ),
        )

        whenever(assetService.getSnippet(1)).thenReturn(Mono.just("let x:number =     1;\nprintln(x);"))

        val result = printScriptService.format(SnippetData(1, Language.Printscript), jwt).block()

        assertEquals(
            "let x : number = 1;\n\nprintln(x);\n",
            result,
        )
    }

    @Test
    fun test002FormatShouldNotWorkIfLexerTokensAreNotFollowed() {
        whenever(ruleManagerService.getFormatRules(jwt)).thenReturn(Mono.just(listOf(RulesDTO("DotFront", "1"))))
        whenever(assetService.getSnippet(1)).thenReturn(Mono.just("let x:NoExisto =     1;\nprintln(x)"))

        assertThrows<Exception> {
            printScriptService.format(SnippetData(1, Language.Printscript), jwt).block()
        }
    }

    @Test
    fun test003ShouldThrowErrorWhenFormatRulesNotRetrieved() {
        whenever(ruleManagerService.getFormatRules(jwt)).thenReturn(Mono.error(Exception("Error getting format rules")))

        assertThrows<Exception> {
            printScriptService.format(SnippetData(1, Language.Printscript), jwt).block()
        }
    }

    @Test
    fun test004ShouldCleanupTempFilesOnExceptionDuringFormatting() {
        whenever(ruleManagerService.getFormatRules(jwt)).thenReturn(Mono.just(listOf(RulesDTO("DotFront", "1"))))
        whenever(assetService.getSnippet(1)).thenReturn(Mono.just("invalid code"))

        assertThrows<Exception> {
            printScriptService.format(SnippetData(1, Language.Printscript), jwt).block()
        }
    }

    @Test
    fun test005ShouldCleanupTempFilesAfterSuccessfulFormatting() {
        whenever(ruleManagerService.getFormatRules(jwt)).thenReturn(
            Mono.just(
                listOf(
                    RulesDTO("DotFront", "1"),
                    RulesDTO("DotBack", "1"),
                    RulesDTO("EqualFront", "1"),
                    RulesDTO("EqualBack", "1"),
                    RulesDTO("amountOfLines", "1"),
                    RulesDTO("Indentation", "4"),
                ),
            ),
        )

        whenever(assetService.getSnippet(1)).thenReturn(Mono.just("let x:number = 1;\nprintln(x);"))

        val result = printScriptService.format(SnippetData(1, Language.Printscript), jwt).block()

        assertEquals(
            "let x : number = 1;\n\nprintln(x);\n",
            result,
        )
    }

    @Test
    fun test006FormatWithRulesShouldWorkCorrectly() {
        whenever(assetService.getSnippet(1)).thenReturn(Mono.just("let x:number = 1;\nprintln(x);"))

        val result =
            printScriptService.formatWithRules(
                FormatSnippetWithRulesDTO(
                    1,
                    listOf(
                        RulesDTO("DotFront", "1"),
                        RulesDTO("DotBack", "1"),
                        RulesDTO("EqualFront", "1"),
                        RulesDTO("EqualBack", "1"),
                        RulesDTO("amountOfLines", "1"),
                    ),
                    Language.Printscript,
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
        whenever(assetService.getSnippet(1)).thenReturn(Mono.just("let x:NoExisto = 1;\nprintln(x)"))

        assertThrows<Exception> {
            printScriptService.formatWithRules(
                FormatSnippetWithRulesDTO(
                    1,
                    listOf(
                        RulesDTO("DotFront", "1"),
                        RulesDTO("DotBack", "1"),
                        RulesDTO("EqualFront", "1"),
                        RulesDTO("EqualBack", "1"),
                        RulesDTO("amountOfLines", "1"),
                    ),
                    Language.Printscript,
                ),
                jwt,
            ).block()
        }
    }

    @Test
    fun test008FormatWithRulesThatViolateValueShouldNotWork() {
        whenever(assetService.getSnippet(1)).thenReturn(Mono.just("let x:number = 1;\nprintln(x);"))

        val exception =
            assertThrows<Exception> {
                printScriptService.formatWithRules(
                    FormatSnippetWithRulesDTO(
                        1,
                        listOf(
                            RulesDTO("DotFront", "-1"),
                            RulesDTO("DotBack", "1"),
                            RulesDTO("EqualFront", "1"),
                            RulesDTO("EqualBack", "1"),
                            RulesDTO("amountOfLines", "2"),
                        ),
                        Language.Printscript,
                    ),
                    jwt,
                ).block()
            }

        assertEquals("java.lang.Exception: Error formatting snippet with rules", exception.message)
    }

    @Test
    fun test009FormatFilesWhenRulesAreEmpty() {
        whenever(ruleManagerService.getFormatRules(jwt)).thenReturn(Mono.just(listOf()))

        whenever(
            assetService.getSnippet(1),
        ).thenReturn(Mono.just("let x:number=1;\nprintln(x);\n       let     y : number = 2;\nprintln(y);"))

        val result = printScriptService.format(SnippetData(1, Language.Printscript), jwt).block()

        assertEquals(
            "let x : number = 1;\n" +
                "\n" +
                "println(x);\n" +
                "let y : number = 2;\n" +
                "\n" +
                "println(y);\n",
            result,
        )
    }
}
