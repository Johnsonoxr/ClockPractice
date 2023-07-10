package com.johnson.sketchclock.pickers

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.johnson.sketchclock.repository.pref.PreferenceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class PickerViewModel<Item> : ViewModel() {

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

    val adapterColumnCount: StateFlow<Int> by lazy { preferenceRepository.getIntFlow("$TAG-adapterColumnCount", defaultColumnCount) }

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
                    preferenceRepository.put("$TAG-adapterColumnCount", event.adapterColumnCount)
                }
            }
        }
    }
}