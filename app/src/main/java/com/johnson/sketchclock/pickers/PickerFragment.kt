package com.johnson.sketchclock.pickers

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuProvider
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.collectLatestWhenResumed
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.common.showDialog
import com.johnson.sketchclock.common.showEditTextDialog
import com.johnson.sketchclock.common.tintBackgroundAttr
import com.johnson.sketchclock.databinding.FragmentPickerBinding

abstract class PickerFragment<T, ViewBinding, out VM : PickerViewModel<T>> : Fragment(), OnFabClickListener {

    private lateinit var vb: FragmentPickerBinding

    @Suppress("PropertyName")
    abstract val TAG: String

    //  view model
    abstract val viewModel: VM

    //  item
    abstract fun T.editable(): Boolean
    abstract fun T.title(): String
    abstract fun T.createTime(): Long
    abstract fun T.clone(title: String? = null, bookmarked: Boolean? = null): T
    abstract fun T.isBookmarked(): Boolean
    abstract fun createEmptyItem(): T

    //  adapter
    abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean
    abstract fun areContentsTheSame(oldItem: T, newItem: T): Boolean
    abstract fun createEditItemIntent(item: T): Intent

    //  view binding
    abstract fun createItemViewBinding(parent: ViewGroup): ViewBinding
    abstract val ViewBinding.rootView: View
    abstract val ViewBinding.title: TextView
    abstract fun ViewBinding.bind(item: T)

    //  menu
    open val isAdapterColumnChangeable: Boolean = true

    private var allItems: List<T> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        fun createLayoutManager(columnCount: Int): RecyclerView.LayoutManager {
            return when (columnCount) {
                1 -> LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                else -> GridLayoutManager(context, viewModel.adapterColumnCount.value)
            }
        }

        vb.rv.layoutManager = createLayoutManager(viewModel.adapterColumnCount.value)
        val adapter = ItemAdapter()
        vb.rv.adapter = adapter

        viewModel.repository.getFlow().collectLatestWhenStarted(this) { items ->
            allItems = items
            adapter.items = sortAndFilterItems(items = items)
        }

        viewModel.selectedItems.collectLatestWhenStarted(this) { adapter.selectedItems = it }

        viewModel.adapterColumnCount.collectLatestWhenStarted(this) { columnCount ->
            vb.rv.layoutManager = createLayoutManager(columnCount)
            updateMenuItem(columnCount = columnCount)
        }

        //  since the FAB is shared with other fragments, we'd like to handle it in resume state
        viewModel.controlMode.collectLatestWhenResumed(this) { controlMode ->
            Log.d(TAG, "controlMode=$controlMode")
            backPressedCallback.isEnabled = controlMode != ControlMode.NORMAL
            updateMenuItem(controlMode = controlMode)
            (activity as? ControllableFabHolder)?.changeFabControlMode(controlMode)
            adapter.notifyDataSetChanged()
        }

        viewModel.deletedItem.collectLatestWhenResumed(this) {
            Snackbar.make(vb.rv, "Deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo") { viewModel.onEvent(PickerEvent.UndoDelete()) }
                .setAnchorView(R.id.fab_add)
                .show()
        }

        viewModel.sortType.collectLatestWhenResumed(this) { sortType ->
            adapter.items = sortAndFilterItems(sortType = sortType)
        }

        viewModel.filterType.collectLatestWhenResumed(this) { filterType ->
            adapter.items = sortAndFilterItems(filterType = filterType)
        }

        activity?.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
        activity?.onBackPressedDispatcher?.addCallback(backPressedCallback.apply { isEnabled = true })
    }

    private fun sortAndFilterItems(
        items: List<T> = allItems,
        sortType: SortType = viewModel.sortType.value,
        filterType: FilterType = viewModel.filterType.value
    ): List<T> {
        val filtering: ((T) -> Boolean) = when (filterType) {
            FilterType.ALL -> { _ -> true }
            FilterType.DEFAULT -> { item -> !item.editable() }
            FilterType.CUSTOM -> { item -> item.editable() }
            FilterType.BOOKMARKED -> { item -> item.isBookmarked() }
        }
        val filteredItems = items.filter(filtering)

        return when (sortType) {
            SortType.NAME -> filteredItems.sortedBy { it.title() }
            SortType.NAME_REVERSE -> filteredItems.sortedByDescending { it.title() }
            SortType.DATE -> filteredItems.sortedBy {
                return@sortedBy when {
                    it.editable() -> it.createTime() + (10L * 365L * 24L * 60L * 60L * 1000L)
                    else -> it.createTime()
                }
            }

            SortType.DATE_REVERSE -> filteredItems.sortedByDescending {
                return@sortedByDescending when {
                    it.editable() -> it.createTime() + (10L * 365L * 24L * 60L * 60L * 1000L)
                    else -> it.createTime()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.NORMAL))
        backPressedCallback.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        //  though the control mode is always NORMAL when resumed, I think it's better to set it explicitly.
        backPressedCallback.isEnabled = viewModel.controlMode.value != ControlMode.NORMAL
    }

    override fun onFabClick() {
        when (viewModel.controlMode.value) {
            ControlMode.DELETE -> {
                val items = viewModel.selectedItems.value
                if (items.isEmpty()) {
                    viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.NORMAL))
                    return
                }
                showDialog("Delete", "Are you sure you want to delete these items?") {
                    viewModel.onEvent(PickerEvent.Delete(items))
                    viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.NORMAL))
                }
            }

            ControlMode.BOOKMARK -> {
                val items = viewModel.selectedItems.value
                if (items.isEmpty()) {
                    viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.NORMAL))
                    return
                }
                viewModel.onEvent(PickerEvent.Update(items.map { it.clone(bookmarked = !it.isBookmarked()) }))
                viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.NORMAL))
            }

            else -> {
                val newItem = createEmptyItem()
                showEditTextDialog("Create", newItem.title()) { title ->
                    viewModel.onEvent(PickerEvent.Add(listOf(newItem.clone(title = title))))
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentPickerBinding.inflate(inflater, container, false).apply { vb = this }.root
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_picker_bottombar, menu)
            this@PickerFragment.menu = menu
            updateMenuItem()
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.menu_back -> viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.NORMAL))
                R.id.menu_delete -> viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.DELETE))
                R.id.menu_bookmark -> viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.BOOKMARK))
                R.id.menu_grid -> viewModel.onEvent(PickerEvent.ChangeAdapterColumns(1))
                R.id.menu_rows -> viewModel.onEvent(PickerEvent.ChangeAdapterColumns(2))
                R.id.menu_sort -> showSortDialog { viewModel.onEvent(PickerEvent.ChangeSortType(it)) }
                R.id.menu_filter -> showFilterDialog { viewModel.onEvent(PickerEvent.ChangeFilterType(it)) }
            }
            return true
        }
    }

    private fun updateMenuItem(
        columnCount: Int = viewModel.adapterColumnCount.value,
        controlMode: ControlMode = viewModel.controlMode.value
    ) {
        val isNormal = controlMode == ControlMode.NORMAL
        menu?.findItem(R.id.menu_grid)?.isVisible = isAdapterColumnChangeable && columnCount > 1
        menu?.findItem(R.id.menu_rows)?.isVisible = isAdapterColumnChangeable && columnCount == 1
        menu?.findItem(R.id.menu_back)?.isVisible = !isNormal
        menu?.findItem(R.id.menu_delete)?.isVisible = isNormal
        menu?.findItem(R.id.menu_bookmark)?.isVisible = isNormal
    }

    private var menu: Menu? = null

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (viewModel.controlMode.value != ControlMode.NORMAL) {
                viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.NORMAL))
            } else {
                isEnabled = false
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    private fun showSortDialog(onSelected: (SortType) -> Unit) {
        val sortTypes = SortType.values()
        val singleChoices = sortTypes.map { getString(it.stringRes) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(singleChoices, sortTypes.indexOf(viewModel.sortType.value)) { dialog, which ->
                onSelected(sortTypes[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showFilterDialog(onSelected: (FilterType) -> Unit) {
        val filterTypes = FilterType.values()
        val singleChoices = filterTypes.map { getString(it.stringRes) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.filter_by)
            .setSingleChoiceItems(singleChoices, filterTypes.indexOf(viewModel.filterType.value)) { dialog, which ->
                onSelected(filterTypes[which])
                dialog.dismiss()
            }
            .show()
    }

    private inner class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

        var items: List<T> = emptyList()
            set(value) {
                DiffUtil.calculateDiff(DiffCallback(field, value)).dispatchUpdatesTo(this)

                if (field.isNotEmpty()) {
                    //  scroll to the last added item, except the first time
                    value.findLast { it !in field }?.let { newFont ->
                        val position = value.indexOf(newFont)
                        vb.rv.postDelayed(100) { vb.rv.smoothScrollToPosition(position) }
                    }
                }

                field = value
            }

        var selectedItems: List<T> = emptyList()
            set(value) {
                val diffIndices = ((value - field.toSet()) + (field - value.toSet())).map { items.indexOf(it) }
                field = value
                diffIndices.forEach { notifyItemChanged(it) }
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(createItemViewBinding(parent))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int {
            return items.size
        }

        inner class ViewHolder(val vb: ViewBinding) : RecyclerView.ViewHolder(vb.rootView), View.OnClickListener {

            private val item: T
                get() = items[bindingAdapterPosition]

            init {
                vb.rootView.setOnClickListener(this)
                vb.title.setOnClickListener(this)
                vb.rootView.setOnLongClickListener { showPopupMenu(vb.rootView) }
            }

            fun bind(item: T) {
                vb.title.isClickable = viewModel.controlMode.value == ControlMode.NORMAL
                val isVisuallySelected = when (viewModel.controlMode.value) {
                    ControlMode.DELETE -> item in viewModel.selectedItems.value
                    ControlMode.BOOKMARK -> item.isBookmarked() xor (item in viewModel.selectedItems.value)
                    ControlMode.NORMAL -> false
                }
                val tintBackgroundAttr = when {
                    isVisuallySelected -> com.google.android.material.R.attr.colorErrorContainer
                    else -> com.google.android.material.R.attr.colorPrimaryContainer
                }
                vb.rootView.tintBackgroundAttr(tintBackgroundAttr)
                vb.title.text = item.title()
                vb.bind(item)
            }

            private fun updateSelection() {
                val selected = if (item in viewModel.selectedItems.value) {
                    viewModel.selectedItems.value - item
                } else {
                    viewModel.selectedItems.value + item
                }
                viewModel.onEvent(PickerEvent.Select(selected))
            }

            private fun showPopupMenu(v: View): Boolean {
                val popup = PopupMenu(requireContext(), v)
                popup.menuInflater.inflate(R.menu.menu_picker_pop, popup.menu)

                popup.setOnMenuItemClickListener { menuItem: MenuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_delete -> {
                            if (!item.editable()) {
                                Toast.makeText(context, "Cannot delete this item", Toast.LENGTH_SHORT).show()
                            } else if (item.isBookmarked()) {
                                Toast.makeText(context, "Cannot delete bookmarked item", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.onEvent(PickerEvent.Delete(listOf(item)))
                            }
                        }

                        R.id.menu_bookmark -> {
                            viewModel.onEvent(PickerEvent.Update(listOf(item.clone(bookmarked = !item.isBookmarked()))))
                        }

                        R.id.menu_rename -> {
                            if (item.editable()) {
                                showEditTextDialog("Rename", item.title()) { newName ->
                                    viewModel.onEvent(PickerEvent.Update(listOf(item.clone(title = newName))))
                                }
                            } else {
                                Toast.makeText(context, "Cannot edit this item", Toast.LENGTH_SHORT).show()
                            }
                        }

                        R.id.menu_copy -> {
                            viewModel.onEvent(PickerEvent.Copy(item.clone(title = "${item.title()} Copy")))
                        }
                    }
                    true
                }

                popup.setForceShowIcon(true)
                popup.show()
                return true
            }

            override fun onClick(v: View) {
                when (v) {
                    vb.rootView -> {
                        when (viewModel.controlMode.value) {
                            ControlMode.DELETE -> {
                                if (!item.editable()) {
                                    Toast.makeText(context, "Cannot delete this item", Toast.LENGTH_SHORT).show()
                                    return
                                } else if (item.isBookmarked()) {
                                    Toast.makeText(context, "Cannot delete bookmarked item", Toast.LENGTH_SHORT).show()
                                    return
                                }
                                updateSelection()
                            }

                            ControlMode.BOOKMARK -> updateSelection()

                            ControlMode.NORMAL -> {
                                if (item.editable()) {
                                    startActivity(createEditItemIntent(item))
                                } else {
                                    Toast.makeText(context, "Cannot edit this item", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    vb.title -> {
                        if (!item.editable()) {
                            Toast.makeText(context, "Cannot edit this item", Toast.LENGTH_SHORT).show()
                            return
                        }
                        showEditTextDialog("Rename", item.title()) { newName ->
                            viewModel.onEvent(PickerEvent.Update(listOf(item.clone(title = newName))))
                        }
                    }
                }
            }
        }
    }

    private inner class DiffCallback(private val oldList: List<T>, private val newList: List<T>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areItemsTheSame(oldList[oldItemPosition], newList[newItemPosition])
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return areContentsTheSame(oldList[oldItemPosition], newList[newItemPosition])
        }
    }
}