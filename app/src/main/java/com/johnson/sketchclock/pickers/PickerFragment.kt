package com.johnson.sketchclock.pickers

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.common.scaleIn
import com.johnson.sketchclock.common.scaleOut
import com.johnson.sketchclock.common.showDialog
import com.johnson.sketchclock.common.showEditTextDialog
import com.johnson.sketchclock.common.tintBackgroundAttr
import com.johnson.sketchclock.databinding.FragmentPickerBinding

abstract class PickerFragment<T, ViewBinding, out VM : PickerViewModel<T>> : Fragment(), OnFabClickListener {

    private lateinit var vb: FragmentPickerBinding

    abstract val viewModel: VM
    abstract val repositoryAdapter: RepositoryAdapter<T>
    abstract fun createEmptyItem(): T
    abstract fun T.editable(): Boolean
    abstract fun T.title(): String
    abstract fun createCopyItemWithNewTitle(item: T, title: String): T
    abstract fun createEditItemIntent(item: T): Intent

    abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean
    abstract fun areContentsTheSame(oldItem: T, newItem: T): Boolean

    abstract fun createItemViewBinding(parent: ViewGroup): ViewBinding
    abstract val ViewBinding.rootView: View
    abstract val ViewBinding.title: TextView
    abstract fun ViewBinding.bind(item: T)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        vb.rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val adapter = ItemAdapter()
        vb.rv.adapter = adapter

        repositoryAdapter.getFlow().collectLatestWhenStarted(this) { adapter.items = it }

        viewModel.selectedItems.collectLatestWhenStarted(this) { adapter.selectedItems = it }

        viewModel.controlMode.collectLatestWhenStarted(this) { controlMode ->
            backPressedCallback.isEnabled = controlMode != ControlMode.NORMAL
            when (controlMode) {
                ControlMode.DELETE, ControlMode.BOOKMARK -> activity?.removeMenuProvider(menuProvider)
                else -> activity?.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
            }
            (activity as? ControllableFabHolder)?.editFab { fab ->
                fab.scaleOut(100) {
                    fab.setImageResource(
                        when (controlMode) {
                            ControlMode.DELETE -> R.drawable.bottom_delete
                            ControlMode.BOOKMARK -> R.drawable.bottom_bookmark
                            else -> R.drawable.fab_add
                        }
                    )
                    fab.scaleIn(100)
                }
            }
        }

        viewModel.deletedItem.collectLatestWhenStarted(this) {
            Snackbar.make(vb.rv, "Deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo") { viewModel.onEvent(PickerEvent.UndoDelete()) }
                .setAnchorView(R.id.fab_add)
                .show()
        }

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    override fun onFabClick() {
        when (viewModel.controlMode.value) {
            ControlMode.DELETE -> {
                if (viewModel.selectedItems.value.isEmpty()) {
                    viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.NORMAL))
                    return
                }
                if (viewModel.selectedItems.value.isEmpty()) return
                showDialog("Delete Font", "Are you sure you want to delete these fonts?") {
                    viewModel.onEvent(PickerEvent.Delete(viewModel.selectedItems.value))
                    viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.NORMAL))
                }
            }

            ControlMode.BOOKMARK -> Toast.makeText(context, "Bookmark", Toast.LENGTH_SHORT).show()

            else -> viewModel.onEvent(PickerEvent.Add(listOf(createEmptyItem())))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentPickerBinding.inflate(inflater, container, false).apply { vb = this }.root
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_picker_bottombar, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.menu_delete -> viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.DELETE))
                R.id.menu_bookmark -> viewModel.onEvent(PickerEvent.ChangeControlMode(ControlMode.BOOKMARK))
            }
            return true
        }
    }

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

    private inner class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ViewHolder>() {

        var items: List<T> = emptyList()
            set(value) {
                DiffUtil.calculateDiff(DiffCallback(field, value)).dispatchUpdatesTo(this)

                value.findLast { it !in field }?.let { newFont ->
                    val position = value.indexOf(newFont)
                    vb.rv.postDelayed(100) { vb.rv.smoothScrollToPosition(position) }
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
                get() = items[adapterPosition]

            init {
                vb.title.setOnClickListener(this)
                vb.rootView.setOnClickListener(this)
            }

            fun bind(item: T) {
                vb.title.text = item.title()
                vb.rootView.tintBackgroundAttr(
                    when (item) {
                        in selectedItems -> com.google.android.material.R.attr.colorErrorContainer
                        else -> com.google.android.material.R.attr.colorPrimaryContainer
                    }
                )
                vb.bind(item)
            }

            override fun onClick(v: View) {
                when (v) {
                    vb.rootView -> {
                        when (viewModel.controlMode.value) {
                            ControlMode.DELETE, ControlMode.BOOKMARK -> {
                                if (ControlMode.DELETE == viewModel.controlMode.value && !item.editable()) {
                                    Toast.makeText(context, "This font is not deletable", Toast.LENGTH_SHORT).show()
                                    return
                                }
                                val selected = if (item in viewModel.selectedItems.value) {
                                    viewModel.selectedItems.value - item
                                } else {
                                    viewModel.selectedItems.value + item
                                }
                                viewModel.onEvent(PickerEvent.Select(selected))
                            }

                            else -> {
                                if (item.editable()) {
                                    startActivity(createEditItemIntent(item))
                                } else {
                                    Toast.makeText(context, "This font is not editable", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    vb.title -> {
                        if (!item.editable()) {
                            Toast.makeText(context, "This font is not editable", Toast.LENGTH_SHORT).show()
                            return
                        }
                        showEditTextDialog("Rename", item.title()) { newName ->
                            viewModel.onEvent(PickerEvent.Update(createCopyItemWithNewTitle(item, newName)))
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