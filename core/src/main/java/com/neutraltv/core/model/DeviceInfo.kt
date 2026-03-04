package com.neutraltv.core.model

data class DeviceInfo(
    val deviceName: String,
    val deviceType: DeviceType,
    val appVersion: String = "1.0.0",
    val apiVersion: Int = 1
)

enum class DeviceType {
    TV,
    MOBILE
}
