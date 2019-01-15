package com.romrell4.tennisladder.controller

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Match
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.support.Adapter
import com.romrell4.tennisladder.support.Callback
import com.romrell4.tennisladder.support.TLActivity
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_player.*
import kotlinx.android.synthetic.main.card_match.view.*
import java.text.SimpleDateFormat
import java.util.*

private const val VS_LIST_INDEX = 1
private val MATCH_DATE_FORMAT = SimpleDateFormat("M/d/YYYY", Locale.US)

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

		Picasso.get().load(player.user.photoUrl).placeholder(R.drawable.ic_default_user).into(image_view)

		title = player.user.name
		ranking_text.text = getString(R.string.ranking_text_format, player.ranking)
		record_text.text = getString(R.string.record_text_format, player.wins, player.losses)

		challenge_button.visibility = if (player != me) View.VISIBLE else View.GONE
		challenge_button.setOnClickListener { _ ->
			data class ContactOption(val title: String, val value: String?, val intent: Intent)

			val contactOptions = listOf(
				ContactOption("Email", player.user.email, Intent(Intent.ACTION_SENDTO).setData(Uri.parse("mailto:${player.user.email}?subject=${Uri.encode("Tennis Ladder Challenge")}"))),
				ContactOption("Phone", player.user.phoneNumber, Intent(Intent.ACTION_SENDTO).setData(Uri.parse("smsto:${player.user.phoneNumber}")))
			).filter { it.value != null }

			if (contactOptions.isNotEmpty()) {
				AlertDialog.Builder(this)
					.setTitle(getString(R.string.challenge_dialog_title))
					.setItems(contactOptions.map { it.title }.toTypedArray()) { _, index ->
						startActivity(contactOptions[index].intent)
					}
					.show()
			} else {
				AlertDialog.Builder(this)
					.setMessage("This player has not set up any contact options. Please notify the leader of the ladder so that they can resolve this.")
					.show()
			}
		}

		recycler_view.layoutManager = LinearLayoutManager(this@PlayerActivity)
		recycler_view.adapter = adapter

		loadMatches()
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.player_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
		R.id.add -> {
			startActivity(
				Intent(ContactsContract.Intents.Insert.ACTION)
					.apply { type = ContactsContract.RawContacts.CONTENT_TYPE }
					.putExtra(ContactsContract.Intents.Insert.NAME, player.user.name)
					.putExtra(ContactsContract.Intents.Insert.EMAIL, player.user.email)
					.putExtra(ContactsContract.Intents.Insert.PHONE, player.user.phoneNumber)
					.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
			)
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		when (requestCode) {
			ReportMatchActivity.RC_MATCH_REPORTED -> loadMatches()
			else -> super.onActivityResult(requestCode, resultCode, data)
		}
	}

	private fun loadMatches() {
		Client.api.getMatches(player.ladderId, player.user.userId).enqueue(object: Callback<List<Match>>(this) {
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
			private val dateText = view.date_text
			private val scoreText = view.score_text

			fun bind(match: Match) {
				nameText.text = listOf(match.winner, match.loser).first { it != player }.user.name
				dateText.text = MATCH_DATE_FORMAT.format(match.matchDate)
				scoreText.text = match.scoreText(player)
			}
		}
	}
}
