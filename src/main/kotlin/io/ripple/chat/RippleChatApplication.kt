package io.ripple.chat

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RippleChatApplication

fun main(args: Array<String>) {
    runApplication<RippleChatApplication>(*args)
}
