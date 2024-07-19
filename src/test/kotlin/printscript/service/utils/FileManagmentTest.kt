package printscript.service.utils

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import printscript.service.dto.*
import java.nio.file.Files
import java.nio.file.Paths

class FileManagmentTest {
    @Test
    fun test001fileManagmentShouldCreateFileCorrectly() {
        val path = FileManagement.createTempFileWithContent("hola")

        assertTrue(Files.exists(Paths.get(path)))

        FileManagement.deleteTempFile(path)
    }

    @Test
    fun test002fileManagmentShouldDeleteFileCorrectly() {
        val path = FileManagement.createTempFileWithContent("hola")

        FileManagement.deleteTempFile(path)

        assertTrue(!Files.exists(Paths.get(path)))
    }

    @Test
    fun test003fileManagmentShouldCreateSCARuleFileCorrectly() {
        val rules = listOf(RulesDTO("rule1", "true"), RulesDTO("rule2", "false"))
        val path = FileManagement.creteSCARuleFile(rules)

        assertTrue(Files.exists(Paths.get(path)))

        FileManagement.deleteTempFile(path)
    }

    @Test
    fun test004FileManagementShouldCreateFormatFileCorrectly() {
        val rules = listOf(RulesDTO("rule1", "1"), RulesDTO("rule2", "2"))
        val path = FileManagement.createFormatRuleFile(rules)

        assertTrue(Files.exists(Paths.get(path)))

        FileManagement.deleteTempFile(path)
    }
}
