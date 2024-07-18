package printscript.service.log

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Component
class RequestLogFilter : OncePerRequestFilter() {
    private val logger: Logger = LoggerFactory.getLogger(RequestLogFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val uri = request.requestURI
        val method = request.method.toString()
        val prefix = "$method $uri"
        try {
            filterChain.doFilter(request, response)
        } finally {
            val statusCode = response.status
            logger.info("$prefix - $statusCode")
        }
    }
}
