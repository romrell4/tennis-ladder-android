package com.romrell4.tennisladder.controller

import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.model.ServerError
import com.romrell4.tennisladder.support.Adapter
import com.romrell4.tennisladder.support.Callback
import com.romrell4.tennisladder.support.TLActivity
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_ladder.*
import kotlinx.android.synthetic.main.card_player.view.*
import kotlinx.android.synthetic.main.dialog_ladder_invite.view.*

private const val VS_SPINNER_INDEX = 0
private const val VS_LIST_INDEX = 1

class LadderActivity: TLActivity() {
	companion object {
		const val LADDER_EXTRA = "ladder"
	}

	private lateinit var ladder: Ladder
	private var me: Player? = null
		set(value) {
			field = value
			loadBottomButton()
		}
	private val adapter = PlayerAdapter()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_ladder)

		ladder = intent.getParcelableExtra(LADDER_EXTRA)
		title = ladder.name

		swipe_refresh_layout.setOnRefreshListener { loadPlayers() }

		recycler_view.layoutManager = LinearLayoutManager(this)
		recycler_view.adapter = adapter

		loadPlayers()
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.ladders_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
		R.id.rules -> {
			val webView = WebView(this)
			webView.loadUrl("https://romrell4.github.io/tennis-ladder-ws/rules.html")

			AlertDialog.Builder(this)
				.setView(webView)
				.setNeutralButton(android.R.string.ok, null)
				.show()
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == ReportMatchActivity.RC_MATCH_REPORTED) {
			loadPlayers()
		} else if (requestCode == RC_SIGN_IN) {
			if (resultCode == AppCompatActivity.RESULT_OK) {
				loadPlayers()
			} else {
				val response = IdpResponse.fromResultIntent(data)
				println("Failed login with error code: ${response?.error?.errorCode}")
			}
		}
		super.onActivityResult(requestCode, resultCode, data)
	}

	private fun loadBottomButton() = when {
		//The user is logged in AND is in the ladder already
		me != null -> {
			report_match_button.setup(R.string.report_match_button_text) {
				val players = adapter.list.filter { it != me }
				var selectedPlayer: Player? = null
				AlertDialog.Builder(this@LadderActivity)
					.setTitle(R.string.report_match_dialog_title)
					.setSingleChoiceItems(players.map { it.user.name }.toTypedArray(), -1) { _, index ->
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
		}
		//The user is logged in, but is not in the ladder yet
		FirebaseAuth.getInstance().currentUser != null -> {
			report_match_button.setup(R.string.request_ladder_invite_text) {
				val alertView = layoutInflater.inflate(R.layout.dialog_ladder_invite, null)
				val editText = alertView.edit_text
				AlertDialog.Builder(this)
					.setTitle(getString(R.string.ladder_invite_dialog_title))
					.setMessage(getString(R.string.ladder_invite_dialog_message))
					.setView(alertView)
					.setPositiveButton(android.R.string.ok) { _, _ ->
						view_switcher.displayedChild = VS_SPINNER_INDEX
						Client.api.addPlayerToLadder(ladder.ladderId, editText.text.toString()).enqueue(object: Callback<List<Player>>(this) {
							override fun onSuccess(data: List<Player>) {
								view_switcher.displayedChild = VS_LIST_INDEX
								me = data.firstOrNull { FirebaseAuth.getInstance().currentUser?.uid == it.user.userId }
								adapter.list = data
								Toast.makeText(this@LadderActivity, getString(R.string.ladder_invite_success_message), Toast.LENGTH_SHORT).show()
							}

							override fun onError(error: ServerError?, t: Throwable) {
								view_switcher.displayedChild = VS_LIST_INDEX
								AlertDialog.Builder(this@LadderActivity)
									.setTitle(getString(R.string.error))
									.setMessage(error?.error ?: t.message)
									.setNeutralButton(android.R.string.ok, null)
									.show()
							}
						})
					}
					.setNegativeButton(android.R.string.cancel, null)
					.show()
			}
		}
		//The user is not logged in
		else -> {
			report_match_button.setup(R.string.login_to_report_match_button_text) {
				startLoginActivity()
			}
		}
	}

	private fun Button.setup(@StringRes textId: Int, listener: ((View) -> Unit)) {
		visibility = View.VISIBLE
		text = getString(textId)
		setOnClickListener(listener)
	}

	private fun loadPlayers() {
		Client.api.getPlayers(ladder.ladderId).enqueue(object: Callback<List<Player>>(this) {
			override fun onSuccess(data: List<Player>) {
				view_switcher.displayedChild = VS_LIST_INDEX
				swipe_refresh_layout.isRefreshing = false
				me = data.firstOrNull { FirebaseAuth.getInstance().currentUser?.uid == it.user.userId }
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
				nameText.text = player.user.name
				scoreText.text = player.score.toString()

				Picasso.get().load(player.user.photoUrl).placeholder(R.drawable.ic_default_user).into(profileImage)

				card.setBackgroundColor(ContextCompat.getColor(this@LadderActivity, if (player == me) R.color.meCardColor else R.color.white))

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
