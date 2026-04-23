package com.example.proyecto

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform