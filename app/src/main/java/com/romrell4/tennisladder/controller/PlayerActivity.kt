package com.romrell4.tennisladder.controller

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.model.Match
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.support.Adapter
import com.romrell4.tennisladder.support.SuccessCallback
import com.romrell4.tennisladder.support.TLActivity
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.card_match.view.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.max

private const val VS_LIST_INDEX = 1

class PlayerActivity: TLActivity() {
	companion object {
		const val LADDER_EXTRA = "ladder"
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

		title = player.name
		ranking_text.text = getString(R.string.ranking_text_format, player.ranking)
		record_text.text = getString(R.string.record_text_format, player.wins, player.losses)

		recycler_view.apply {
			layoutManager = LinearLayoutManager(this@PlayerActivity)
			adapter = MatchAdapter()
		}

		//TODO: GONE button if they clicked on themselves
		report_match_button.setOnClickListener {
			startActivityForResult(
				Intent(this@PlayerActivity, ReportMatchActivity::class.java)
					.putExtra(ReportMatchActivity.ME_EXTRA, me)
					.putExtra(ReportMatchActivity.OPPONENT_EXTRA, player)
				, ReportMatchActivity.RC_MATCH_REPORTED
			)
		}

		loadMatches()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == ReportMatchActivity.RC_MATCH_REPORTED) {
			loadMatches()
		}
		super.onActivityResult(requestCode, resultCode, data)
	}

	private fun loadMatches() {
		Client.api.getMatches(player.ladderId, player.userId).enqueue(object: SuccessCallback<List<Match>>(this) {
			override fun onSuccess(data: List<Match>) {
				adapter.list = data
				view_switcher.displayedChild = VS_LIST_INDEX
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
