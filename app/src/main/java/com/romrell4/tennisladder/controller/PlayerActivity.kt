package com.romrell4.tennisladder.controller

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.databinding.ActivityPlayerBinding
import com.romrell4.tennisladder.databinding.CardMatchBinding
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Match
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.model.ServerError
import com.romrell4.tennisladder.support.*
import com.squareup.picasso.Picasso
import okhttp3.ResponseBody
import java.text.SimpleDateFormat
import java.util.*

private const val VS_LOADING_INDEX = 0
private const val VS_LIST_INDEX = 1
private val MATCH_DATE_FORMAT = SimpleDateFormat("M/d/yyyy", Locale.US)

class PlayerActivity : TLActivity() {
    companion object {
        const val ME_EXTRA = "me"
        const val PLAYER_EXTRA = "player"
        const val IS_ADMIN = "isAdmin"
    }

    private lateinit var binding: ActivityPlayerBinding

    private var me: Player? = null
    private lateinit var player: Player
    private var isAdmin: Boolean = false
    private val adapter = MatchAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityPlayerBinding.inflate(layoutInflater).also { binding = it }.root)

        me = intent.getExtra(ME_EXTRA)
        player = intent.requireExtra(PLAYER_EXTRA)
        isAdmin = intent.requireExtra(IS_ADMIN)

        Picasso.get().load(player.user.photoUrl).placeholder(R.drawable.ic_default_user).into(binding.imageView)

        title = player.user.name
        binding.rankingText.text = getString(R.string.ranking_text_format, player.ranking)
        binding.recordText.text = getString(R.string.record_text_format, player.wins, player.losses)

        binding.challengeButton.visibility = if (me != null && player != me) View.VISIBLE else View.GONE
        binding.challengeButton.setOnClickListener { _ ->
            data class ContactOption(val title: String, val value: String?, val intent: Intent)

            val contactOptions = listOf(
                ContactOption(
                    title = "Email",
                    value = player.user.email,
                    intent = Intent(Intent.ACTION_SENDTO).setData(
                        Uri.parse(
                            "mailto:${player.user.email}?subject=${
                                Uri.encode("Tennis Ladder Challenge")
                            }"
                        )
                    )
                ),
                ContactOption(
                    title = "Phone",
                    value = player.user.phoneNumber,
                    intent = Intent(Intent.ACTION_SENDTO).setData(Uri.parse("smsto:${player.user.phoneNumber}"))
                )
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

        binding.recyclerView.layoutManager = LinearLayoutManager(this@PlayerActivity)
        binding.recyclerView.adapter = adapter

        loadMatches()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (me != null) {
            menuInflater.inflate(R.menu.player_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.add_contact -> {
            startActivity(
                Intent(ContactsContract.Intents.Insert.ACTION)
                    .apply { type = ContactsContract.RawContacts.CONTENT_TYPE }
                    .putExtra(ContactsContract.Intents.Insert.NAME, player.user.name)
                    .putExtra(ContactsContract.Intents.Insert.EMAIL, player.user.email)
                    .putExtra(ContactsContract.Intents.Insert.PHONE, player.user.phoneNumber)
                    .putExtra(
                        ContactsContract.Intents.Insert.PHONE_TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
            )
            true
        }
        R.id.view_profile -> {
            startActivity(
                Intent(this, ProfileActivity::class.java)
                    .putExtra(ProfileActivity.MY_ID_EXTRA, me?.user?.userId)
                    .putExtra(ProfileActivity.USER_ID_EXTRA, player.user.userId)
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
        Client.api.getMatches(player.ladderId, player.user.userId).enqueue(object : Callback<List<Match>>(this) {
            override fun onSuccess(data: List<Match>) {
                binding.viewSwitcher.displayedChild = VS_LIST_INDEX
                adapter.list = data
            }
        })
    }

    private inner class MatchAdapter : Adapter<Match>(this, R.string.no_matches_text) {
        override fun createViewHolder(parent: ViewGroup) =
            MatchViewHolder(CardMatchBinding.inflate(layoutInflater, parent, false))

        override fun bind(viewHolder: RecyclerView.ViewHolder, item: Match) {
            (viewHolder as? MatchViewHolder)?.bind(item)
        }

        private inner class MatchViewHolder(private val cardBinding: CardMatchBinding) : RecyclerView.ViewHolder(cardBinding.root) {
            fun bind(match: Match) {
                cardBinding.nameText.text = listOf(match.winner, match.loser).first { it != player }.user.name
                cardBinding.dateText.text = MATCH_DATE_FORMAT.format(match.matchDate)
                cardBinding.scoreText.text = match.scoreText(player)
                cardBinding.scoreText.setTextColor(
                    ContextCompat.getColor(
                        this@PlayerActivity,
                        if (match.winner == player) R.color.matchWinner else R.color.matchLoser
                    )
                )
                if (isAdmin) {
                    itemView.setOnClickListener {
                        AlertDialog.Builder(this@PlayerActivity)
                            .setTitle(R.string.match_options_title)
                            .setItems(arrayOf("Edit Scores", "Delete")) { _, which ->
                                when (which) {
                                    0 -> editMatchScores(match)
                                    1 -> deleteMatch(match)
                                }
                            }
                            .show()
                    }
                }
            }

            fun editMatchScores(match: Match) {
                val dialogView = EnterMatchScoresView(context = this@PlayerActivity).also {
                    it.setMatch(match)
                }
                AlertDialog.Builder(this@PlayerActivity)
                    .setTitle(R.string.edit_match_dialog_title)
                    .setView(dialogView)
                    .setPositiveButton(R.string.save) { _, _ ->
                        val updatedMatch = dialogView.getMatchScores().apply(match)
                        binding.viewSwitcher.displayedChild = VS_LOADING_INDEX
                        Client.api.updateMatchScores(
                            ladderId = updatedMatch.ladderId,
                            matchId = updatedMatch.matchId,
                            match = updatedMatch
                        ).enqueue(object : Callback<Match>(this@PlayerActivity) {
                            override fun onSuccess(data: Match) {
                                binding.viewSwitcher.displayedChild = VS_LIST_INDEX
                                adapter.list = adapter.list.mapIndexed { index, match ->
                                    if (index == absoluteAdapterPosition) data else match
                                }
                            }

                            override fun onError(error: ServerError?, t: Throwable) {
                                binding.viewSwitcher.displayedChild = VS_LIST_INDEX
                                super.onError(error, t)
                            }
                        })
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }

            fun deleteMatch(match: Match) {
                binding.viewSwitcher.displayedChild = VS_LOADING_INDEX
                Client.api.deleteMatch(match.ladderId, match.matchId).enqueue(object : Callback<ResponseBody>(this@PlayerActivity) {
                    override fun onSuccess(data: ResponseBody) {
                        binding.viewSwitcher.displayedChild = VS_LIST_INDEX
                        adapter.list = adapter.list.filterIndexed { index, _ -> index != absoluteAdapterPosition }
                    }

                    override fun onError(error: ServerError?, t: Throwable) {
                        binding.viewSwitcher.displayedChild = VS_LIST_INDEX
                        super.onError(error, t)
                    }
                })
            }
        }
    }
}
