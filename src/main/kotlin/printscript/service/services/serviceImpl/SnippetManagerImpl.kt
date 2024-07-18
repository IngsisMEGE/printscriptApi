package printscript.service.services.serviceImpl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import printscript.service.dto.StatusDTO
import printscript.service.services.interfaces.SnippetManagerService

@Service
class SnippetManagerImpl(private val redisTemplate: RedisTemplate<String, Any>) : SnippetManagerService {
    private val objectMapper = jacksonObjectMapper()

    override fun updateSnippetStatus(newStatus: StatusDTO) {
        val requestData = objectMapper.writeValueAsString(newStatus)
        redisTemplate.opsForList().rightPush("snippet_sca_queue", requestData)
    }
}
