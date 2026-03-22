package com.example.pdtranslator

import androidx.annotation.StringRes

data class LibrarySpec(
  @StringRes val nameResId: Int,
  @StringRes val descriptionResId: Int,
  val license: String,
  val version: String,
  val url: String
)

object LibraryCatalog {
  val libraries = listOf(
    LibrarySpec(
      nameResId = R.string.lib_kotlin_name,
      descriptionResId = R.string.lib_kotlin_desc,
      license = "Apache License 2.0",
      version = "1.9.22",
      url = "https://github.com/JetBrains/kotlin"
    ),
    LibrarySpec(
      nameResId = R.string.lib_gson_name,
      descriptionResId = R.string.lib_gson_desc,
      license = "Apache License 2.0",
      version = "2.10.1",
      url = "https://github.com/google/gson"
    ),
    LibrarySpec(
      nameResId = R.string.lib_crash_name,
      descriptionResId = R.string.lib_crash_desc,
      license = "Apache License 2.0",
      version = "2.4.0",
      url = "https://github.com/Ereza/CustomActivityOnCrash"
    ),
    LibrarySpec(
      nameResId = R.string.lib_ktor_name,
      descriptionResId = R.string.lib_ktor_desc,
      license = "Apache License 2.0",
      version = "2.3.8",
      url = "https://ktor.io/"
    ),
    LibrarySpec(
      nameResId = R.string.lib_kotlinx_serialization_name,
      descriptionResId = R.string.lib_kotlinx_serialization_desc,
      license = "Apache License 2.0",
      version = "1.5.1",
      url = "https://github.com/Kotlin/kotlinx.serialization"
    ),
    LibrarySpec(
      nameResId = R.string.lib_kotlinx_coroutines_name,
      descriptionResId = R.string.lib_kotlinx_coroutines_desc,
      license = "Apache License 2.0",
      version = "1.7.1",
      url = "https://github.com/Kotlin/kotlinx.coroutines"
    ),
    LibrarySpec(
      nameResId = R.string.lib_nav_compose_name,
      descriptionResId = R.string.lib_nav_compose_desc,
      license = "Apache License 2.0",
      version = "2.7.7",
      url = "https://developer.android.com/jetpack/androidx/releases/navigation"
    ),
    LibrarySpec(
      nameResId = R.string.lib_core_ktx_name,
      descriptionResId = R.string.lib_core_ktx_desc,
      license = "Apache License 2.0",
      version = "1.12.0",
      url = "https://developer.android.com/jetpack/androidx/releases/core"
    ),
    LibrarySpec(
      nameResId = R.string.lib_splashscreen_name,
      descriptionResId = R.string.lib_splashscreen_desc,
      license = "Apache License 2.0",
      version = "1.0.1",
      url = "https://developer.android.com/jetpack/androidx/releases/core"
    ),
    LibrarySpec(
      nameResId = R.string.lib_lifecycle_name,
      descriptionResId = R.string.lib_lifecycle_desc,
      license = "Apache License 2.0",
      version = "2.6.2",
      url = "https://developer.android.com/jetpack/androidx/releases/lifecycle"
    ),
    LibrarySpec(
      nameResId = R.string.lib_activity_compose_name,
      descriptionResId = R.string.lib_activity_compose_desc,
      license = "Apache License 2.0",
      version = "1.8.2",
      url = "https://developer.android.com/jetpack/androidx/releases/activity"
    ),
    LibrarySpec(
      nameResId = R.string.lib_compose_name,
      descriptionResId = R.string.lib_compose_desc,
      license = "Apache License 2.0",
      version = "2023.08.00",
      url = "https://developer.android.com/jetpack/compose/bom"
    ),
    LibrarySpec(
      nameResId = R.string.lib_material3_name,
      descriptionResId = R.string.lib_material3_desc,
      license = "Apache License 2.0",
      version = "1.1.1",
      url = "https://developer.android.com/jetpack/androidx/releases/compose-material3"
    ),
    LibrarySpec(
      nameResId = R.string.lib_accompanist_name,
      descriptionResId = R.string.lib_accompanist_desc,
      license = "Apache License 2.0",
      version = "0.28.0",
      url = "https://github.com/google/accompanist"
    )
  )
}
