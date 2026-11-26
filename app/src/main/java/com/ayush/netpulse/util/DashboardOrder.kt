package com.ayush.netpulse.util

internal object DashboardOrder {
    val defaults = listOf("usage_insights", "network_details", "quick_actions")
    fun parse(raw: String?): List<String> {
        val valid = raw.orEmpty().split(",").filter { it in defaults }.distinct()
        return valid + defaults.filterNot { it in valid }
    }
    fun move(order: List<String>, index: Int, offset: Int): List<String> {
        val target = index + offset
        if (index !in order.indices || target !in order.indices) return order
        return order.toMutableList().apply { add(target, removeAt(index)) }
    }
}
