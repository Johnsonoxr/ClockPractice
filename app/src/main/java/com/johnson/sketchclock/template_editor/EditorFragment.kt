package com.johnson.sketchclock.template_editor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.johnson.sketchclock.databinding.FragmentEditorBinding
import com.johnson.sketchclock.common.Template
import com.johnson.sketchclock.common.createTimeTemplate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.elements.collectLatest { pieces ->
                vb.controlView.elements = pieces
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.fontLoaded.collectLatest { font ->
                vb.controlView.font = font
            }
        }

        vb.controlView.visualizer = viewModel.visualizer

        vb.fabDone.setOnClickListener {
            viewModel.onEvent(EditorEvent.Save)
        }

        vb.fabClear.setOnClickListener {
            viewModel.onEvent(EditorEvent.RemovePieces(viewModel.elements.value ?: emptyList()))
        }

        vb.fabAddTime.setOnClickListener {
            viewModel.onEvent(EditorEvent.AddPieces(createTimeTemplate()))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return FragmentEditorBinding.inflate(inflater, container, false).also { vb = it }.root
    }

    companion object {
        fun newInstance(template: Template) = EditorFragment().apply {
            arguments = bundleOf(
                TEMPLATE to template
            )
        }
    }
}