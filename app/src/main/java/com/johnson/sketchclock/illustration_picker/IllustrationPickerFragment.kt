package com.johnson.sketchclock.illustration_picker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.GlideHelper
import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.common.scaleIn
import com.johnson.sketchclock.common.scaleOut
import com.johnson.sketchclock.common.showDialog
import com.johnson.sketchclock.common.showEditTextDialog
import com.johnson.sketchclock.common.tintBackgroundAttr
import com.johnson.sketchclock.databinding.FragmentPickerBinding
import com.johnson.sketchclock.databinding.ItemIllustrationBinding
import com.johnson.sketchclock.illustration_canvas.IllustrationCanvasActivity
import com.johnson.sketchclock.pickers.ControlMode
import com.johnson.sketchclock.pickers.ControllableFabHolder
import com.johnson.sketchclock.pickers.OnFabClickListener
import com.johnson.sketchclock.repository.illustration.IllustrationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class IllustrationPickerFragment : Fragment(), OnFabClickListener {

    @Inject
    lateinit var illustrationRepository: IllustrationRepository

    private lateinit var vb: FragmentPickerBinding

    private val viewModel: IllustrationPickerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        vb.rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val adapter = IllustrationAdapter()
        vb.rv.adapter = adapter

        illustrationRepository.getIllustrations().collectLatestWhenStarted(this) { adapter.illustrations = it }

        viewModel.selectedIllustrations.collectLatestWhenStarted(this) { adapter.selectedIllustrations = it }

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

        viewModel.deletedIllustration.collectLatestWhenStarted(this) {
            Snackbar.make(vb.rv, "Illustration deleted", Snackbar.LENGTH_LONG)
                .setAction("Undo") { viewModel.onEvent(IllustrationPickerEvent.UndoDeleteIllustration) }
                .setAnchorView(R.id.fab_add)
                .show()
        }

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    override fun onFabClick() {
        when (viewModel.controlMode.value) {
            ControlMode.DELETE -> {
                if (viewModel.selectedIllustrations.value.isEmpty()) {
                    viewModel.onEvent(IllustrationPickerEvent.ChangeControlMode(ControlMode.NORMAL))
                    return
                }
                showDialog("Delete Illustration", "Are you sure you want to delete these illustrations?") {
                    viewModel.onEvent(IllustrationPickerEvent.DeleteIllustrations(viewModel.selectedIllustrations.value))
                    viewModel.onEvent(IllustrationPickerEvent.ChangeControlMode(ControlMode.NORMAL))
                }
            }

            ControlMode.BOOKMARK -> Toast.makeText(context, "Bookmark", Toast.LENGTH_SHORT).show()

            else -> viewModel.onEvent(IllustrationPickerEvent.AddIllustrations(listOf(Illustration(title = "new illustration"))))
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
                R.id.menu_delete -> viewModel.onEvent(IllustrationPickerEvent.ChangeControlMode(ControlMode.DELETE))
                R.id.menu_bookmark -> viewModel.onEvent(IllustrationPickerEvent.ChangeControlMode(ControlMode.BOOKMARK))
            }
            return true
        }
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (viewModel.controlMode.value != ControlMode.NORMAL) {
                viewModel.onEvent(IllustrationPickerEvent.ChangeControlMode(ControlMode.NORMAL))
            } else {
                isEnabled = false
                activity?.onBackPressedDispatcher?.onBackPressed()
            }
        }
    }

    private inner class IllustrationAdapter : RecyclerView.Adapter<IllustrationAdapter.ViewHolder>() {
        var illustrations: List<Illustration> = emptyList()
            set(value) {
                DiffUtil.calculateDiff(DiffCallback(field, value)).dispatchUpdatesTo(this)

                value.find { it !in field }?.let { newIllustration ->
                    val position = value.indexOf(newIllustration)
                    vb.rv.postDelayed(100) { vb.rv.smoothScrollToPosition(position) }
                }

                field = value
            }

        var selectedIllustrations: List<Illustration> = emptyList()
            set(value) {
                val diffIndices = ((value - field.toSet()) + (field - value.toSet())).map { illustrations.indexOf(it) }
                field = value
                diffIndices.forEach { notifyItemChanged(it) }
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemIllustrationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(illustrations[position])
        }

        override fun getItemCount(): Int {
            return illustrations.size
        }

        inner class ViewHolder(val vb: ItemIllustrationBinding) : RecyclerView.ViewHolder(vb.root), View.OnClickListener {

            private val illustration: Illustration
                get() = illustrations[adapterPosition]

            init {
                vb.tvName.setOnClickListener(this)
                vb.root.setOnClickListener(this)
            }

            fun bind(illustration: Illustration) {
                vb.tvName.text = illustration.title
                vb.root.tintBackgroundAttr(
                    when (illustration) {
                        in selectedIllustrations -> com.google.android.material.R.attr.colorErrorContainer
                        else -> com.google.android.material.R.attr.colorPrimaryContainer
                    }
                )
                GlideHelper.load(vb.ivPreview, illustrationRepository.getIllustrationFile(illustration))
            }

            override fun onClick(v: View) {
                when (v) {
                    vb.root -> {
                        when (viewModel.controlMode.value) {
                            ControlMode.DELETE, ControlMode.BOOKMARK -> {
                                if (ControlMode.DELETE == viewModel.controlMode.value && !illustration.editable) {
                                    Toast.makeText(context, "This illustration is not deletable", Toast.LENGTH_SHORT).show()
                                    return
                                }
                                val selectedIllustrations = if (illustration in viewModel.selectedIllustrations.value) {
                                    viewModel.selectedIllustrations.value - illustration
                                } else {
                                    viewModel.selectedIllustrations.value + illustration
                                }
                                viewModel.onEvent(IllustrationPickerEvent.SetSelectIllustrations(selectedIllustrations))
                            }

                            else -> {
                                if (illustration.editable) {
                                    startActivity(IllustrationCanvasActivity.createIntent(requireContext(), illustration))
                                } else {
                                    Toast.makeText(context, "This illustration is not editable", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    vb.tvName -> {
                        if (!illustration.editable) {
                            Toast.makeText(context, "This illustration is not editable", Toast.LENGTH_SHORT).show()
                            return
                        }
                        showEditTextDialog("Rename Illustration", illustration.title) { newName ->
                            viewModel.onEvent(IllustrationPickerEvent.UpdateIllustration(illustration.copy(title = newName)))
                        }
                    }
                }
            }
        }
    }

    private class DiffCallback(private val oldList: List<Illustration>, private val newList: List<Illustration>) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].resName == newList[newItemPosition].resName
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].resName == newList[newItemPosition].resName
                    && oldList[oldItemPosition].lastModified == newList[newItemPosition].lastModified
                    && oldList[oldItemPosition].title == newList[newItemPosition].title
        }
    }
}