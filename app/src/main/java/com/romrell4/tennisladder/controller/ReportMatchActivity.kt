package com.romrell4.tennisladder.controller

import android.os.Bundle
import android.widget.NumberPicker
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.support.TLActivity
import kotlinx.android.synthetic.main.activity_report_match.*

class ReportMatchActivity: TLActivity() {
	companion object {
		const val PLAYER_EXTRA = "match-player"
	}

	private lateinit var requiredPickers: List<NumberPicker>
	private lateinit var nonRequiredPickers: List<NumberPicker>
	private val pickers: List<NumberPicker>
		get() = requiredPickers + nonRequiredPickers

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_report_match)

		requiredPickers = listOf(winner_set1_picker, loser_set1_picker, winner_set2_picker, loser_set2_picker)
		nonRequiredPickers = listOf(winner_set3_picker, loser_set3_picker)

		requiredPickers.forEach { it.apply { minValue = 0 }.apply { maxValue = 7 } }
		nonRequiredPickers.forEach { it.apply { minValue = 0 }.apply { maxValue = 100 } }

		button.setOnClickListener {

		}
	}
}
