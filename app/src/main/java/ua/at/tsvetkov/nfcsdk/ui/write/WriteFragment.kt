package ua.at.tsvetkov.nfcsdk.ui.write

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import ua.at.tsvetkov.nfcsdk.databinding.FragmentWriteBinding
import ua.at.tsvetkov.nfcsdk.ui.main.NfcViewModel

class WriteFragment : Fragment() {

    private var _binding: FragmentWriteBinding? = null
    private val binding get() = _binding!!

    private val nfcViewModel: NfcViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWriteBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.viewModel = nfcViewModel
        binding.lifecycleOwner = this

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.lifecycleOwner = null
        _binding = null
    }
}
