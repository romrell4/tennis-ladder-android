package com.romrell4.tennisladder.controller

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.support.TLActivity
import kotlinx.android.synthetic.main.activity_ladder.*
import kotlinx.android.synthetic.main.card_player.view.*
import java.util.*
import kotlin.concurrent.schedule

private const val VS_LIST_INDEX = 1

class LadderActivity: TLActivity() {
	companion object {
		const val LADDER_EXTRA = "ladder"
	}

	private lateinit var ladder: Ladder
	private val adapter = PlayersAdapter()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_ladder)

		ladder = intent.getParcelableExtra(LADDER_EXTRA)
		title = ladder.name

		recycler_view.layoutManager = LinearLayoutManager(this)
		recycler_view.adapter = adapter

		loadPlayers()
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.ladder_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
		R.id.report_option -> {
			//TODO: Filter out yourself
			val players = adapter.players
			var selectedPlayer: Player? = null
			AlertDialog.Builder(this)
				.setSingleChoiceItems(players.map { it.name }.toTypedArray(), -1) { _, index ->
					selectedPlayer = players[index]
				}.setPositiveButton("Select") { _, _ ->
					selectedPlayer?.let {
						startActivityForResult(Intent(this@LadderActivity, ReportMatchActivity::class.java).putExtra(ReportMatchActivity.PLAYER_EXTRA, it), ReportMatchActivity.RC_MATCH_REPORTED)
					}
				}.setNegativeButton("Cancel", null).show()
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == ReportMatchActivity.RC_MATCH_REPORTED) {
			loadPlayers()
		}
		super.onActivityResult(requestCode, resultCode, data)
	}

	private fun loadPlayers() {
		Timer("").schedule(1000) {
			runOnUiThread {
				view_switcher.displayedChild = VS_LIST_INDEX
				adapter.players = listOf(
					Player(3, 1, "Albus Dumbledore", 100, 1, 4, 0),
					Player(1, 1, "Rebecca Bean", 30, 2, 3, 0),
					Player(2, 1, "Eric Romrell", 10, 3, 2, 2),
					Player(4, 1, "Jessica Romrell", 10, 3, 1, 1)
				)
			}
		}
	}

	private inner class PlayersAdapter(var players: List<Player> = emptyList()): RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder>() {
		override fun getItemCount() = players.size
		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PlayerViewHolder(layoutInflater.inflate(R.layout.card_player, parent, false))
		override fun onBindViewHolder(viewHolder: PlayerViewHolder, position: Int) {
			viewHolder.bind(players[position])
		}

		private inner class PlayerViewHolder(view: View): RecyclerView.ViewHolder(view) {
			private val nameText = view.name_text
			private val scoreText = view.score_text

			fun bind(player: Player) {
				nameText.text = player.name
				scoreText.text = player.score.toString()
				itemView.setOnClickListener {
					startActivity(Intent(this@LadderActivity, PlayerActivity::class.java).putExtra(PlayerActivity.PLAYER_EXTRA, player))
				}
			}
		}
	}
}
