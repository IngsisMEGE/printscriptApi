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
class SCAIntegrationTest {
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

    val scaRules =
        listOf(
            RulesDTO("CamelCaseFormat", "true"),
            RulesDTO("SnakeCaseFormat", "false"),
            RulesDTO("MethodNoExpression", "false"),
            RulesDTO("InputNoExpression", "false"),
        )

    val lintRules =
        listOf(
            RulesDTO("STRING_VALUE", "\"(?:\\\\.|[^\"])*\""),
            RulesDTO("STRING_VALUE", "'(?:\\\\.|[^'])*'"),
            RulesDTO("DECLARATION_VARIABLE", "\\blet\\b"),
            RulesDTO("DECLARATION_IMMUTABLE", "\\bconst\\b"),
            RulesDTO("IF_STATEMENT", "\\bif\\b"),
            RulesDTO("ELSE_STATEMENT", "\\}\\s*else"),
            RulesDTO("OPERATOR_PLUS", "\\+"),
            RulesDTO("OPERATOR_MINUS", "-"),
            RulesDTO("OPERATOR_MULTIPLY", "\\*"),
            RulesDTO("OPERATOR_DIVIDE", "/"),
            RulesDTO("DOUBLE_DOTS", ":"),
            RulesDTO("SEPARATOR", ";"),
            RulesDTO("ASSIGNATION", "="),
            RulesDTO("LEFT_PARENTHESIS", "\\("),
            RulesDTO("RIGHT_PARENTHESIS", "\\)"),
            RulesDTO("LEFT_BRACKET", "\\{"),
            RulesDTO("RIGHT_BRACKET", "\\}"),
            RulesDTO("METHOD_CALL", "\\b\\w+\\s*\\((?:[^()]*|\\([^()]*\\))*\\)"),
            RulesDTO("COMA", ","),
            RulesDTO("NUMBER_TYPE", "\\bnumber\\b"),
            RulesDTO("STRING_TYPE", "\\bstring\\b"),
            RulesDTO("BOOLEAN_TYPE", "\\bboolean\\b"),
            RulesDTO("BOOLEAN_VALUE", "\\btrue\\b"),
            RulesDTO("BOOLEAN_VALUE", "\\bfalse\\b"),
            RulesDTO("NUMBER_VALUE", "\\b\\d+\\.?\\d*\\b"),
            RulesDTO("VARIABLE_NAME", "(?<!\")\\b[a-zA-Z_][a-zA-Z0-9_]*\\b(?!\")"),
        )

    @Test
    fun test001AnalyzeCodeSuccessfully() {
        val snippetData = SnippetData(1L, Language.Printscript)
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/sca/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isOk)
            .andExpect(content().json("null"))
    }

    @Test
    fun test002AnalyzeCodeWithRulesSuccessfully() {
        val snippetData = SCASnippetWithRulesDTO(1L, scaRules, Language.Printscript)
        val jsonContent = objectMapper.writeValueAsString(snippetData)

        mockMvc.perform(
            post("/sca/withRules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonContent)
                .header("Authorization", "Bearer ${jwt.tokenValue}"),
        )
            .andExpect(status().isOk)
            .andExpect(content().json("null"))
    }
}
