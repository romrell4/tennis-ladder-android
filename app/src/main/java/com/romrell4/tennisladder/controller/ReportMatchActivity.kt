package com.romrell4.tennisladder.controller

import android.os.Bundle
import android.widget.NumberPicker
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.support.TLActivity
import kotlinx.android.synthetic.main.activity_report_match.*

class ReportMatchActivity: TLActivity() {
	companion object {
		const val RC_MATCH_REPORTED = 1
		const val ME_EXTRA = "me-match-player"
		const val OPPONENT_EXTRA = "opp-match-player"
	}

	private lateinit var me: Player
	private lateinit var opponent: Player
	private lateinit var requiredPickers: List<NumberPicker>
	private lateinit var nonRequiredPickers: List<NumberPicker>
	private val pickers: List<NumberPicker>
		get() = requiredPickers + nonRequiredPickers

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_report_match)

		me = intent.getParcelableExtra(ME_EXTRA)
		opponent = intent.getParcelableExtra(OPPONENT_EXTRA)

		me_name_text.text = me.name
		opponent_name_text.text = opponent.name

		requiredPickers = listOf(winner_set1_picker, loser_set1_picker, winner_set2_picker, loser_set2_picker)
		nonRequiredPickers = listOf(winner_set3_picker, loser_set3_picker)

		requiredPickers.forEach { it.apply { minValue = 0 }.apply { maxValue = 7 } }
		nonRequiredPickers.forEach { it.apply { minValue = 0 }.apply { maxValue = 100 } }

		button.setOnClickListener {
			//TODO: Figure out how to finish with a result
		}
	}
}
