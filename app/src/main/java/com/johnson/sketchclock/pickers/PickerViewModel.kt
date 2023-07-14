package com.johnson.sketchclock.pickers

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnson.sketchclock.repository.pref.PreferenceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class PickerViewModel<Item> : ViewModel() {

    private val columnCountKey get() = "$TAG-columnCount"
    private val sortTypeKey get() = "$TAG-sortType"
    private val filterTypeKey get() = "$TAG-filterType"

    @Suppress("PropertyName")
    protected abstract val TAG: String

    protected abstract val preferenceRepository: PreferenceRepository

    private val _deletedItem = MutableSharedFlow<List<Item>>()
    val deletedItem = _deletedItem

    private var recoverableDeletedItem: List<Item>? = null

    private val _controlMode = MutableStateFlow(ControlMode.NORMAL)
    val controlMode: StateFlow<ControlMode> = _controlMode

    private val _selectedItems = MutableStateFlow(emptyList<Item>())
    val selectedItems: StateFlow<List<Item>> = _selectedItems

    val adapterColumnCount: StateFlow<Int> by lazy {
        preferenceRepository
            .getIntFlow(columnCountKey)
            .map { it ?: defaultColumnCount }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), defaultColumnCount)
    }

    val sortType: StateFlow<SortType> by lazy {
        preferenceRepository
            .getStringFlow(sortTypeKey)
            .map { it?.let { SortType.valueOf(it) } ?: SortType.DATE }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SortType.DATE)
    }

    val filterType: StateFlow<FilterType> by lazy {
        preferenceRepository
            .getStringFlow(filterTypeKey)
            .map { it?.let { FilterType.valueOf(it) } ?: FilterType.ALL }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), FilterType.ALL)
    }

    abstract val repository: RepositoryAdapter<Item>

    open val defaultColumnCount: Int = 1

    fun onEvent(event: PickerEvent<Item>) {
        Log.v(TAG, "onEvent: $event")
        viewModelScope.launch {
            when (event) {
                is PickerEvent.Add -> {
                    repository.addItems(event.items)
                }

                is PickerEvent.Delete -> {
                    repository.deleteItems(event.items)
                    recoverableDeletedItem = event.items
                    _deletedItem.emit(event.items)
                    _selectedItems.value = _selectedItems.value - event.items.toSet()
                }

                is PickerEvent.Update -> {
                    repository.updateItems(event.items)
                }

                is PickerEvent.UndoDelete -> {
                    recoverableDeletedItem?.let {
                        repository.addItems(it)
                    }
                    recoverableDeletedItem = null
                }

                is PickerEvent.ChangeControlMode -> {
                    _controlMode.value = event.controlMode
                    _selectedItems.value = emptyList()
                }

                is PickerEvent.Select -> {
                    _selectedItems.value = event.items
                }

                is PickerEvent.ChangeAdapterColumns -> {
                    preferenceRepository.putInt(columnCountKey, event.adapterColumnCount)
                }

                is PickerEvent.ChangeSortType -> {
                    preferenceRepository.putString(sortTypeKey, event.sortType.name)
                }

                is PickerEvent.ChangeFilterType -> {
                    preferenceRepository.putString(filterTypeKey, event.filterType.name)
                }

                is PickerEvent.Copy -> {
                    repository.copyAsNewItem(event.item)
                }
            }
        }
    }
}