package com.johnson.sketchclock.template_editor

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.johnson.sketchclock.R
import com.johnson.sketchclock.canvas.StickerCanvasActivity
import com.johnson.sketchclock.common.Constants
import com.johnson.sketchclock.common.EType
import com.johnson.sketchclock.common.Element
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.common.Hand
import com.johnson.sketchclock.common.Sticker
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.addCancelObserverView
import com.johnson.sketchclock.common.collectLatestWhenStarted
import com.johnson.sketchclock.common.removeCancelObserverView
import com.johnson.sketchclock.common.scaleIn
import com.johnson.sketchclock.common.scaleOut
import com.johnson.sketchclock.databinding.FragmentEditorBinding
import com.johnson.sketchclock.repository.sticker.StickerRepository
import com.johnson.sketchclock.template_editor.SimpleFontSelectorFragment.*
import com.johnson.sketchclock.template_editor.SimpleFontSelectorFragment.Companion.showFontSelectorDialog
import com.johnson.sketchclock.template_editor.SimpleHandSelectorFragment.Companion.showHandSelectorDialog
import com.johnson.sketchclock.template_editor.SimpleStickerSelectorFragment.Companion.showStickerSelectorDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import java.lang.ref.WeakReference
import javax.inject.Inject

private const val TEMPLATE = "template"

@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class EditorFragment : Fragment() {

    @Inject
    lateinit var stickerRepository: StickerRepository

    private val viewModel: EditorViewModel by activityViewModels()

    private lateinit var vb: FragmentEditorBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        activity?.addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        if (!viewModel.isInitialized) {
            (arguments?.getSerializable(TEMPLATE) as? Template)?.let { template ->
                Log.d("EditorFragment", "onViewCreated: $template")
                viewModel.onEvent(EditorEvent.Init(template))
            }
        }

        viewModel.contentUpdated.collectLatestWhenStarted(this) {
            vb.controlView.render()
            if (it == "tint") {
                handleTintTypeChange(viewModel.selectedElements.value)
            }
        }

        viewModel.elements.collectLatestWhenStarted(this) {
            vb.controlView.render()
            if (it.isNotEmpty())
                vb.fabSelectAll.scaleIn()
            else
                vb.fabSelectAll.scaleOut()
        }

        viewModel.elements.combine(viewModel.selectedElements) { es, ses -> es.size == ses.size }.collectLatestWhenStarted(this) { isAllSelected ->
            vb.fabSelectAll.setImageResource(
                if (isAllSelected)
                    R.drawable.fab_deselect
                else
                    R.drawable.fab_select_all
            )
        }

        viewModel.selectedElements.collectLatestWhenStarted(this) { selectedElements ->
            vb.controlView.selectedElements = selectedElements
            showAddFab(selectedElements.isEmpty())
            showOptionButtons(selectedElements.isNotEmpty())
            handleTintTypeChange(selectedElements)
        }

        viewModel.templateSaved.collectLatestWhenStarted(this) {
            Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
        }

        viewModel.layerUpEnabled.collectLatestWhenStarted(this) { enabled -> vb.fabLayerUp.isEnabled = enabled }

        viewModel.layerDownEnabled.collectLatestWhenStarted(this) { enabled -> vb.fabLayerDown.isEnabled = enabled }

        viewModel.errorMessage.collectLatestWhenStarted(this) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }

        stickerRepository.getStickers().collectLatestWhenStarted(this) { vb.controlView.render() }

        vb.controlView.viewModelRef = WeakReference(viewModel)

        vb.fabAdd.setOnClickListener {
            showAddTemplateButtons(!vb.fabAddTime24h.isShown)
        }

        setupAddTemplateFab(Type.HOUR_24H, vb.fabAddTime24h, EType.Hour1, EType.Hour2, EType.Colon, EType.Minute1, EType.Minute2)
        setupAddTemplateFab(Type.HOUR_12H, vb.fabAddTime12h, EType.Hour12Hr1, EType.Hour12Hr2, EType.Colon, EType.Minute1, EType.Minute2, EType.AmPm)
        setupAddTemplateFab(Type.DATE, vb.fabAddDate, EType.Month1, EType.Month2, EType.Slash, EType.Day1, EType.Day2)

        vb.fabAddHands.setOnClickListener {
            showHandSelectorDialog { hand ->
                showAddTemplateButtons(false)
                val elements = createHandElements(hand)
                viewModel.onEvent(EditorEvent.AddElements(elements))
                viewModel.onEvent(EditorEvent.SetSelectedElements(elements))
            }
        }

        vb.fabAddSticker.setOnClickListener {
            showStickerSelectorDialog { sticker ->
                showAddTemplateButtons(false)
                val element = createStickerElement(sticker)
                viewModel.onEvent(EditorEvent.AddElements(listOf(element)))
                viewModel.onEvent(EditorEvent.SetSelectedElements(listOf(element)))
            }
        }

        vb.fabSelectAll.setOnClickListener {
            if (viewModel.selectedElements.value.size == viewModel.elements.value.size) {
                viewModel.onEvent(EditorEvent.SetSelectedElements(emptyList()))
            } else {
                viewModel.onEvent(EditorEvent.SetSelectedElements(viewModel.elements.value))
            }
        }

        vb.fabOptionFont.setOnClickListener {
            val charElements = viewModel.selectedElements.value.filter { it.eType.isCharacter() }
            showFontSelectorDialog(Type.NONE) { font ->
                font.resName?.let { viewModel.onEvent(EditorEvent.ChangeRes(charElements, it)) }
            }
        }

        vb.fabOptionClock.setOnClickListener {
            val handElements = viewModel.selectedElements.value.filter { it.eType.isHand() }
            showHandSelectorDialog { hand ->
                hand.resName?.let { viewModel.onEvent(EditorEvent.ChangeRes(handElements, it)) }
            }
        }

        vb.fabLayerUp.setOnClickListener {
            viewModel.onEvent(EditorEvent.LayerUp(viewModel.selectedElements.value))
        }

        vb.fabLayerDown.setOnClickListener {
            viewModel.onEvent(EditorEvent.LayerDown(viewModel.selectedElements.value))
        }

        vb.fabOptionColor.setOnClickListener {
            showTintControlPanel(true)
            showOptionButtons(false)
            vb.fabSelectAll.scaleOut()

            vb.colorPicker.addCancelObserverView {
                showTintControlPanel(false)
                showOptionButtons(true)
                vb.fabSelectAll.scaleIn()
            }
        }

        vb.fabOptionEdit.setOnClickListener {
            val sticker = viewModel.selectedElements.value.firstOrNull()?.resName
                ?.let { resName -> stickerRepository.getStickerByRes(resName) }
            if (sticker != null) {
                startActivity(StickerCanvasActivity.createIntent(requireContext(), sticker))
            }
        }

        vb.tgTintNone.setOnClickListener {
            viewModel.onEvent(EditorEvent.SetTint(viewModel.selectedElements.value))
        }

        vb.tgTintHard.setOnClickListener {
            vb.colorPicker.selectedColor?.let { color ->
                viewModel.onEvent(EditorEvent.SetTint(viewModel.selectedElements.value, hardTint = color))
            }
        }

        vb.tgTintSoft.setOnClickListener {
            vb.colorPicker.selectedColor?.let { color ->
                viewModel.onEvent(EditorEvent.SetTint(viewModel.selectedElements.value, softTint = color))
            }
        }

        vb.colorPicker.onColorSelected = { color ->
            when (vb.tgGroupTintType.checkedButtonId) {
                vb.tgTintSoft.id -> viewModel.onEvent(EditorEvent.SetTint(viewModel.selectedElements.value, softTint = color))
                else -> viewModel.onEvent(EditorEvent.SetTint(viewModel.selectedElements.value, hardTint = color))  // for both none and hard
            }
        }

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, true) {
            showSaveDialogIfNeed {
                activity?.finish()
            }
        }
    }

    private fun showSaveDialogIfNeed(block: () -> Unit) {
        if (viewModel.isTemplateSaved) {
            block()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setMessage("Save changes?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.onEvent(EditorEvent.Save)
                block()
            }
            .setNegativeButton("No") { _, _ -> block() }
            .show()
    }

    private fun handleTintTypeChange(selectedElements: List<Element>) {
        val hardTintColors = selectedElements.map { it.hardTintColor }.distinct()
        val softTintColors = selectedElements.map { it.softTintColor }.distinct()
        when {
            hardTintColors.size == 1 && hardTintColors.firstOrNull() != null -> {   //  same hard tint
                vb.colorPicker.selectedColor = hardTintColors.firstOrNull()
                vb.tgGroupTintType.check(R.id.tg_tint_hard)
            }

            softTintColors.size == 1 && softTintColors.firstOrNull() != null -> {   //  same soft tint
                vb.colorPicker.selectedColor = softTintColors.firstOrNull()
                vb.tgGroupTintType.check(R.id.tg_tint_soft)
            }

            hardTintColors.size == 1 && softTintColors.size == 1 -> {   //  both hard and soft tint are null, which means no tint
                vb.colorPicker.selectedColor = null
                vb.tgGroupTintType.check(R.id.tg_tint_none)
            }

            else -> {   //  multiple hard or soft tint
                vb.colorPicker.selectedColor = null
                vb.tgGroupTintType.clearChecked()
            }
        }
    }

    private fun showAddFab(show: Boolean) {
        if (show) {
            vb.fabAdd.scaleIn()
        } else {
            vb.fabAdd.scaleOut()
        }
    }

    private fun setupAddTemplateFab(type: Type, addTemplateBtn: View, vararg eTypes: EType) {
        addTemplateBtn.setOnClickListener {
            showFontSelectorDialog(type) { font ->
                showAddTemplateButtons(false)
                val elements = createFontElements(font, *eTypes)
                viewModel.onEvent(EditorEvent.AddElements(elements))
                viewModel.onEvent(EditorEvent.SetSelectedElements(elements))
            }
        }
    }

    private fun showAddTemplateButtons(show: Boolean) {
        if (show) {
            vb.fabAdd.animate().setInterpolator(OvershootInterpolator()).rotation(45.0f).setDuration(300).start()
            vb.fabAdd.addCancelObserverView {
                showAddTemplateButtons(false)
            }
            vb.fabSelectAll.scaleOut()
            listOf(vb.fabAddTime12h, vb.fabAddTime24h, vb.fabAddDate, vb.fabAddHands, vb.fabAddSticker).forEach { it.scaleIn() }
        } else {
            vb.fabAdd.animate().setInterpolator(OvershootInterpolator()).rotation(0f).setDuration(300).start()
            vb.fabAdd.removeCancelObserverView()
            vb.fabSelectAll.scaleIn()
            listOf(vb.fabAddTime12h, vb.fabAddTime24h, vb.fabAddDate, vb.fabAddHands, vb.fabAddSticker).forEach { it.scaleOut() }
        }
    }

    private fun showOptionButtons(show: Boolean) {
        val elements = viewModel.selectedElements.value

        val isAnEditableSticker = elements.size == 1
                && elements.firstOrNull()?.eType == EType.Sticker
                && elements.firstOrNull()?.resName?.let { stickerRepository.getStickerByRes(it)?.editable } == true

        mapOf(
            vb.fabLayerUp to true,
            vb.fabLayerDown to true,
            vb.fabOptionColor to true,
            vb.fabOptionEdit to isAnEditableSticker,
            vb.fabOptionFont to (elements.all { it.eType.isCharacter() }),
            vb.fabOptionClock to (elements.all { it.eType.isHand() }),
        ).forEach { (fab, validOption) ->
            if (show && validOption) {
                if (!fab.isVisible) fab.scaleIn()
            } else {
                if (fab.isVisible) fab.scaleOut()
            }
        }
    }

    private fun showTintControlPanel(show: Boolean) {
        if (show) {
            vb.colorPicker.scaleIn()
            vb.tgGroupTintType.scaleIn()
        } else {
            vb.colorPicker.scaleOut()
            vb.tgGroupTintType.scaleOut()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentEditorBinding.inflate(inflater, container, false).also { vb = it }.root
    }

    private fun createFontElements(font: Font, vararg eTypes: EType): List<Element> {
        val anchorMatrix = Matrix().apply {
            preTranslate(Constants.TEMPLATE_WIDTH / 2f, Constants.TEMPLATE_HEIGHT / 2f)
            preScale(0.5f, 0.5f)
        }
        return eTypes.mapIndexed { index, eType ->
            Element(eType = eType, resName = font.resName, matrixArray = Matrix().let { matrix ->
                matrix.preTranslate((index - (eTypes.size - 1) / 2f) * eType.width(), 0f)
                matrix.postConcat(anchorMatrix)
                FloatArray(9).apply { matrix.getValues(this) }
            })
        }
    }

    private fun createStickerElement(sticker: Sticker): Element {
        return Element(eType = EType.Sticker, resName = sticker.resName, matrixArray = Matrix().let { matrix ->
            matrix.preTranslate(.5f * Constants.TEMPLATE_WIDTH, .5f * Constants.TEMPLATE_HEIGHT)
//            matrix.preScale(0.8f, 0.8f)
            FloatArray(9).apply { matrix.getValues(this) }
        })
    }

    private fun createHandElements(hand: Hand): List<Element> {
        return listOf(EType.HourHand, EType.MinuteHand).map { eType ->
            Element(eType = eType, resName = hand.resName, matrixArray = Matrix().let { matrix ->
                matrix.preTranslate(.5f * Constants.TEMPLATE_WIDTH, .5f * Constants.TEMPLATE_HEIGHT)
                FloatArray(9).apply { matrix.getValues(this) }
            })
        }
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_canvas, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_save -> {
                    viewModel.onEvent(EditorEvent.Save)
                    true
                }

                else -> false
            }
        }
    }

    companion object {
        fun newInstance(template: Template) = EditorFragment().apply {
            arguments = bundleOf(
                TEMPLATE to template
            )
        }
    }
}