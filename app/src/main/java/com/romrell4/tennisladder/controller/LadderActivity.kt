package com.romrell4.tennisladder.controller

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.databinding.ActivityLadderBinding
import com.romrell4.tennisladder.databinding.CardPlayerBinding
import com.romrell4.tennisladder.databinding.DialogLadderInviteBinding
import com.romrell4.tennisladder.databinding.DialogUpdatePlayerBinding
import com.romrell4.tennisladder.model.*
import com.romrell4.tennisladder.support.*
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Response
import java.util.*

private const val VS_SPINNER_INDEX = 0
private const val VS_LIST_INDEX = 1

class LadderActivity : TLActivity() {
    companion object {
        const val LADDER_EXTRA = "ladder"
    }

    private lateinit var binding: ActivityLadderBinding

    private lateinit var ladder: Ladder
    private val me: Player?
        get() = adapter.list.firstOrNull { FirebaseAuth.getInstance().currentUser?.uid == it.user.userId }

    private val adapter = PlayerAdapter()
    private val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.START or ItemTouchHelper.END) {
        override fun isLongPressDragEnabled() = ladder.loggedInUserIsAdmin && ladder.startDate.after(Date())
        override fun isItemViewSwipeEnabled() = ladder.loggedInUserIsAdmin && ladder.startDate.before(Date())

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            adapter.onMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val alertBinding = DialogUpdatePlayerBinding.inflate(layoutInflater, null, false)
            val editText = alertBinding.editText
            val player = adapter.list[viewHolder.bindingAdapterPosition]
            AlertDialog.Builder(this@LadderActivity)
                .setTitle(R.string.player_update_dialog_title)
                .setMessage(getString(R.string.player_update_dialog_message, player.user.name))
                .setView(alertBinding.root)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    editText.text.toString().toIntOrNull()?.let { newBorrowedPoints ->
                        val newPlayer = player.copy(borrowedPoints = newBorrowedPoints)
                        binding.swipeRefreshLayout.isRefreshing = true
                        Client.api.updatePlayer(ladder.ladderId, player.user.userId, newPlayer).enqueue(object : Callback<List<Player>>(this@LadderActivity) {
                            override fun onResponse(call: Call<List<Player>>, response: Response<List<Player>>) {
                                super.onResponse(call, response)
                                binding.swipeRefreshLayout.isRefreshing = false
                            }

                            override fun onSuccess(data: List<Player>) {
                                loadBottomButton()
                                adapter.list = data
                            }
                        })
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            adapter.notifyItemChanged(viewHolder.bindingAdapterPosition)
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityLadderBinding.inflate(layoutInflater).also { binding = it }.root)

        ladder = intent.requireExtra(LADDER_EXTRA)
        title = ladder.name

        binding.swipeRefreshLayout.setOnRefreshListener { loadPlayers() }

        binding.recyclerView.adapter = adapter
        touchHelper.attachToRecyclerView(binding.recyclerView)
        loadPlayers()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (ladder.loggedInUserIsAdmin) {
            menuInflater.inflate(R.menu.ladders_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.save_player_positions -> {
            updatePlayerOrder(generateBorrowedPoints = false)
            true
        }
        R.id.generate_borrowed_points -> {
            updatePlayerOrder(generateBorrowedPoints = true)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ReportMatchActivity.RC_MATCH_REPORTED) {
            loadPlayers()
        } else if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                loadPlayers()
            } else {
                val response = IdpResponse.fromResultIntent(data)
                println("Failed login with error code: ${response?.error?.errorCode}")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun loadBottomButton() {
        when {
            //The user is logged in AND is in the ladder already
            me != null -> {
                binding.reportMatchButton.setup(R.string.report_match_button_text) {
                    val players = adapter.list.filter { it != me }.sortedBy { it.user.name.toLowerCase(Locale.getDefault()) }
                    var selectedPlayer: Player? = null
                    AlertDialog.Builder(this@LadderActivity)
                        .setTitle(R.string.report_match_dialog_title)
                        .setSingleChoiceItems(players.map { it.user.name }.toTypedArray(), -1) { _, index ->
                            selectedPlayer = players[index]
                        }
                        .setPositiveButton("Select") { _, _ ->
                            selectedPlayer?.let {
                                startActivityForResult(
                                    Intent(this@LadderActivity, ReportMatchActivity::class.java)
                                        .putExtra(ReportMatchActivity.ME_EXTRA, me)
                                        .putExtra(ReportMatchActivity.OPPONENT_EXTRA, it),
                                    ReportMatchActivity.RC_MATCH_REPORTED
                                )
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            //The user is logged in, but is not in the ladder yet
            FirebaseAuth.getInstance().currentUser != null -> {
                binding.reportMatchButton.setup(R.string.request_ladder_invite_text) {
                    @SuppressLint("InflateParams")
                    val alertViewBinding = DialogLadderInviteBinding.inflate(layoutInflater, null, false)
                    val editText = alertViewBinding.editText
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.ladder_invite_dialog_title))
                        .setMessage(getString(R.string.ladder_invite_dialog_message))
                        .setView(alertViewBinding.root)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            binding.viewSwitcher.displayedChild = VS_SPINNER_INDEX
                            Client.api.addPlayerToLadder(ladder.ladderId, editText.text.toString()).enqueue(object : Callback<List<Player>>(this) {
                                override fun onSuccess(data: List<Player>) {
                                    binding.viewSwitcher.displayedChild = VS_LIST_INDEX
                                    loadBottomButton()
                                    adapter.list = data
                                    Toast.makeText(this@LadderActivity, getString(R.string.ladder_invite_success_message), Toast.LENGTH_SHORT).show()
                                }

                                override fun onError(error: ServerError?, t: Throwable) {
                                    binding.viewSwitcher.displayedChild = VS_LIST_INDEX
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
                binding.reportMatchButton.setup(R.string.login_to_report_match_button_text) {
                    startLoginActivity()
                }
            }
        }
    }

    private fun Button.setup(@StringRes textId: Int, listener: ((View) -> Unit)) {
        visibility = View.VISIBLE
        text = getString(textId)
        setOnClickListener(listener)
    }

    private fun loadPlayers() {
        Client.api.getPlayers(ladder.ladderId).enqueue(object : Callback<List<Player>>(this) {
            override fun onSuccess(data: List<Player>) {
                binding.viewSwitcher.displayedChild = VS_LIST_INDEX
                binding.swipeRefreshLayout.isRefreshing = false
                adapter.list = data
                loadBottomButton()
            }
        })
    }

    private fun updatePlayerOrder(generateBorrowedPoints: Boolean) {
        binding.swipeRefreshLayout.isRefreshing = true
        Client.api.updatePlayerOrder(ladder.ladderId, generateBorrowedPoints, adapter.list).enqueue(object : Callback<List<Player>>(this) {
            override fun onResponse(call: Call<List<Player>>, response: Response<List<Player>>) {
                super.onResponse(call, response)
                binding.swipeRefreshLayout.isRefreshing = false
            }

            override fun onSuccess(data: List<Player>) {
                adapter.list = data
            }
        })
    }

    private inner class PlayerAdapter : Adapter<Player>(this, R.string.no_players_text) {
        override fun createViewHolder(parent: ViewGroup) = PlayerViewHolder(CardPlayerBinding.inflate(layoutInflater, parent, false))
        override fun bind(viewHolder: RecyclerView.ViewHolder, item: Player) {
            (viewHolder as? PlayerViewHolder)?.bind(item)
        }

        fun onMove(fromPosition: Int, toPosition: Int) {
            if (fromPosition < toPosition) {
                for (i in fromPosition until toPosition) {
                    Collections.swap(list, i, i + 1)
                }
            } else {
                for (i in fromPosition downTo toPosition + 1) {
                    Collections.swap(list, i, i - 1)
                }
            }
            notifyItemMoved(fromPosition, toPosition)
        }

        private inner class PlayerViewHolder(private val cardBinding: CardPlayerBinding) : RecyclerView.ViewHolder(cardBinding.root) {

            fun bind(player: Player) {
                cardBinding.nameText.text = player.user.name
                if (player.borrowedPoints == 0) {
                    cardBinding.earnedPoints.visibility = View.GONE
                    cardBinding.borrowedPoints.visibility = View.GONE
                } else {
                    cardBinding.earnedPoints.visibility = View.VISIBLE
                    cardBinding.borrowedPoints.visibility = View.VISIBLE
                    cardBinding.earnedPoints.text = getString(R.string.earned_points_text, player.earnedPoints)
                    cardBinding.borrowedPoints.text = getString(R.string.borrowed_points_text, player.borrowedPoints)
                }
                cardBinding.scoreText.text = getString(R.string.total_points_text, player.score)

                Picasso.get().load(player.user.photoUrl?.takeIf { it.isNotEmpty() }).placeholder(R.drawable.ic_default_user).into(cardBinding.profileImage)

                cardBinding.card.setBackgroundColor(ContextCompat.getColor(this@LadderActivity, if (player == me) R.color.meCardColor else R.color.white))

                itemView.setOnClickListener {
                    startActivity(
                        Intent(this@LadderActivity, PlayerActivity::class.java)
                            .putExtra(PlayerActivity.ME_EXTRA, me)
                            .putExtra(PlayerActivity.PLAYER_EXTRA, player)
                            .putExtra(PlayerActivity.IS_ADMIN, ladder.loggedInUserIsAdmin)
                    )
                }
            }
        }
    }
}
