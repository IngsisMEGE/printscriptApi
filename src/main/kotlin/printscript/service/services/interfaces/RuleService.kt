package printscript.service.services.interfaces

import org.springframework.security.oauth2.jwt.Jwt
import printscript.service.dto.RulesDTO
import reactor.core.publisher.Mono

interface RuleService {
    fun getFormatRules(userData: Jwt): Mono<String>

    fun getLintingRules(userData: Jwt): Mono<List<RulesDTO>>
}
