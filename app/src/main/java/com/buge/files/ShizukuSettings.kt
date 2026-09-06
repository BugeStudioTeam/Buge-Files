package com.buge.files

data class ShizukuSettings(
    val enabled: Boolean = false,
    val installerName: String = "",
    val preferShizukuForInstall: Boolean = true
)