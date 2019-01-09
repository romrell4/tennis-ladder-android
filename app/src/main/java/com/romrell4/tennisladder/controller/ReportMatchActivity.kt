package com.romrell4.tennisladder.controller

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Match
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.support.Callback
import com.romrell4.tennisladder.support.TLActivity
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_report_match.*
import java.lang.NumberFormatException

class ReportMatchActivity: TLActivity() {
	companion object {
		const val RC_MATCH_REPORTED = 1
		const val ME_EXTRA = "me-match-player"
		const val OPPONENT_EXTRA = "opp-match-player"
	}

	private lateinit var me: Player
	private lateinit var opponent: Player

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_report_match)

		setupScoreFields()

		me = intent.getParcelableExtra(ME_EXTRA)
		opponent = intent.getParcelableExtra(OPPONENT_EXTRA)

		me_name_text.text = me.user.name
		opponent_name_text.text = opponent.user.name

		Picasso.get().load(me.user.photoUrl).placeholder(R.drawable.ic_default_user).into(me_image)
		Picasso.get().load(opponent.user.photoUrl).placeholder(R.drawable.ic_default_user).into(opponent_image)

		button.setOnClickListener {
			try {
				val match = getMatch()
				AlertDialog.Builder(this)
					.setTitle(R.string.score_confirmation_title)
					.setMessage(getString(R.string.score_confirmation_message, if (match.winner == me) "won" else "lost", match.scoreText))
					.setPositiveButton(R.string.yes) { _, _ ->
						Client.api.reportMatch(me.ladderId, match).enqueue(object: Callback<Match>(this) {
							override fun onSuccess(data: Match) {
								Toast.makeText(this@ReportMatchActivity, R.string.report_success_message, Toast.LENGTH_SHORT).show()
								setResult(RC_MATCH_REPORTED)
								finish()
							}
						})
					}
					.setNegativeButton(R.string.no, null)
					.show()
			} catch (e: NumberFormatException) {
				Toast.makeText(this, "Invalid inputs. Please enter a valid set score.", Toast.LENGTH_LONG).show()
			}
		}
	}

	private fun setupScoreFields() {
		val tiebreakFields = listOf(me_set3_score, opponent_set3_score)
		val scoreFields = listOf(me_set1_score, opponent_set1_score, me_set2_score, opponent_set2_score) + tiebreakFields

		//Make the keyboard pop up immediately with focus on the first field
		scoreFields.first().requestFocus()

		for ((index, editText) in scoreFields.withIndex()) {
			val nextEditText = scoreFields.getOrNull(index + 1)
			val allowedDigits = if (editText in tiebreakFields) 2 else 1
			editText.addTextChangedListener(DigitTextWatcher(editText, nextEditText, allowedDigits))
			nextEditText?.let {
				editText.imeOptions = EditorInfo.IME_ACTION_NEXT
				editText.nextFocusForwardId = nextEditText.id
			} ?: run {
				editText.imeOptions = EditorInfo.IME_ACTION_DONE
			}
		}
	}

	private fun getMatch(): Match {
		val playedThirdSet = me_set3_score.getIntOrNull() != null && opponent_set3_score.getIntOrNull() != null
		val lastSetScore = if (playedThirdSet) me_set3_score.getInt() to opponent_set3_score.getInt() else me_set2_score.getInt() to opponent_set2_score.getInt()
		val iWon = lastSetScore.first > lastSetScore.second

		return Match(
			ladderId = me.ladderId,
			winner = if (iWon) me else opponent,
			loser = if (iWon) opponent else me,
			winnerSet1Score = (if (iWon) me_set1_score else opponent_set1_score).getInt(),
			loserSet1Score = (if (iWon) opponent_set1_score else me_set1_score).getInt(),
			winnerSet2Score = (if (iWon) me_set2_score else opponent_set2_score).getInt(),
			loserSet2Score = (if (iWon) opponent_set2_score else me_set2_score).getInt()
		).apply {
			if (playedThirdSet) {
				winnerSet3Score = (if (iWon) me_set3_score else opponent_set3_score).getInt()
				loserSet3Score = (if (iWon) opponent_set3_score else me_set3_score).getInt()
			}
		}
	}

	private fun EditText.getInt() = text.toString().toInt()
	private fun EditText.getIntOrNull() = text.toString().toIntOrNull()

	private inner class DigitTextWatcher(private val editText: EditText, private val nextEditText: EditText?, private val digits: Int): TextWatcher {
		override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
			if (editText.text.length == digits) {
				//Either move to the next focus or hide the keyboard
				nextEditText?.requestFocus() ?: run {
					this@ReportMatchActivity.hideKeyboard()
				}
			}
		}

		override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
		override fun afterTextChanged(s: Editable?) {}
	}

	private fun Activity.hideKeyboard() {
		val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
		inputMethodManager.hideSoftInputFromWindow((if (currentFocus == null) View(this) else currentFocus).windowToken, 0)
	}
}
