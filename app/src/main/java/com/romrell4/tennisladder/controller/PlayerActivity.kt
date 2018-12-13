package com.romrell4.tennisladder.controller

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Match
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.support.TLActivity
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.card_match.view.*
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.max

private const val VS_LIST_INDEX = 1

class PlayerActivity: TLActivity() {
	companion object {
		const val PLAYER_EXTRA = "player"
	}

	private lateinit var player: Player

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_player)

		player = intent.getParcelableExtra(PLAYER_EXTRA)
		title = player.name
		ranking_text.text = getString(R.string.ranking_text_format, player.ranking)
		record_text.text = getString(R.string.record_text_format, player.wins, player.losses)

		recycler_view.apply {
			layoutManager = LinearLayoutManager(this@PlayerActivity)
			adapter = MatchAdapter(
				listOf(
					Match(0, 0, Date(), Player(0, 0, "Tester", 0, 0, 0, 0), player, 6, 4, 6, 2),
					Match(0, 0, Date(), player, Player(0, 0, "Tester", 0, 0, 0, 0), 6, 4, 6, 2)
				)
			)
		}

		report_match_button.setOnClickListener {
			startActivityForResult(Intent(this@PlayerActivity, ReportMatchActivity::class.java).putExtra(ReportMatchActivity.PLAYER_EXTRA, player), ReportMatchActivity.RC_MATCH_REPORTED)
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
		Timer("Test", false).schedule(1000) {
			runOnUiThread {
				view_switcher.displayedChild = VS_LIST_INDEX
			}
		}
	}

	private inner class MatchAdapter(private val matches: List<Match>): RecyclerView.Adapter<MatchAdapter.BaseMatchViewHolder>() {
		override fun getItemCount() = max(matches.size, 1)
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
			if (matches.isEmpty()) EmptyViewHolder(layoutInflater.inflate(R.layout.card_no_matches, parent, false))
			else MatchViewHolder(layoutInflater.inflate(R.layout.card_match, parent, false))

		override fun onBindViewHolder(viewHolder: BaseMatchViewHolder, position: Int) {
			(viewHolder as? MatchViewHolder)?.bind(matches[position])
		}

		private abstract inner class BaseMatchViewHolder(view: View): RecyclerView.ViewHolder(view)
		private inner class EmptyViewHolder(view: View): BaseMatchViewHolder(view)
		private inner class MatchViewHolder(view: View): BaseMatchViewHolder(view) {
			private val nameText = view.name_text
			private val scoreText = view.score_text

			fun bind(match: Match) {
				nameText.text = listOf(match.winner, match.loser).first { it.userId != player.userId }.name
				scoreText.text = match.scoreText
			}
		}
	}
}
