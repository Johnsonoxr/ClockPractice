package com.johnson.sketchclock.template_editor

import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.johnson.sketchclock.common.EType
import com.johnson.sketchclock.common.Element
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.launchWhenStarted
import com.johnson.sketchclock.common.scaleIn
import com.johnson.sketchclock.common.scaleOut
import com.johnson.sketchclock.databinding.FragmentEditorBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import java.lang.ref.WeakReference

private const val TEMPLATE = "template"

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
            viewModel.elements.collectLatest { pieces ->
                vb.controlView.elements = pieces
            }
        }

        launchWhenStarted {
            viewModel.resUpdated.collectLatest {
                vb.controlView.render()
            }
        }

        launchWhenStarted {
            viewModel.selectedElements.collectLatest { selectedElements ->
                vb.controlView.selectedElements = selectedElements
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
            showAddFabs(false)
            viewModel.onEvent(EditorEvent.AddElements(createTimeTemplate()))
        }

        vb.fabAddDate.setOnClickListener {
            showAddFabs(false)
            viewModel.onEvent(EditorEvent.AddElements(createDateTemplate()))
        }

        vb.fabAddIllustration.setOnClickListener {
            showAddFabs(false)
            viewModel.onEvent(EditorEvent.AddElements(listOf(createIllustrationTemplate())))
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

    private fun createTimeTemplate(): List<Element> {
        return listOf(
            EType.Hour1,
            EType.Hour2,
            EType.Colon,
            EType.Minute1,
            EType.Minute2
        ).mapIndexed { index, eType ->
            Element(eType = eType, resId = 0, Matrix().let { matrix ->
                matrix.postScale(0.3f, 0.3f)
                matrix.postTranslate(index * 108.0f - 216, 0.0f)
                FloatArray(9).apply { matrix.getValues(this) }
            })
        }
    }

    private fun createDateTemplate(): List<Element> {
        return listOf(
            EType.Month1,
            EType.Month2,
            EType.Slash,
            EType.Day1,
            EType.Day2,
        ).mapIndexed { index, eType ->
            Element(eType = eType, resId = 0, Matrix().let { matrix ->
                matrix.postScale(0.3f, 0.3f)
                matrix.postTranslate(index * 108.0f - 216, 0.0f)
                FloatArray(9).apply { matrix.getValues(this) }
            })
        }
    }

    private fun createIllustrationTemplate(): Element {
        return Element(eType = EType.Illustration, resId = 0)
    }

    companion object {
        fun newInstance(template: Template) = EditorFragment().apply {
            arguments = bundleOf(
                TEMPLATE to template
            )
        }
    }
}