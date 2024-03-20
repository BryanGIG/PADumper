package com.dumper.android.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FeaturedPlayList
import androidx.compose.material.icons.automirrored.outlined.FeaturedPlayList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.dumper.android.R

sealed class Screen(
    val route: String,
    @StringRes val resourceId: Int,
    val iconDefault: ImageVector,
    val iconOutline: ImageVector
) {
    data object Memory : Screen("memory", R.string.home, Icons.Filled.Home, Icons.Outlined.Home)

    data object Console : Screen("console", R.string.log,
        Icons.AutoMirrored.Filled.FeaturedPlayList, Icons.AutoMirrored.Outlined.FeaturedPlayList
    )
}
