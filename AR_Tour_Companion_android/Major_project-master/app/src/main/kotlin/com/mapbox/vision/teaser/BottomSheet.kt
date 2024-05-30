package com.mapbox.vision.teaser

import android.os.Bundle
//import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
//import android.widget.Toast
import com.mapbox.vision.teaser.databinding.FragmentBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
//import com.mapbox.vision.teaser.MainActivity
import com.mapbox.vision.teaser.ar.ArMapActivity

class BottomSheet : BottomSheetDialogFragment() {

    // _binding is a nullable variable to hold an instance of FragmentBottomSheetBinding(  auto-generated class  created by view binding )
    private var _binding: FragmentBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        return inflater.inflate(R.layout.fragment_bottom_sheet, container, false)
        _binding = FragmentBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val arMapActivity = activity as? ArMapActivity

        //Landmarks
        binding.window.setOnClickListener {
            arMapActivity?.getRouteForLocation(27.67212497482302, 85.42837195113326)
        }

        binding.Badrinath.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.672211100306864, 85.42759909885706)
        }

        binding.bhairavnath.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.670994218610314, 85.42947340939915)
        }

        binding.Dattatreya.setOnClickListener {
            arMapActivity?.getRouteForLocation(27.673627325003874, 85.43533552565205)
        }

        binding.krishna.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.672411440467886, 85.4275816137424)
        }

        binding.nyatapola.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.6715303247031, 85.4293660542616)
        }
        binding.Rameshwar.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.672396529871776, 85.42757368773006)
        }
        binding.siddhiLaxmi.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.67231405875478, 85.42874236507815)
        }
        binding.silluMahadev.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.673028434564316, 85.42913226125552)
        }
        binding.vatsalaDevi.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.67214063046286, 85.42839357856808)
        }

        //Restaurants
        binding.Yomari.setOnClickListener {
            arMapActivity?.getRouteForLocation(27.67221448575251, 85.4282172799386)
        }

        binding.Bara.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.671962302225328, 85.4293992223205)
        }

        binding.black.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.671400576372957, 85.42984242742475)
        }

        binding.Garuda.setOnClickListener {
            arMapActivity?.getRouteForLocation(27.67103383846626, 85.42883571490216)
        }

        binding.hungry.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.671787626378347, 85.42812279825372)
        }

        binding.namaste.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.67149463033739, 85.42952535563131)
        }
        binding.taumadhi.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.671312638966647, 85.42888984864913)
        }
        binding.gold.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.671969333978343, 85.42942431027684)
        }
        binding.Totey.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.67175632460405, 85.42852540798764)
        }
        binding.watshala.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.671909624420113, 85.42861689464868)
        }

        //Hotel & Guest House
        binding.goldenGate.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.672343023303313, 85.4284349734595)
        }

        binding.empire.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.67189673556418, 85.42939448233318)
        }

        binding.traditional.setOnClickListener {
            arMapActivity?.getRouteForLocation(27.6721350382901, 85.42794305143119)
        }

        binding.inn.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.672925035235725, 85.42924721075175)
        }

        binding.chhen.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.6722590188639, 85.42727070232014)
        }
        binding.kumari.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.67177574333922, 85.42882546775685)
        }
        binding.shiva.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.672109444434142, 85.42857225122752)
        }
        binding.siddhi.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.6715120357665, 85.42957857448896)
        }
        binding.tushita.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.671602543118453, 85.42971436721136)
        }

        //ATMs & Money Exchange
        binding.bktremit.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.672625423850395, 85.42723692184911)
        }

        binding.citizen.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.672339250811724, 85.42681907456144)
        }

        binding.durbar.setOnClickListener {
            arMapActivity?.getRouteForLocation(27.672087226480826, 85.42861028252123)
        }

        binding.money.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.672522140715685, 85.42723733492402)
        }

        binding.nabil.setOnClickListener {
            arMapActivity?.getRouteForLocation(27.67312783194954, 85.42896111628238)
        }

        binding.limited.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.6726241280286, 85.42737496644321)
        }
        binding.investment.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.672683515139646, 85.42719910083011)
        }
        binding.nyatapolmoney.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.67167263145704, 85.42909741187157)
        }
        binding.temple.setOnClickListener{
            arMapActivity?.getRouteForLocation(27.67244622352352, 85.42716366567566)
        }



    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}