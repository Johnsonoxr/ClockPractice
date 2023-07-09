package com.johnson.sketchclock.pickers

sealed class PickerEvent<T> {
    data class Add<T>(val items: List<T>) : PickerEvent<T>()
    data class Delete<T>(val items: List<T>) : PickerEvent<T>()
    data class Update<T>(val items: List<T>) : PickerEvent<T>()
    data class Select<T>(val items: List<T>) : PickerEvent<T>()
    data class ChangeControlMode<T>(val controlMode: ControlMode) : PickerEvent<T>()
    data class ChangeAdapterColumns<T>(val adapterColumnCount: Int) : PickerEvent<T>()
    data class UndoDelete<T>(val nothing: Nothing? = null) : PickerEvent<T>()
}