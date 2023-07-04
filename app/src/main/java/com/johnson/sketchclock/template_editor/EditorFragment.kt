package com.johnson.sketchclock.template_editor

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.johnson.sketchclock.R
import com.johnson.sketchclock.common.Constants
import com.johnson.sketchclock.common.EType
import com.johnson.sketchclock.common.Element
import com.johnson.sketchclock.common.Font
import com.johnson.sketchclock.common.Illustration
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.addCancelObserverView
import com.johnson.sketchclock.common.launchWhenStarted
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
                    viewModel.selectedElements.value.firstOrNull()?.let { element ->
                        when {
                            element.softTintColor != null -> vb.tgGroupTintType.check(R.id.tg_tint_soft)
                            element.hardTintColor != null -> vb.tgGroupTintType.check(R.id.tg_tint_hard)
                            else -> vb.tgGroupTintType.check(R.id.tg_tint_none)
                        }
                    }
                }
            }
        }

        launchWhenStarted {
            viewModel.selectedElements.collectLatest { selectedElements ->
                vb.controlView.selectedElements = selectedElements
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
        }

        launchWhenStarted {
            viewModel.templateSaved.collectLatest {
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
            }
        }

        vb.controlView.viewModelRef = WeakReference(viewModel)

        vb.fabDone.setOnClickListener {
            viewModel.onEvent(EditorEvent.Save)
        }

        vb.fabAdd.setOnClickListener {
            showAddFabs(!vb.fabAddTime24h.isShown)
        }

        vb.fabAddTime24h.setOnClickListener {
            showFontSelectorDialog { font ->
                showAddFabs(false)
                val elements = createFontElements(font, EType.Hour1, EType.Hour2, EType.Colon, EType.Minute1, EType.Minute2)
                viewModel.onEvent(EditorEvent.AddElements(elements))
                viewModel.onEvent(EditorEvent.SetSelectedElements(elements))
            }
        }

        vb.fabAddTime12h.setOnClickListener {
            showFontSelectorDialog { font ->
                showAddFabs(false)
                val elements = createFontElements(font, EType.Hour12Hr1, EType.Hour12Hr2, EType.Colon, EType.Minute1, EType.Minute2, EType.AmPm)
                viewModel.onEvent(EditorEvent.AddElements(elements))
                viewModel.onEvent(EditorEvent.SetSelectedElements(elements))
            }
        }

        vb.fabAddDate.setOnClickListener {
            showFontSelectorDialog { font ->
                showAddFabs(false)
                val elements = createFontElements(font, EType.Month1, EType.Month2, EType.Slash, EType.Day1, EType.Day2)
                viewModel.onEvent(EditorEvent.AddElements(elements))
                viewModel.onEvent(EditorEvent.SetSelectedElements(elements))
            }
        }

        vb.fabAddIllustration.setOnClickListener {
            showIllustrationSelectorDialog { illustration ->
                showAddFabs(false)
                val element = createIllustrationElement(illustration)
                viewModel.onEvent(EditorEvent.AddElements(listOf(element)))
                viewModel.onEvent(EditorEvent.SetSelectedElements(listOf(element)))
            }
        }

        vb.controlView.onFontOptionClicked = { elements ->
            if (elements.any { it.eType == EType.Illustration }) {
                Toast.makeText(requireContext(), "Illustration cannot be changed", Toast.LENGTH_SHORT).show()
            } else {
                val charElements = elements.filter { it.eType.isCharacter() }
                showFontSelectorDialog { font ->
                    viewModel.onEvent(EditorEvent.ChangeRes(charElements, font))
                }
            }
        }

        vb.controlView.onColorOptionClicked = {
            vb.fabAdd.scaleOut()
            vb.fabDone.scaleOut()
            vb.colorPicker.scaleIn()
            vb.tgGroupTintType.scaleIn()

            vb.colorPicker.addCancelObserverView {
                vb.fabAdd.scaleIn()
                vb.fabDone.scaleIn()
                vb.colorPicker.scaleOut()
                vb.tgGroupTintType.scaleOut()
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

    private fun showAddFabs(show: Boolean) {
        if (show) {
            vb.fabAdd.animate().setInterpolator(OvershootInterpolator()).rotation(45.0f).setDuration(300).start()
            listOf(vb.fabAddTime12h, vb.fabAddTime24h, vb.fabAddDate, vb.fabAddIllustration).forEach { fab -> fab.scaleIn() }
        } else {
            vb.fabAdd.animate().setInterpolator(OvershootInterpolator()).rotation(0f).setDuration(300).start()
            listOf(vb.fabAddTime12h, vb.fabAddTime24h, vb.fabAddDate, vb.fabAddIllustration).forEach { fab -> fab.scaleOut() }
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

    companion object {
        fun newInstance(template: Template) = EditorFragment().apply {
            arguments = bundleOf(
                TEMPLATE to template
            )
        }
    }
}