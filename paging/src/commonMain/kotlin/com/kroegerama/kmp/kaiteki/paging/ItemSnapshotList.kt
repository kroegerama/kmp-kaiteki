package com.kroegerama.kmp.kaiteki.paging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.ItemSnapshotList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

context(vm: ViewModel)
public fun <T> Flow<ItemSnapshotList<T>>.asStateFlow(
    initialList: ItemSnapshotList<T> = emptyItemSnapshotList()
): StateFlow<ItemSnapshotList<T>> = stateIn(vm.viewModelScope, SharingStarted.Lazily, initialList)

public fun <T> emptyItemSnapshotList(): ItemSnapshotList<T> =
    ItemSnapshotList(0, 0, emptyList())
