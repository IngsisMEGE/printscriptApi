package printscript.service.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import printscript.service.dto.SnippetData
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.RuleService
import printscript.service.services.serviceImpl.PrintScriptImpl
import printscript.service.utils.FileManagement
import reactor.core.publisher.Mono

class PrintScriptServiceTest {
    private var assetService: AssetService = mock()
    private val ruleService: RuleService = mock()
    private val printScriptService = PrintScriptImpl(assetService, ruleService)

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

        val result = printScriptService.format(SnippetData(1)).block()

        FileManagement.deleteTempFile("formatterConfig.json")

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
            printScriptService.format(SnippetData(1)).block()
        }
    }

    @Test
    fun test003ShouldThrowErrorWhenFormatRulesNotRetrieved() {
        whenever(ruleService.getFormatRules()).thenReturn(Mono.error(Exception("Error getting format rules")))

        assertThrows<Exception> {
            printScriptService.format(SnippetData(1)).block()
        }
    }

    @Test
    fun test004ShouldCleanupTempFilesOnExceptionDuringFormatting() {
        whenever(ruleService.getFormatRules()).thenReturn(Mono.just("format rules"))
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("invalid code"))

        assertThrows<Exception> {
            printScriptService.format(SnippetData(1)).block()
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

        val result = printScriptService.format(SnippetData(1)).block()

        assertEquals(
            "let x : number = 1;\n\nprintln(x);\n",
            result,
        )
    }
}
