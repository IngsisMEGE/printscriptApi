package printscript.service.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import printscript.service.dto.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@AutoConfigureMockMvc
class FormatIntegrationTest {
    @LocalServerPort
    private val port: Int = 0

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    val jwt =
        Jwt.withTokenValue("test")
            .header("alg", "RS256") // Add the algorithm header (you may adjust this based on your JWT)
            .claim("email", "test@test.com") // Extract other claims as needed
            .build()

    @Test
    fun test001FormatSnippetSuccessfully() {
        val snippetData = SnippetData(1L, Language.Printscript)
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/format/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isOk)
            .andExpect(content().json("null"))
    }

    @Test
    fun test002FormatSnippetWithRulesSuccessfully() {
        val snippetData = SnippetData(1L, Language.Printscript)
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/format/withRules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isOk)
            .andExpect(content().json("null"))
    }

    @Test
    fun test003FormatSnippetAndSaveSuccessfully() {
        val snippetData = SnippetData(1L, Language.Printscript)
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/format/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isOk)
            .andExpect(content().json("null"))
    }

    @Test
    fun test004FormatSnippetWithRulesAndSaveSuccessfully() {
        val snippetData = SnippetData(1L, Language.Printscript)
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/format/withRules/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isOk)
            .andExpect(content().json("null"))
    }

    @Test
    fun test006FormatSnippetAndErrorDueToLinting() {
        val snippetData = SnippetData(1L, Language.Printscript)
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/format/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().json("null"))
    }

    @Test
    fun test007FormatSnippetWithRulesAndErrorDueToLinting() {
        val snippetData = SnippetData(1L, Language.Printscript)
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/format/withRules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().json("null"))
    }

    @Test
    fun test008FormatSnippetAndSaveAndErrorDueToLinting() {
        val snippetData = SnippetData(1L, Language.Printscript)
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/format/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().json("null"))
    }

    @Test
    fun test009FormatSnippetWithRulesAndSaveAndErrorDueToLinting() {
        val snippetData = SnippetData(1L, Language.Printscript)
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/format/withRules/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().json("null"))
    }

    @Test
    fun test010FormatFileWithNotSupportedLanguageShouldThrowError() {
        val snippetData = SnippetData(1L, Language.Java)
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/format/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().json("null"))
    }
}
