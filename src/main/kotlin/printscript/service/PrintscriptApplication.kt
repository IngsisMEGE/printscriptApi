package printscript.service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PrintscriptApplication

fun main(args: Array<String>) {
    runApplication<PrintscriptApplication>(*args)
}
