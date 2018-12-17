package com.romrell4.tennisladder.controller

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.support.Adapter
import com.romrell4.tennisladder.support.SuccessCallback
import com.romrell4.tennisladder.support.TLActivity
import kotlinx.android.synthetic.main.activity_ladder.*
import kotlinx.android.synthetic.main.card_player.view.*

private const val VS_LIST_INDEX = 1

class LadderActivity: TLActivity() {
	companion object {
		const val LADDER_EXTRA = "ladder"
	}

	private lateinit var ladder: Ladder
	private var me: Player? = null
	private val adapter = PlayerAdapter()

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
			val players = adapter.list.filter { it.userId != FirebaseAuth.getInstance().currentUser?.uid }
			var selectedPlayer: Player? = null
			AlertDialog.Builder(this)
				.setSingleChoiceItems(players.map { it.name }.toTypedArray(), -1) { _, index ->
					selectedPlayer = players[index]
				}.setPositiveButton("Select") { _, _ ->
					selectedPlayer?.let {
						startActivityForResult(
							Intent(this@LadderActivity, ReportMatchActivity::class.java)
								.putExtra(ReportMatchActivity.ME_EXTRA, me)
								.putExtra(ReportMatchActivity.OPPONENT_EXTRA, it),
							ReportMatchActivity.RC_MATCH_REPORTED
						)
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
		Client.api.getPlayers(ladder.ladderId).enqueue(object: SuccessCallback<List<Player>>(this) {
			override fun onSuccess(data: List<Player>) {
				me = data.firstOrNull { FirebaseAuth.getInstance().currentUser?.uid == it.userId }
				view_switcher.displayedChild = VS_LIST_INDEX
				adapter.list = data
			}
		})
	}

	private inner class PlayerAdapter: Adapter<Player>(this, R.string.no_players_text) {
		override fun createViewHolder(parent: ViewGroup) = PlayerViewHolder(layoutInflater.inflate(R.layout.card_player, parent, false))
		override fun bind(viewHolder: RecyclerView.ViewHolder, item: Player) {
			(viewHolder as? PlayerViewHolder)?.bind(item)
		}

		private inner class PlayerViewHolder(view: View): RecyclerView.ViewHolder(view) {
			private val card = view.card
			private val nameText = view.name_text
			private val scoreText = view.score_text

			fun bind(player: Player) {
				nameText.text = player.name
				scoreText.text = player.score.toString()

				if (player == me) {
					card.setBackgroundColor(ContextCompat.getColor(this@LadderActivity, R.color.me_card_color))
				}

				itemView.setOnClickListener {
					startActivity(
						Intent(this@LadderActivity, PlayerActivity::class.java)
							.putExtra(PlayerActivity.ME_EXTRA, me)
							.putExtra(PlayerActivity.PLAYER_EXTRA, player)
					)
				}
			}
		}
	}
}
