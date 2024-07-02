package printscript.service.services.serviceImpl

import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.mono
import org.apache.coyote.BadRequestException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlow
import org.springframework.web.reactive.function.client.bodyToMono
import printscript.service.exceptions.NotFoundException
import printscript.service.services.interfaces.AssetService
import reactor.core.publisher.Mono
import java.awt.image.DataBuffer

@Service
class AssetServiceImpl(
    @Autowired private val webClient: WebClient,
    @Autowired private val dotenv: Dotenv,
) : AssetService {
    private val bucketURL = "${dotenv["BUCKET_URL"]}/v1/asset/snippet"

    override fun getSnippet(snippetId: Long): Mono<String> {
        return mono {
            val response: Flow<DataBuffer> =
                webClient.get()
                    .uri("$bucketURL/$snippetId")
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
            stringBuilder.toString()
        }
    }

    override fun saveSnippet(
        snippetId: Long,
        snippet: String,
    ): Mono<String> {
        return webClient.post()
            .uri("$bucketURL/$snippetId")
            .bodyValue(snippet)
            .retrieve()
            .onStatus({ status -> status.is4xxClientError }) { response ->
                onStatus(response)
            }
            .bodyToMono<String>()
    }

    private fun onStatus(response: ClientResponse): Mono<out Throwable> {
        return response.bodyToMono<String>().flatMap { errorBody ->
            when (response.statusCode().value()) {
                400 -> Mono.error(BadRequestException("Bad Request Getting Snippet: $errorBody"))
                404 -> Mono.error(NotFoundException("Not Found Getting Snippet: $errorBody"))
                else -> Mono.error(Exception("Error Getting Snippet: $errorBody"))
            }
        }
    }
}
