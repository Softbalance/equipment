package ru.softbalance.equipment.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ru.softbalance.equipment.R

class PosiflexFragment : BaseFragment() {

    companion object {
        fun newInstance(): PosiflexFragment {
            return PosiflexFragment()
        }
    }

    override val title: String
        get() = getString(R.string.equipment_lib_title_posiflex)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_posiflex, container, false)
        return view
    }
}