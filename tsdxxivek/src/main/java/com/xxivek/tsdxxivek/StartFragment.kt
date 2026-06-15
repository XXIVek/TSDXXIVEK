package com.xxivek.tsdxxivek

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.xxivek.tsdxxivek.databinding.FragmentStartBinding


class StartFragment : Fragment() {
    // binding FragmentItemListBinding
    private var _binding: FragmentStartBinding?=null
    val binding get() = _binding!!

    private var mediaController : MediaController? = null

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//     }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // так лучше чем типовой пока
        _binding = FragmentStartBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (mediaController==null){
            mediaController = MediaController(binding.root.context)
            mediaController!!.setAnchorView(binding.videoView)
        }

        val bbb="com.xxivek.tsdxxivek"//TSDXXIVekApplication().packageName
        val aaa=Uri.parse("android.resource://" + bbb + "/" + R.raw.zvezda)
        binding.videoView.setVideoURI(aaa)
        binding.videoView.requestFocus()
        binding.videoView.start()

        binding.videoView.setOnCompletionListener {
            val navControler = binding.root.findNavController()
            navControler.navigate(R.id.action_startFragment_to_logoFragment)
        }
     }
}