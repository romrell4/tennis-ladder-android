package com.romrell4.tennisladder.controller

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.auth.AuthUI
import com.romrell4.tennisladder.BuildConfig
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.model.Match
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.support.Adapter
import com.romrell4.tennisladder.support.SuccessCallback
import com.romrell4.tennisladder.support.TLActivity
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.card_match.view.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.max

private const val RC_SIGN_IN = 100
private const val VS_LIST_INDEX = 1

class PlayerActivity: TLActivity() {
	companion object {
		const val ME_EXTRA = "me"
		const val PLAYER_EXTRA = "player"
	}

	private var me: Player? = null
	private lateinit var player: Player
	private val adapter = MatchAdapter()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_player)

		me = intent.getParcelableExtra(ME_EXTRA)
		player = intent.getParcelableExtra(PLAYER_EXTRA)

		Picasso.get().load(player.photoUrl).into(image_view)

		title = player.name
		ranking_text.text = getString(R.string.ranking_text_format, player.ranking)
		record_text.text = getString(R.string.record_text_format, player.wins, player.losses)

		recycler_view.layoutManager = LinearLayoutManager(this@PlayerActivity)
		recycler_view.adapter = adapter

		loadReportButton()
		loadMatches()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		when (requestCode) {
			ReportMatchActivity.RC_MATCH_REPORTED -> loadMatches()
			RC_SIGN_IN -> loadReportButton()
			else -> super.onActivityResult(requestCode, resultCode, data)
		}
	}

	private fun loadReportButton() {
		report_match_button.apply {
			if (me == null) {
				//If the user isn't logged in, make the button log them in
				text = getString(R.string.login_to_report_match_button_text)
				setOnClickListener {
					startActivityForResult(
						AuthUI.getInstance().createSignInIntentBuilder()
							.setLogo(R.drawable.ic_tennis_ladder)
							.setIsSmartLockEnabled(!BuildConfig.DEBUG)
							.setAvailableProviders(listOf(
								AuthUI.IdpConfig.GoogleBuilder(),
								AuthUI.IdpConfig.EmailBuilder()
							).map { it.build() }).build(), RC_SIGN_IN
					)
				}
			} else if (me == player) {
				//If they clicked on themself
				visibility = View.GONE
			} else {
				setOnClickListener {
					startActivityForResult(
						Intent(this@PlayerActivity, ReportMatchActivity::class.java)
							.putExtra(ReportMatchActivity.ME_EXTRA, me)
							.putExtra(ReportMatchActivity.OPPONENT_EXTRA, player)
						, ReportMatchActivity.RC_MATCH_REPORTED
					)
				}
			}
		}
	}

	private fun loadMatches() {
		Client.api.getMatches(player.ladderId, player.userId).enqueue(object: SuccessCallback<List<Match>>(this) {
			override fun onSuccess(data: List<Match>) {
				view_switcher.displayedChild = VS_LIST_INDEX
				adapter.list = data
			}
		})
	}

	private inner class MatchAdapter: Adapter<Match>(this, R.string.no_matches_text) {
		override fun createViewHolder(parent: ViewGroup) = MatchViewHolder(layoutInflater.inflate(R.layout.card_match, parent, false))
		override fun bind(viewHolder: RecyclerView.ViewHolder, item: Match) {
			(viewHolder as? MatchViewHolder)?.bind(item)
		}

		private inner class MatchViewHolder(view: View): RecyclerView.ViewHolder(view) {
			private val nameText = view.name_text
			private val scoreText = view.score_text

			fun bind(match: Match) {
				nameText.text = listOf(match.winner, match.loser).first { it.userId != player.userId }.name
				scoreText.text = match.scoreText
			}
		}
	}
}
