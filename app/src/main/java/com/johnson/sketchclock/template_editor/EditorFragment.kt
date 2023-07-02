package com.johnson.sketchclock.template_editor

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.johnson.sketchclock.databinding.ItemCanvasColorSelectorBinding
import com.johnson.sketchclock.template_editor.SimpleFontSelectorFragment.Companion.showFontSelectorDialog
import com.johnson.sketchclock.template_editor.SimpleIllustrationSelectorFragment.Companion.showIllustrationSelectorDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.lang.ref.WeakReference

private const val TEMPLATE = "template"

@AndroidEntryPoint
class EditorFragment : Fragment() {

    private val viewModel: EditorViewModel by activityViewModels()

    private lateinit var vb: FragmentEditorBinding

    private lateinit var colorPrimitiveAdapter: ColorPrimitiveAdapter
    private lateinit var colorSecondaryAdapter: ColorSecondaryAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        colorPrimitiveAdapter = ColorPrimitiveAdapter()
        colorSecondaryAdapter = ColorSecondaryAdapter()
        colorSecondaryAdapter.colorTint = colorPrimitiveAdapter.colors[0]
        vb.rvColorPrimary.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        vb.rvColorSecondary.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        vb.rvColorPrimary.adapter = colorPrimitiveAdapter
        vb.rvColorSecondary.adapter = colorSecondaryAdapter

        if (!viewModel.isInitialized) {
            (arguments?.getSerializable(TEMPLATE) as? Template)?.let { template ->
                Log.d("EditorFragment", "onViewCreated: $template")
                viewModel.onEvent(EditorEvent.Init(template))
            }
        }

        launchWhenStarted {
            viewModel.elements.collectLatest { pieces ->
                vb.controlView.elements = pieces
            }
        }

        launchWhenStarted {
            viewModel.contentUpdated.collectLatest {
                vb.controlView.render()
            }
        }

        launchWhenStarted {
            viewModel.selectedElements.collectLatest { selectedElements ->
                vb.controlView.selectedElements = selectedElements
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
            showAddFabs(!vb.fabAddTime.isShown)
        }

        vb.fabAddTime.setOnClickListener {
            showFontSelectorDialog { font ->
                showAddFabs(false)
                val elements = createTimeElements(font)
                viewModel.onEvent(EditorEvent.AddElements(elements))
                viewModel.onEvent(EditorEvent.SetSelectedElements(elements))
            }
        }

        vb.fabAddDate.setOnClickListener {
            showFontSelectorDialog { font ->
                showAddFabs(false)
                val elements = createDateElements(font)
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
            vb.colorContainer.scaleIn()
            vb.colorContainer.addCancelObserverView { vb.colorContainer.scaleOut() }
        }
    }

    private fun showAddFabs(show: Boolean) {
        if (show) {
            vb.fabAdd.animate().setInterpolator(OvershootInterpolator()).rotation(45.0f).setDuration(300).start()
            listOf(vb.fabAddTime, vb.fabAddDate, vb.fabAddIllustration).forEach { fab -> fab.scaleIn() }
        } else {
            vb.fabAdd.animate().setInterpolator(OvershootInterpolator()).rotation(0f).setDuration(300).start()
            listOf(vb.fabAddTime, vb.fabAddDate, vb.fabAddIllustration).forEach { fab -> fab.scaleOut() }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentEditorBinding.inflate(inflater, container, false).also { vb = it }.root
    }

    private fun createTimeElements(font: Font): List<Element> {
        return listOf(
            EType.Hour1,
            EType.Hour2,
            EType.Colon,
            EType.Minute1,
            EType.Minute2
        ).mapIndexed { index, eType ->
            Element(eType = eType, resName = font.resName, matrixArray = Matrix().let { matrix ->
                matrix.preTranslate(.5f * Constants.TEMPLATE_WIDTH, .5f * Constants.TEMPLATE_HEIGHT)
                matrix.preTranslate(.5f * (index - 2f) * Constants.NUMBER_WIDTH, 0f)
                matrix.preScale(0.5f, 0.5f)
                FloatArray(9).apply { matrix.getValues(this) }
            })
        }
    }

    private fun createDateElements(font: Font): List<Element> {
        return listOf(
            EType.Month1,
            EType.Month2,
            EType.Slash,
            EType.Day1,
            EType.Day2,
        ).mapIndexed { index, eType ->
            Element(eType = eType, resName = font.resName, matrixArray = Matrix().let { matrix ->
                matrix.preTranslate(.5f * Constants.TEMPLATE_WIDTH, .5f * Constants.TEMPLATE_HEIGHT)
                matrix.preTranslate(.5f * (index - 2f) * Constants.NUMBER_WIDTH, 0f)
                matrix.preScale(0.5f, 0.5f)
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

    private inner class ColorPrimitiveAdapter : RecyclerView.Adapter<ColorPrimitiveAdapter.ViewHolder>() {

        val colors = arrayOf(
            Color.WHITE,
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            getAttrColor(requireContext(), android.R.attr.colorPrimary),
        )

        inner class ViewHolder(val binding: ItemCanvasColorSelectorBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
            init {
                binding.root.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                colorSecondaryAdapter.colorTint = colors[adapterPosition]
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemCanvasColorSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount() = colors.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.iv.imageTintList = ColorStateList.valueOf(colors[position])
        }

        private fun getAttrColor(context: Context, @AttrRes attr: Int): Int {
            val typedValue = TypedValue()
            context.theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }
    }

    private inner class ColorSecondaryAdapter : RecyclerView.Adapter<ColorSecondaryAdapter.ViewHolder>() {

        var colorTint: Int = 0
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                val lightnessArray = when (field) {
                    Color.WHITE -> (0..5).map { it / 5f }
                    else -> (1..6).map { it / 7f }
                }.asReversed()
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(colorTint, hsl)
                lightnessArray.map { ColorUtils.HSLToColor(floatArrayOf(hsl[0], hsl[1], it)) }.toTypedArray().toIntArray().copyInto(tintList)
                notifyDataSetChanged()
            }

        val tintList: IntArray = IntArray(6)

        inner class ViewHolder(val binding: ItemCanvasColorSelectorBinding) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {
            init {
                binding.root.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                viewModel.onEvent(EditorEvent.SetTint(viewModel.selectedElements.value, tintList[adapterPosition]))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ItemCanvasColorSelectorBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount() = 6

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.binding.iv.imageTintList = ColorStateList.valueOf(tintList[position])

            val tints = viewModel.selectedElements.value.map { it.softTintColor }.distinct()
            val elementSoftTintColor = if (tints.size == 1 && tints.first() != null) tints.first() else null
            holder.binding.ivSelected.isVisible = elementSoftTintColor == tintList[position]
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