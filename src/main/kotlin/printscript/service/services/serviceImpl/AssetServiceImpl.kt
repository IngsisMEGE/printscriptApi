package printscript.service.services.serviceImpl

import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.mono
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
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.bodyToMono
import printscript.service.exceptions.NotFoundException
import printscript.service.log.CorrIdFilter.Companion.CORRELATION_ID_KEY
import printscript.service.services.interfaces.AssetService
import reactor.core.publisher.Mono
import java.awt.image.DataBuffer

@Service
class AssetServiceImpl(
    @Autowired private val webClient: WebClient,
    @Autowired private val dotenv: Dotenv,
) : AssetService {
    private val logger: Logger = LoggerFactory.getLogger(AssetServiceImpl::class.java)
    private val bucketURL = "${dotenv["BUCKET_URL"]}/v1/asset/snippet"

    override fun getSnippet(snippetId: Long): Mono<String> {
        logger.debug("Entering getSnippet with snippetId: $snippetId")
        return mono {
            val response: Flow<DataBuffer> =
                webClient.get()
                    .uri("$bucketURL/$snippetId")
                    .headers { headers -> headers.addAll(getHeader()) }
                    .retrieve()
                    .onStatus({ status -> status.is4xxClientError }) { response ->
                        onStatus(response)
                    }
                    .bodyToFlow<DataBuffer>()

            val stringBuilder = StringBuilder()
            response.collect { dataBuffer ->
                val eventString = dataBuffer.toString()
                stringBuilder.append(eventString)
            }
            val snippet = stringBuilder.toString()
            logger.info("Successfully retrieved snippet with id: $snippetId")
            snippet
        }.doOnError { error ->
            logger.error("Error retrieving snippet with id: $snippetId", error)
        }.doFinally {
            logger.debug("Exiting getSnippet with snippetId: $snippetId")
        }
    }

    override fun saveSnippet(
        snippetId: Long,
        snippet: String,
    ): Mono<String> {
        logger.debug("Entering saveSnippet with snippetId: $snippetId")
        return webClient.post()
            .uri("$bucketURL/$snippetId")
            .bodyValue(snippet)
            .headers { headers -> headers.addAll(getHeader()) }
            .retrieve()
            .onStatus({ status -> status.is4xxClientError }) { response ->
                onStatus(response)
            }
            .bodyToMono<String>()
            .doOnSuccess {
                logger.info("Successfully saved snippet with id: $snippetId")
            }.doOnError { error ->
                logger.error("Error saving snippet with id: $snippetId", error)
            }.doFinally {
                logger.debug("Exiting saveSnippet with snippetId: $snippetId")
            }
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
