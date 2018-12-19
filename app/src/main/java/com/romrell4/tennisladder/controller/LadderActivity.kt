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
import com.squareup.picasso.Picasso
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

		swipe_refresh_layout.setOnRefreshListener { loadPlayers() }

		recycler_view.layoutManager = LinearLayoutManager(this)
		recycler_view.adapter = adapter

		report_match_button.setOnClickListener {
			val players = adapter.list.filter { it != me }
			var selectedPlayer: Player? = null
			AlertDialog.Builder(this)
				.setTitle(R.string.report_match_dialog_title)
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
		}

		loadPlayers()
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
				FirebaseAuth.getInstance().currentUser?.let { user ->
					data.firstOrNull { user.uid == it.userId }?.let {
						me = it
					} ?: run {
						//TODO: Allow the user to request to be added to the tournament
					}
				} ?: run {
					//TODO: If the user is not logged in, do something?
				}
				view_switcher.displayedChild = VS_LIST_INDEX
				swipe_refresh_layout.isRefreshing = false
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
			private val profileImage = view.profile_image
			private val nameText = view.name_text
			private val scoreText = view.score_text

			fun bind(player: Player) {
				nameText.text = player.name
				scoreText.text = player.score.toString()

				Picasso.get().load(player.photoUrl).into(profileImage)

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
