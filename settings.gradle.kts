/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Copyright (C) 2026 Buge Studio
 */

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Buge Files"
include(":app")
