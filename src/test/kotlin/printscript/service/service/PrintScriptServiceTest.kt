package printscript.service.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import printscript.service.dto.SnippetData
import printscript.service.services.interfaces.AssetService
import printscript.service.services.interfaces.PrintScriptService
import printscript.service.services.serviceImpl.PrintScriptImpl
import reactor.core.publisher.Mono

class PrintScriptServiceTest {
    private var assetService: AssetService = mock()
    private val printScriptService: PrintScriptService = PrintScriptImpl(assetService)

    @Test
    fun test001formatShouldWorkCorrectly() {
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("let x:number =     1;\nprintln(x);"))

        val result = printScriptService.format(SnippetData(1)).block()

        assertEquals(
            "let x : number = 1;\n" +
                "\n" +
                "println(x);\n",
            result,
        )
    }

    @Test
    fun test002formatShouldNotWorkIfLexerTokenAreNotFollowed() {
        whenever(assetService.getSnippet("snippets", 1)).thenReturn(Mono.just("let x:NoExisto =     1;\nprintln(x)"))

        assertThrows<Exception> {
            printScriptService.format(SnippetData(1)).block()
        }
    }
}
