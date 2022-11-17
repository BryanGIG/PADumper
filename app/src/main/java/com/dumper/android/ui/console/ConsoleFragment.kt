package com.dumper.android.ui.console

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.dumper.android.databinding.FragmentConsoleBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ConsoleFragment : Fragment() {

    private lateinit var consoleBind: FragmentConsoleBinding
    private val vm: ConsoleViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        consoleBind = FragmentConsoleBinding.inflate(layoutInflater, container, false)

        vm.console.observe(viewLifecycleOwner) {
            consoleBind.console.text = "$it\n"
            lifecycleScope.launch {
                delay(10)
                consoleBind.scrollView.fullScroll(View.FOCUS_DOWN)
            }
        }

        return consoleBind.root
    }
}
