package com.romrell4.tennisladder.controller

import android.app.AlertDialog
import android.os.Bundle
import android.widget.NumberPicker
import android.widget.Toast
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Match
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.support.SuccessCallback
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
	private lateinit var setOnlyPickers: List<NumberPicker>
	private lateinit var setOrTiebreakPickers: List<NumberPicker>

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_report_match)

		me = intent.getParcelableExtra(ME_EXTRA)
		opponent = intent.getParcelableExtra(OPPONENT_EXTRA)

		me_name_text.text = me.name
		opponent_name_text.text = opponent.name

		setOnlyPickers = listOf(me_set1_picker, opponent_set1_picker, me_set2_picker, opponent_set2_picker)
		setOrTiebreakPickers = listOf(me_set3_picker, opponent_set3_picker)

		setOnlyPickers.forEach { it.apply { minValue = 0 }.apply { maxValue = 7 } }
		setOrTiebreakPickers.forEach { it.apply { minValue = 0 }.apply { maxValue = 100 } }

		button.setOnClickListener {
			val match = getMatch()
			AlertDialog.Builder(this)
				.setTitle(R.string.score_confirmation_title)
				.setMessage(getString(R.string.score_confirmation_message, if (match.winner == me) "won" else "lost", match.scoreText))
				.setPositiveButton(R.string.yes) { _, _ ->
					Client.api.reportMatch(me.ladderId, match).enqueue(object: SuccessCallback<Match>(this) {
						override fun onSuccess(data: Match) {
							Toast.makeText(this@ReportMatchActivity, R.string.report_success_message, Toast.LENGTH_SHORT).show()
							setResult(RC_MATCH_REPORTED)
							finish()
						}
					})
				}
				.setNegativeButton(R.string.no, null)
				.show()
		}
	}

	private fun getMatch(): Match {
		val playedThirdSet = me_set3_picker.value != 0 || opponent_set3_picker.value != 0
		val lastSetScore = if (playedThirdSet) me_set3_picker.value to opponent_set3_picker.value else me_set2_picker.value to opponent_set2_picker.value
		val iWon = lastSetScore.first > lastSetScore.second

		return Match(
			ladderId = me.ladderId,
			winner = if (iWon) me else opponent,
			loser = if (iWon) opponent else me,
			winnerSet1Score = if (iWon) me_set1_picker.value else opponent_set1_picker.value,
			loserSet1Score = if (iWon) opponent_set1_picker.value else me_set1_picker.value,
			winnerSet2Score = if (iWon) me_set2_picker.value else opponent_set2_picker.value,
			loserSet2Score = if (iWon) opponent_set2_picker.value else me_set2_picker.value
		).apply {
			if (playedThirdSet) {
				winnerSet3Score = if (iWon) me_set3_picker.value else opponent_set3_picker.value
				loserSet3Score = if (iWon) opponent_set3_picker.value else me_set3_picker.value
			}
		}
	}
}
