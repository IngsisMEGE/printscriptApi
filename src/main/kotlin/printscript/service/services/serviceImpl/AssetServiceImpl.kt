package printscript.service.services.serviceImpl

import io.github.cdimascio.dotenv.Dotenv
import org.apache.coyote.BadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import printscript.service.exceptions.NotFoundException
import printscript.service.log.CorrIdFilter.Companion.CORRELATION_ID_KEY
import printscript.service.services.interfaces.AssetService
import reactor.core.publisher.Mono

@Service
class AssetServiceImpl(
    @Autowired private val webClient: WebClient,
    @Autowired private val dotenv: Dotenv,
) : AssetService {
    private val logger: Logger = LoggerFactory.getLogger(AssetServiceImpl::class.java)
    private val bucketURL = "${dotenv["BUCKET_URL"]}/v1/asset/snippet"

    override fun getSnippet(snippetId: Long): Mono<String> {
        logger.debug("Entering getSnippetFromBucket with snippetId: $snippetId")
        val snippetUrl = "$bucketURL/$snippetId"
        val headers = getHeader()
        return try {
            val snippet =
                webClient.get()
                    .uri(snippetUrl)
                    .headers { httpHeaders -> httpHeaders.addAll(headers) }
                    .retrieve()
                    .onStatus({ it.is4xxClientError || it.is5xxServerError }, {
                        logger.error("Error status received while getting snippet with id: $snippetId")
                        it.bodyToMono(String::class.java)
                            .map { errorBody -> Exception("Error getting snippet: $errorBody") }
                    })
                    .bodyToMono(String::class.java)
                    .block() ?: throw Exception("Error getting snippet")
            logger.info("Successfully retrieved snippet with id: $snippetId")
            Mono.just(snippet)
        } catch (e: Exception) {
            logger.error("Error retrieving snippet with id: $snippetId", e)
            throw e
        } finally {
            logger.debug("Exiting getSnippetFromBucket with snippetId: $snippetId")
        }
    }

    override fun saveSnippet(
        snippetId: Long,
        snippet: String,
    ): Mono<String> {
        logger.debug("Entering saveSnippetInBucket with snippetId: $snippetId")
        val snippetUrl = "$bucketURL/$snippetId"
        val headers = getHeader()
        return webClient.post()
            .uri(snippetUrl)
            .bodyValue(snippet)
            .headers { httpHeaders -> httpHeaders.addAll(headers) }
            .retrieve()
            .onStatus({ it.is4xxClientError || it.is5xxServerError }, {
                logger.error("Error status received while saving snippet with id: $snippetId")
                it.bodyToMono(String::class.java)
                    .map { errorBody -> Exception("Error saving snippet: $errorBody") }
            })
            .toBodilessEntity()
            .then(
                Mono.just("Success"),
            )
            .doOnSuccess { logger.info("Successfully saved snippet with id: $snippetId") }
            .doOnError { e -> logger.error("Error saving snippet with id: $snippetId", e) }
            .doFinally { logger.debug("Exiting saveSnippetInBucket with snippetId: $snippetId") }
    }

    private fun onStatus(response: ClientResponse): Mono<out Throwable> {
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
