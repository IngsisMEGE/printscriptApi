package printscript.service.services.serviceImpl

import io.github.cdimascio.dotenv.Dotenv
import org.apache.coyote.BadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
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
    private val logger: Logger = LoggerFactory.getLogger(RuleManagerServiceImpl::class.java)
    private val ruleAPIURL = dotenv["RULE_URL"]

    override fun getFormatRules(userData: Jwt): Mono<String> {
        logger.debug("Entering getFormatRules for user")
        val headers = createHeaders(userData)
        return webClient.post()
            .uri("$ruleAPIURL/rules/get/user/format")
            .headers { httpHeaders -> httpHeaders.addAll(headers) }
            .bodyValue("")
            .retrieve()
            .onStatus({ status -> status.is4xxClientError }) { response ->
                handleErrorResponse(response)
            }
            .bodyToMono<String>()
            .doOnSuccess {
                logger.info("Successfully retrieved format rules for user")
            }
            .doOnError { error ->
                logger.error("Error retrieving format rules for user", error)
            }
    }

    override fun getSCARules(userData: Jwt): Mono<List<RulesDTO>> {
        logger.debug("Entering getSCARules for user")
        val headers = createHeaders(userData)
        return webClient.post()
            .uri("$ruleAPIURL/rules/get/user/sca")
            .headers { httpHeaders -> httpHeaders.addAll(headers) }
            .bodyValue("")
            .retrieve()
            .onStatus({ status -> status.is4xxClientError }) { response ->
                handleErrorResponse(response)
            }
            .bodyToMono<List<RulesDTO>>()
            .doOnSuccess {
                logger.info("Successfully retrieved SCA rules for user")
            }
            .doOnError { error ->
                logger.error("Error retrieving SCA rules for user", error)
            }
    }

    private fun createHeaders(userData: Jwt): HttpHeaders {
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", "Bearer ${userData.tokenValue}")
            }
        headers.addAll(getHeader())
        return headers
    }

    private fun handleErrorResponse(response: ClientResponse): Mono<out Throwable> {
        return response.bodyToMono<String>().flatMap { errorBody ->
            when (response.statusCode().value()) {
                400 -> {
                    logger.error("Bad Request: $errorBody")
                    Mono.error(BadRequestException("Bad Request: $errorBody"))
                }
                404 -> {
                    logger.error("Not Found: $errorBody")
                    Mono.error(NotFoundException("Not Found: $errorBody"))
                }
                else -> {
                    logger.error("Error: $errorBody")
                    Mono.error(Exception("Error: $errorBody"))
                }
            }
        }
    }

    private fun getHeader(): HttpHeaders {
        val correlationId = MDC.get(CORRELATION_ID_KEY)
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Correlation-Id", correlationId)
        }
    }
}
