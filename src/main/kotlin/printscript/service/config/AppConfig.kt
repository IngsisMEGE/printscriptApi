package printscript.service.config

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class AppConfig {
    @Bean
    fun webClient(): WebClient {
        return WebClient.builder().build()
    }

    @Bean
    fun dotenv(): Dotenv {
        return Dotenv.configure()
            .directory("/home/gradle/src")
            .filename(".env")
            .load()    }
}
