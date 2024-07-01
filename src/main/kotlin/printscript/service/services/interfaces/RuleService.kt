package printscript.service.services.interfaces

import reactor.core.publisher.Mono

interface RuleService {
    fun getFormatRules(): Mono<String>

    fun getLintingRules(): Mono<String>
}
