package com.kroegerama.kmp.kaiteki.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

@Composable
public inline fun <reified Route : Any, reified VM : ViewModel> NavBackStackEntry.hiltSharedViewModel(
    navController: NavController
): VM {
    val backStackEntry = remember(this) {
        navController.getBackStackEntry<Route>()
    }
    return hiltViewModel<VM>(backStackEntry)
}
