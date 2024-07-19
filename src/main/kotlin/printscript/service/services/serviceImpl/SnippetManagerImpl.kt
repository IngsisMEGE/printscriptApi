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
import printscript.service.dto.StatusDTO
import printscript.service.exceptions.NotFoundException
import printscript.service.log.CorrIdFilter.Companion.CORRELATION_ID_KEY
import printscript.service.services.interfaces.SnippetManagerService
import reactor.core.publisher.Mono

@Service
class SnippetManagerImpl(
    @Autowired private val webClient: WebClient,
    @Autowired private val dotenv: Dotenv,
) : SnippetManagerService {
    private val snippetManagerURL = dotenv["SNIPPET_MANAGER_URL"]
    private val logger: Logger = LoggerFactory.getLogger(SnippetManagerImpl::class.java)

    override fun updateSnippetStatus(
        newStatus: StatusDTO,
        userJwt: Jwt,
    ) {
        logger.info("Updating snippet status with id: ${newStatus.id}")
        val headers = createHeaders(userJwt)

        webClient.post()
            .uri("$snippetManagerURL/snippetManager/update/status")
            .headers { httpHeaders -> httpHeaders.addAll(headers) }
            .bodyValue(newStatus)
            .retrieve()
            .onStatus(
                { it.is4xxClientError || it.is5xxServerError },
            ) { response ->
                handleErrorResponse(response)
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
