package printscript.service.services.serviceImpl

import io.github.cdimascio.dotenv.Dotenv
import org.apache.coyote.BadRequestException
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import printscript.service.dto.RulesDTO
import printscript.service.exceptions.NotFoundException
import printscript.service.log.CorrIdFilter.Companion.CORRELATION_ID_KEY
import printscript.service.services.interfaces.RuleManagerService
import reactor.core.publisher.Mono

@Service
class RuleManagerServiceImpl(
    @Autowired private val webClient: WebClient,
    @Autowired private val dotenv: Dotenv,
) : RuleManagerService {
    private val ruleAPIURL = dotenv["RULE_URL"]

    override fun getFormatRules(userData: Jwt): Mono<String> {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", "Bearer ${userData.tokenValue}")
            }
        headers.addAll(getHeader())
        return webClient.post()
            .uri("$ruleAPIURL/rules/get/user/format")
            .headers { httpHeaders -> httpHeaders.addAll(headers) }
            .bodyValue("")
            .retrieve()
            .onStatus({ status -> status.is4xxClientError }) { response ->
                response.bodyToMono<String>().flatMap { errorBody ->
                    when (response.statusCode().value()) {
                        400 -> Mono.error(BadRequestException("Bad Request Getting Snippet: $errorBody"))
                        404 -> Mono.error(NotFoundException("Not Found Getting Snippet: $errorBody"))
                        else -> Mono.error(Exception("Error Getting Snippet: $errorBody"))
                    }
                }
            }
            .bodyToMono<String>()
    }

    override fun getLintingRules(userData: Jwt): Mono<List<RulesDTO>> {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", "Bearer ${userData.tokenValue}")
            }
        headers.addAll(getHeader())
        return webClient.post()
            .uri("$ruleAPIURL/rules/get/user/lint")
            .headers { httpHeaders -> httpHeaders.addAll(headers) }
            .bodyValue("")
            .retrieve()
            .onStatus({ status -> status.is4xxClientError }) { response ->
                response.bodyToMono<String>().flatMap { errorBody ->
                    when (response.statusCode().value()) {
                        400 -> Mono.error(BadRequestException("Bad Request Getting Snippet: $errorBody"))
                        404 -> Mono.error(NotFoundException("Not Found Getting Snippet: $errorBody"))
                        else -> Mono.error(Exception("Error Getting Snippet: $errorBody"))
                    }
                }
            }
            .bodyToMono<List<RulesDTO>>()
    }

    override fun getSCARules(userData: Jwt): Mono<List<RulesDTO>> {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", "Bearer ${userData.tokenValue}")
            }
        headers.addAll(getHeader())
        return webClient.post()
            .uri("$ruleAPIURL/rules/get/user/sca")
            .headers { httpHeaders -> httpHeaders.addAll(headers) }
            .bodyValue("")
            .retrieve()
            .onStatus({ status -> status.is4xxClientError }) { response ->
                response.bodyToMono<String>().flatMap { errorBody ->
                    when (response.statusCode().value()) {
                        400 -> Mono.error(BadRequestException("Bad Request Getting Snippet: $errorBody"))
                        404 -> Mono.error(NotFoundException("Not Found Getting Snippet: $errorBody"))
                        else -> Mono.error(Exception("Error Getting Snippet: $errorBody"))
                    }
                }
            }
            .bodyToMono<List<RulesDTO>>()
    }

    override fun callbackFormat(
        snippetFormated: String,
        userData: Jwt,
    ): Mono<Void> {
        return webClient.post()
            .uri("$ruleAPIURL/rules")
            .header("Authorization", "Bearer ${userData.tokenValue}")
            .bodyValue(snippetFormated)
            .retrieve()
            .bodyToMono()
    }

    private fun getHeader(): HttpHeaders {
        val correlationId = MDC.get(CORRELATION_ID_KEY)
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Correlation-Id", correlationId)
            }
        return headers
    }
}
