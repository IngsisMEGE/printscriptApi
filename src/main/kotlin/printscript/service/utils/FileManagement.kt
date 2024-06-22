package printscript.service.utils

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class FileManagement {
    companion object {
        fun createTempFileWithContent(content: String): String {
            val tempFile = Files.createTempFile("snippet-", ".txt")
            Files.write(tempFile, content.toByteArray(), StandardOpenOption.WRITE)
            return tempFile.toString()
        }

        fun deleteTempFile(filePath: String) {
            try {
                Files.deleteIfExists(Paths.get(filePath))
            } catch (e: Exception) {
                println("Error deleting temp file: $filePath")
            }
        }
    }
}
