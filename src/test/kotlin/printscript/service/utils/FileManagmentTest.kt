package printscript.service.utils

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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
}
