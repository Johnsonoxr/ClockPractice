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
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.Constants
import com.johnson.sketchclock.common.EType
import com.johnson.sketchclock.common.Element
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.addCancelObserverView
import com.johnson.sketchclock.common.launchWhenStarted
import com.johnson.sketchclock.common.removeCancelObserverView
import com.johnson.sketchclock.common.scaleIn
import com.johnson.sketchclock.common.scaleOut
import com.johnson.sketchclock.databinding.FragmentEditorBinding
import com.johnson.sketchclock.template_editor.SimpleFontSelectorFragment.Companion.showFontSelectorDialog
import com.johnson.sketchclock.template_editor.SimpleIllustrationSelectorFragment.Companion.showIllustrationSelectorDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.lang.ref.WeakReference

private const val TEMPLATE = "template"

@SuppressLint("NotifyDataSetChanged")
@AndroidEntryPoint
class EditorFragment : Fragment() {

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

        launchWhenStarted {
            viewModel.contentUpdated.collectLatest {
                vb.controlView.render()
                if (it == "tint") {
                    handleTintTypeChange(viewModel.selectedElements.value)
                }
            }
        }

        launchWhenStarted {
            viewModel.elements.collectLatest {
                vb.controlView.render()
            }
        }

        launchWhenStarted {
            viewModel.selectedElements.collectLatest { selectedElements ->
                vb.controlView.selectedElements = selectedElements
                showAddFab(selectedElements.isEmpty())
                showOptionButtons(selectedElements.isNotEmpty())
                handleTintTypeChange(selectedElements)
            }
        }

        launchWhenStarted {
            viewModel.templateSaved.collectLatest {
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
            }
        }

        launchWhenStarted {
            viewModel.layerUpEnabled.collectLatest { enabled ->
                vb.fabLayerUp.isEnabled = enabled
            }
        }

        launchWhenStarted {
            viewModel.layerDownEnabled.collectLatest { enabled ->
                vb.fabLayerDown.isEnabled = enabled
            }
        }

        vb.controlView.viewModelRef = WeakReference(viewModel)

        vb.fabAdd.setOnClickListener {
            showAddTemplateButtons(!vb.fabAddTime24h.isShown)
        }

        setupAddTemplateFab(vb.fabAddTime24h, EType.Hour1, EType.Hour2, EType.Colon, EType.Minute1, EType.Minute2)
        setupAddTemplateFab(vb.fabAddTime12h, EType.Hour12Hr1, EType.Hour12Hr2, EType.Colon, EType.Minute1, EType.Minute2, EType.AmPm)
        setupAddTemplateFab(vb.fabAddDate, EType.Month1, EType.Month2, EType.Slash, EType.Day1, EType.Day2)

        vb.fabAddIllustration.setOnClickListener {
            showIllustrationSelectorDialog { illustration ->
                vb.fabAdd.removeCancelObserverView()
                showAddTemplateButtons(false)
                val element = createIllustrationElement(illustration)
                viewModel.onEvent(EditorEvent.AddElements(listOf(element)))
                viewModel.onEvent(EditorEvent.SetSelectedElements(listOf(element)))
            }
        }

        vb.fabOptionFont.setOnClickListener {
            val charElements = viewModel.selectedElements.value.filter { it.eType.isCharacter() }
            showFontSelectorDialog { font ->
                viewModel.onEvent(EditorEvent.ChangeRes(charElements, font))
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

            vb.colorPicker.addCancelObserverView {
                showTintControlPanel(false)
                showOptionButtons(true)
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

    private fun setupAddTemplateFab(addTemplateBtn: View, vararg eTypes: EType) {
        addTemplateBtn.setOnClickListener {
            showFontSelectorDialog { font ->
                vb.fabAdd.removeCancelObserverView()
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
            listOf(vb.fabAddTime12h, vb.fabAddTime24h, vb.fabAddDate, vb.fabAddIllustration).forEach { fab -> fab.scaleIn() }
        } else {
            vb.fabAdd.animate().setInterpolator(OvershootInterpolator()).rotation(0f).setDuration(300).start()
            vb.fabAdd.removeCancelObserverView()
            listOf(vb.fabAddTime12h, vb.fabAddTime24h, vb.fabAddDate, vb.fabAddIllustration).forEach { fab -> fab.scaleOut() }
        }
    }

    private fun showOptionButtons(show: Boolean) {
        val elements = viewModel.selectedElements.value

        mapOf(
            vb.fabLayerUp to true,
            vb.fabLayerDown to true,
            vb.fabOptionColor to true,
            vb.fabOptionEdit to viewModel.isSelectedElementsEditable.value,
            vb.fabOptionFont to (elements.all { it.eType.isCharacter() }),   //  only characters can be changed font
        ).forEach { (fab, visible) ->
            if (show && visible) fab.scaleIn() else fab.scaleOut()
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

    private fun createIllustrationElement(illustration: Illustration): Element {
        return Element(eType = EType.Illustration, resName = illustration.resName, matrixArray = Matrix().let { matrix ->
            matrix.preTranslate(.5f * Constants.TEMPLATE_WIDTH, .5f * Constants.TEMPLATE_HEIGHT)
            matrix.preScale(0.8f, 0.8f)
            FloatArray(9).apply { matrix.getValues(this) }
        })
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