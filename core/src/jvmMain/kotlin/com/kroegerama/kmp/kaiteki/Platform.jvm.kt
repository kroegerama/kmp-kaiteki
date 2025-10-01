package com.kroegerama.kmp.kaiteki

public actual object Platform {
    public actual val name: String = "Java ${System.getProperty("java.version")}"
}
