package printscript.service.services.serviceImpl

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import printscript.service.dto.StatusDTO
import printscript.service.services.interfaces.SnippetManagerService

@Service
class SnippetManagerImpl(
    private val redisTemplate: RedisTemplate<String, Any>,
    private val objectMapper: ObjectMapper,
) : SnippetManagerService {
    private val logger: Logger = LoggerFactory.getLogger(SnippetManagerImpl::class.java)

    override fun updateSnippetStatus(newStatus: StatusDTO) {
        println("Entering updateSnippetStatus")
        logger.debug("Entering updateSnippetStatus")
        val requestData = objectMapper.writeValueAsString(newStatus)
        logger.debug("Object of Status has Been written to String")
        redisTemplate.opsForList().rightPush("snippet_sca_status", requestData)
        logger.debug("Queued snippet status update request for snippetId: ${newStatus.id}")
    }
}
