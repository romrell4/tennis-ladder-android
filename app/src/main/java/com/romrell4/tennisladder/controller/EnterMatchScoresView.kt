package com.romrell4.tennisladder.controller

import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import com.romrell4.tennisladder.databinding.EnterMatchScoresViewBinding
import com.romrell4.tennisladder.model.Match

class EnterMatchScoresView(
    context: Context,
    attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {
    private val binding = EnterMatchScoresViewBinding.inflate(LayoutInflater.from(context), this)

    fun setMatch(match: Match) {
        with(binding) {
            winnerSet1Score.setText(match.winnerSet1Score.toString())
            winnerSet2Score.setText(match.winnerSet2Score.toString())
            winnerSet3Score.setText(match.winnerSet3Score?.toString())
            loserSet1Score.setText(match.loserSet1Score.toString())
            loserSet2Score.setText(match.loserSet2Score.toString())
            loserSet3Score.setText(match.loserSet3Score?.toString())
        }
    }

    fun getMatchScores(): MatchScores {
        fun EditText.getInt() = text.toString().toInt()
        fun EditText.getIntOrNull() = text.toString().toIntOrNull()

        return with(binding) {
            MatchScores(
                winnerSet1 = winnerSet1Score.getInt(),
                winnerSet2 = winnerSet2Score.getInt(),
                winnerSet3 = winnerSet3Score.getIntOrNull(),
                loserSet1 = loserSet1Score.getInt(),
                loserSet2 = loserSet2Score.getInt(),
                loserSet3 = loserSet3Score.getIntOrNull()
            ).validate()
        }
    }

    init {
        setupScoreFields()
    }

    private fun setupScoreFields() {
        val tiebreakFields = listOf(binding.winnerSet3Score, binding.loserSet3Score)
        val scoreFields = listOf(
            binding.winnerSet1Score,
            binding.loserSet1Score,
            binding.winnerSet2Score,
            binding.loserSet2Score
        ) + tiebreakFields

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

    private inner class DigitTextWatcher(
        private val editText: EditText,
        private val nextEditText: EditText?,
        private val digits: Int
    ) :
        TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (editText.text.length == digits) {
                //Either move to the next focus or hide the keyboard
                nextEditText?.requestFocus() ?: run {
                    hideKeyboard()
                }
            }
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    data class MatchScores(
        val winnerSet1: Int,
        val winnerSet2: Int,
        val winnerSet3: Int?,
        val loserSet1: Int,
        val loserSet2: Int,
        val loserSet3: Int?
    ) {
        fun apply(match: Match): Match {
            return match.copy(
                winnerSet1Score = winnerSet1,
                winnerSet2Score = winnerSet2,
                winnerSet3Score = winnerSet3,
                loserSet1Score = loserSet1,
                loserSet2Score = loserSet2,
                loserSet3Score = loserSet3
            )
        }

        /**
         * Don't try to validate all the rules around scores. Just make sure the loser isn't trying to report
         */
        fun validate(): MatchScores {
            return apply {
                // If they played a third set, check that. Otherwise, check the second set
                val lastSetScore =
                    if (winnerSet3 != null && loserSet3 != null) winnerSet3 to loserSet3 else winnerSet2 to loserSet2
                if (lastSetScore.first <= lastSetScore.second) {
                    throw IllegalStateException()
                }
            }
        }
    }
}
