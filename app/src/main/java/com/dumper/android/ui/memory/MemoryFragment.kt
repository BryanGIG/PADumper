package com.dumper.android.ui.memory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.dumper.android.core.MainActivity
import com.dumper.android.databinding.FragmentMemoryBinding
import com.dumper.android.ui.console.ConsoleViewModel

class MemoryFragment : Fragment() {

    private var _binding: FragmentMemoryBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val memViewModel: MemoryViewModel by activityViewModels()
    private val console: ConsoleViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMemoryBinding.inflate(inflater, container, false)

        memViewModel.packageName.observe(viewLifecycleOwner) {
            binding.processText.editText?.setText(it)
        }

            selectedApps.observe(viewLifecycleOwner) {
                binding.processText.editText?.setText(it)
            }

            libName.observe(viewLifecycleOwner) {
                binding.libName.editText?.setText(it)
            }

            isFixELF.observe(viewLifecycleOwner) {
                binding.autoFix.isChecked = it
            }

            isDumpMetadata.observe(viewLifecycleOwner) {
                binding.metadata.isChecked = it
            }
        }

        binding.selectApps.setOnClickListener {
            getMainActivity()?.sendRequestAllProcess()
        }

        binding.dumpButton.setOnClickListener {
            val process = binding.processText.editText!!.text.toString()
            val libName = binding.libName.editText!!.text.toString()
            if (process.isBlank()) {
                Toast.makeText(context, "Process name is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (libName.isBlank()) {
                Toast.makeText(context, "Lib name is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            console.appendLine("==========================\nProcess : $process")

            val listDump = mutableListOf(libName)
            if (binding.metadata.isChecked)
                listDump.add("global-metadata.dat")

            getMainActivity()?.sendRequestDump(
                process,
                listDump.toTypedArray(),
                binding.autoFix.isChecked
            )
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //Save state into view-models before destroying view
        with(memViewModel) {
            packageName.value = binding.processText.editText?.text.toString()
            libName.value = binding.libName.editText?.text.toString()
            isFixELF.value = binding.autoFix.isChecked
            isDumpMetadata.value = binding.metadata.isChecked
        }

        with(sharedPref!!.edit()) {
            putString("packageName", binding.processText.editText?.text.toString())
            putString("libName", binding.libName.editText?.text.toString())
            commit()
        }
        _binding = null
    }

    private fun getMainActivity(): MainActivity? {
        val activity = requireActivity()
        return if (activity is MainActivity) {
            activity
        } else {
            null
        }
    }
}