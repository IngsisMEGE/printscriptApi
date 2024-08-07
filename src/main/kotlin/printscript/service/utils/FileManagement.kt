package printscript.service.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import printscript.service.dto.RulesDTO
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

        fun createLexerRuleFile(rules: List<RulesDTO>): String {
            val objectMapper = ObjectMapper().registerModule(KotlinModule())

            val rulesMap =
                rules.associate { rule ->
                    rule.value to
                        mapOf(
                            "pattern" to rule.value,
                            "type" to rule.name,
                        )
                }

            val jsonString = objectMapper.writeValueAsString(rulesMap)

            return createTempFileWithContent(jsonString)
        }

        fun creteSCARuleFile(rules: List<RulesDTO>): String {
            val map = rules.associate { it.name to it.value.toBoolean() }

            val jsonObject = JsonObject(map.mapValues { JsonPrimitive(it.value) })

            val json = Json.encodeToString(JsonObject.serializer(), jsonObject)

            return createTempFileWithContent(json)
        }
    }
}
