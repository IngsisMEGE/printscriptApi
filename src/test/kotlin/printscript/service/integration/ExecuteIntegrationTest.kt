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
class ExecuteIntegrationTest {
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
    fun test001ExecuteTestSuccess() {
        val snippetData = SnippetData(1L, Language.Printscript)
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/execute/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isOk)
            .andExpect(content().json("null"))
    }

    @Test
    fun test002ExecuteLiveSuccess() {
        val snippetData = SnippetDataInputs(Language.Printscript, 1L, listOf("1"))
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/execute/live")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isOk)
            .andExpect(content().json("null"))
    }
}
