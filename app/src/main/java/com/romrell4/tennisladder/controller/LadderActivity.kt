package com.romrell4.tennisladder.controller

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.support.TLActivity
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.model.Player
import kotlinx.android.synthetic.main.activity_ladder.*
import kotlinx.android.synthetic.main.card_player.view.*

class LadderActivity: TLActivity() {
	companion object {
		const val LADDER_EXTRA = "ladder"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_ladder)

		title = intent.getParcelableExtra<Ladder>(LADDER_EXTRA).name

		recycler_view.layoutManager = LinearLayoutManager(this)
		recycler_view.adapter = PlayersAdapter(
			listOf(
				Player(3, 1, "Albus Dumbledore", 100, 1, 4, 0),
				Player(1, 1, "Rebecca Bean", 30, 2, 3, 0),
				Player(2, 1, "Eric Romrell", 10, 3, 2, 2),
				Player(4, 1, "Jessica Romrell", 10, 3, 1, 1)
			)
		)
	}

	private inner class PlayersAdapter(val players: List<Player>): RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder>() {
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
