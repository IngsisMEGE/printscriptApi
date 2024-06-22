package printscript.service.services.serviceImpl

import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactor.mono
import org.apache.coyote.BadRequestException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
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
    private val bucketURL = "${dotenv["BUCKET_URL"]}/v1/asset"

    override fun getSnippet(
        container: String,
        key: Long,
    ): Mono<String> {
        return mono {
            val response: Flow<DataBuffer> =
                webClient.get()
                    .uri("$bucketURL/$container/$key")
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
                    .bodyToFlow<DataBuffer>()

            val stringBuilder = StringBuilder()
            response.collect { dataBuffer ->
                val eventString = dataBuffer.toString()
                stringBuilder.append(eventString)
            }
            stringBuilder.toString()
        }
    }
}
