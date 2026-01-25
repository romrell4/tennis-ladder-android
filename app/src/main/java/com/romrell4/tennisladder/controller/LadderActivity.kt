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
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.IdpResponse
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.databinding.ActivityLadderBinding
import com.romrell4.tennisladder.databinding.CardPlayerBinding
import com.romrell4.tennisladder.databinding.DialogLadderInviteBinding
import com.romrell4.tennisladder.databinding.DialogUpdatePlayerBinding
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.support.Adapter
import com.romrell4.tennisladder.support.TLActivity
import com.romrell4.tennisladder.support.bindVisibilityTo
import com.romrell4.tennisladder.support.requireExtra
import com.romrell4.tennisladder.viewmodel.BottomButtonState
import com.romrell4.tennisladder.viewmodel.LadderViewModel
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.Date

class LadderActivity : TLActivity() {
    companion object {
        const val LADDER_EXTRA = "ladder"
    }

    private lateinit var binding: ActivityLadderBinding
    private val viewModel: LadderViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LadderViewModel(intent.requireExtra(LADDER_EXTRA)) as T
            }
        }
    }

    private lateinit var ladder: Ladder
    private val adapter = PlayerAdapter()
    private val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        ItemTouchHelper.START or ItemTouchHelper.END
    ) {
        override fun isLongPressDragEnabled() =
            ladder.loggedInUserIsAdmin && ladder.startDate.after(Date())

        override fun isItemViewSwipeEnabled() =
            ladder.loggedInUserIsAdmin && ladder.startDate.before(Date())

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
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
                        viewModel.updatePlayer(newPlayer)
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

        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.loadPlayers() }

        binding.recyclerView.adapter = adapter
        touchHelper.attachToRecyclerView(binding.recyclerView)

        lifecycleScope.launch {
            viewModel.viewState.collect { viewState ->
                binding.viewSwitcher.displayedChild = viewState.viewSwitcherIndex
                binding.swipeRefreshLayout.isRefreshing = viewState.swipeLoadingDisplayed
                adapter.list = viewState.players
                binding.reportMatchButton.bindVisibilityTo(viewState.bottomButtonState) {
                    val button = it
                    setup(button.buttonText) {
                        when (button) {
                            is BottomButtonState.ReportMatch -> viewModel.reportMatchClicked()
                            is BottomButtonState.RequestInvite -> viewModel.requestInviteClicked()
                            is BottomButtonState.Login -> startLoginActivity()
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.commandFlow.collect { it.execute() }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (ladder.loggedInUserIsAdmin) {
            menuInflater.inflate(R.menu.ladders_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.save_player_positions -> {
            viewModel.updatePlayerOrder(generateBorrowedPoints = false)
            true
        }

        R.id.generate_borrowed_points -> {
            viewModel.updatePlayerOrder(generateBorrowedPoints = true)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == ReportMatchActivity.RC_MATCH_REPORTED) {
            viewModel.loadPlayers()
        } else if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                viewModel.loadPlayers()
            } else {
                val response = IdpResponse.fromResultIntent(data)
                println("Failed login with error code: ${response?.error?.errorCode}")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun LadderViewModel.Command.execute() {
        when (this) {
            is LadderViewModel.Command.ShowToast -> {
                Toast.makeText(this@LadderActivity, message, Toast.LENGTH_SHORT)
                    .show()
            }

            is LadderViewModel.Command.ShowReportMatchDialog -> {
                var selectedPlayer: Player? = null
                AlertDialog.Builder(this@LadderActivity)
                    .setTitle(R.string.report_match_dialog_title)
                    .setSingleChoiceItems(otherPlayers.map { it.user.name }
                        .toTypedArray(), -1) { _, index ->
                        selectedPlayer = otherPlayers[index]
                    }
                    .setPositiveButton("Select") { _, _ ->
                        selectedPlayer?.let {
                            startActivityForResult(
                                Intent(this@LadderActivity, ReportMatchActivity::class.java)
                                    .putExtra(ReportMatchActivity.ME_EXTRA, currentPlayer)
                                    .putExtra(ReportMatchActivity.OPPONENT_EXTRA, it),
                                ReportMatchActivity.RC_MATCH_REPORTED
                            )
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            is LadderViewModel.Command.ShowRequestInviteDialog -> {
                @SuppressLint("InflateParams")
                val alertViewBinding =
                    DialogLadderInviteBinding.inflate(layoutInflater, null, false)
                val editText = alertViewBinding.editText
                AlertDialog.Builder(this@LadderActivity)
                    .setTitle(getString(R.string.ladder_invite_dialog_title))
                    .setMessage(getString(R.string.ladder_invite_dialog_message))
                    .setView(alertViewBinding.root)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.addPlayerToLadder(editText.text.toString())
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }

    private fun Button.setup(@StringRes textId: Int, listener: ((View) -> Unit)) {
        isVisible = true
        text = getString(textId)
        setOnClickListener(listener)
    }

    private inner class PlayerAdapter : Adapter<Player>(this, R.string.no_players_text) {
        override fun createViewHolder(parent: ViewGroup) =
            PlayerViewHolder(CardPlayerBinding.inflate(layoutInflater, parent, false))

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

        private inner class PlayerViewHolder(private val cardBinding: CardPlayerBinding) :
            RecyclerView.ViewHolder(cardBinding.root) {

            fun bind(player: Player) {
                cardBinding.nameText.text = player.user.name
                cardBinding.rankingText.text =
                    getString(R.string.player_cell_ranking_text_format, player.ranking)
                if (player.borrowedPoints == 0) {
                    cardBinding.earnedPoints.visibility = View.GONE
                    cardBinding.borrowedPoints.visibility = View.GONE
                } else {
                    cardBinding.earnedPoints.visibility = View.VISIBLE
                    cardBinding.borrowedPoints.visibility = View.VISIBLE
                    cardBinding.earnedPoints.text =
                        getString(R.string.earned_points_text, player.earnedPoints)
                    cardBinding.borrowedPoints.text =
                        getString(R.string.borrowed_points_text, player.borrowedPoints)
                }
                cardBinding.scoreText.text = getString(R.string.total_points_text, player.score)

                Picasso.get().load(player.user.photoUrl?.takeIf { it.isNotEmpty() })
                    .placeholder(R.drawable.ic_default_user).into(cardBinding.profileImage)

                val me = viewModel.getCurrentPlayer()
                cardBinding.card.setBackgroundColor(
                    ContextCompat.getColor(
                        this@LadderActivity,
                        if (player == me) R.color.meCardColor else R.color.white
                    )
                )

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
