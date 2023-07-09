package com.johnson.sketchclock.pickers

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class PickerViewModel<Item> : ViewModel() {

    @Suppress("PropertyName")
    protected abstract val TAG: String

    private val _deletedItem = MutableSharedFlow<List<Item>>()
    val deletedItem = _deletedItem

    private var recoverableDeletedItem: List<Item>? = null

    private val _controlMode = MutableStateFlow(ControlMode.NORMAL)
    val controlMode: StateFlow<ControlMode> = _controlMode

    private val _selectedItems = MutableStateFlow(emptyList<Item>())
    val selectedItems: StateFlow<List<Item>> = _selectedItems

    private val _adapterColumnCount = MutableStateFlow(1)
    val adapterColumnCount: StateFlow<Int> = _adapterColumnCount

    abstract val repository: RepositoryAdapter<Item>

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
                    repository.updateItem(event.item)
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
                    _adapterColumnCount.value = event.adapterColumnCount
                }
            }
        }
    }
}