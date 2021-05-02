package com.romrell4.tennisladder.controller

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.databinding.ActivityReportMatchBinding
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Match
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.support.Callback
import com.romrell4.tennisladder.support.TLActivity
import com.romrell4.tennisladder.support.requireExtra
import com.squareup.picasso.Picasso

class ReportMatchActivity : TLActivity() {
    companion object {
        const val RC_MATCH_REPORTED = 1
        const val ME_EXTRA = "me-match-player"
        const val OPPONENT_EXTRA = "opp-match-player"
    }

    private lateinit var binding: ActivityReportMatchBinding

    private lateinit var me: Player
    private lateinit var opponent: Player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityReportMatchBinding.inflate(layoutInflater).also { binding = it }.root)

        me = intent.requireExtra(ME_EXTRA)
        opponent = intent.requireExtra(OPPONENT_EXTRA)

        binding.meNameText.text = me.user.name
        binding.opponentNameText.text = opponent.user.name

        Picasso.get().load(me.user.photoUrl).placeholder(R.drawable.ic_default_user).into(binding.meImage)
        Picasso.get().load(opponent.user.photoUrl).placeholder(R.drawable.ic_default_user).into(binding.opponentImage)

        binding.button.setOnClickListener {
            try {
                val match = getMatch()
                AlertDialog.Builder(this)
                    .setTitle(R.string.score_confirmation_title)
                    .setMessage(getString(R.string.score_confirmation_message, match.scoreText(me)))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        Client.api.reportMatch(me.ladderId, match)
                            .enqueue(object : Callback<Match>(this) {
                                override fun onSuccess(data: Match) {
                                    Toast.makeText(
                                        this@ReportMatchActivity,
                                        R.string.report_success_message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    setResult(RC_MATCH_REPORTED)
                                    finish()
                                }
                            })
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
            } catch (e: IllegalStateException) {
                Toast.makeText(
                    this,
                    "Only winners report matches. Please let ${opponent.user.name} know to report the set scores.",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: NumberFormatException) {
                Toast.makeText(
                    this,
                    "Invalid inputs. Please enter a valid set score.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun getMatch(): Match {
        return binding.scoresView.getMatchScores().apply(
            Match(
                ladderId = me.ladderId,
                winner = me,
                loser = opponent,
                // The scores will be replaced by the match scores
                winnerSet1Score = 0,
                loserSet1Score = 0,
                winnerSet2Score = 0,
                loserSet2Score = 0
            )
        )
    }
}
