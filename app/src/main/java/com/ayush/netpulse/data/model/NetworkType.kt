package com.ayush.netpulse.data.model

enum class NetworkType(val displayName: String) {
    WIFI("Wi-Fi"),
    CELLULAR("Mobile Data"),
    ETHERNET("Ethernet"),
    DISCONNECTED("Disconnected"),
    UNKNOWN("Unknown")
}
