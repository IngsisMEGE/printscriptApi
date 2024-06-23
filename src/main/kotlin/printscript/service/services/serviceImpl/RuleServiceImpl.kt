package printscript.service.services.serviceImpl

import io.github.cdimascio.dotenv.Dotenv
import org.apache.coyote.BadRequestException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import printscript.service.exceptions.NotFoundException
import printscript.service.services.interfaces.RuleService
import reactor.core.publisher.Mono

@Service
class RuleServiceImpl(
    @Autowired private val webClient: WebClient,
    @Autowired private val dotenv: Dotenv,
) : RuleService {
    private val ruleAPIURL = dotenv["RULE_URL"]

    override fun getFormatRules(): Mono<String> {
        return webClient.post()
            .uri("$ruleAPIURL/rules/format")
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
}
