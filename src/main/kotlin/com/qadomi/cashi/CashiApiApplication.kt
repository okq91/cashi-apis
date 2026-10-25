package com.qadomi.cashi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CashiApiApplication

fun main(args: Array<String>) {
	runApplication<CashiApiApplication>(*args)
}
