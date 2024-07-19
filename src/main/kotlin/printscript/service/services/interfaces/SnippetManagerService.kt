package printscript.service.services.interfaces

import org.springframework.security.oauth2.jwt.Jwt
import printscript.service.dto.StatusDTO

interface SnippetManagerService {
    fun updateSnippetStatus(
        newStatus: StatusDTO,
        userJwt: Jwt,
    )
}
