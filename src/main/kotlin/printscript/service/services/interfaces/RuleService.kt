package printscript.service.services.interfaces

import org.springframework.security.oauth2.jwt.Jwt
import reactor.core.publisher.Mono

interface RuleService {
    fun getFormatRules(userData: Jwt): Mono<String>

    fun getLintingRules(userData: Jwt): Mono<String>
}
