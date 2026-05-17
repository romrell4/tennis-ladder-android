package com.romrell4.tennisladder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.romrell4.tennisladder.model.Match
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.repository.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(val me: Player?, val player: Player, val isAdmin: Boolean) : ViewModel() {
    private val repository = Repository()

    private val _commandFlow = MutableSharedFlow<Command>()
    val commandFlow = _commandFlow.asSharedFlow()

    private val state = MutableStateFlow(State())
    val viewState: StateFlow<PlayerViewState> = state.map { it.toViewState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, state.value.toViewState())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getMatches(player.ladderId, player.user.userId)
                .onSuccess {}
        }
    }

    private data class State(
        val matches: List<Match>? = null,
    ) {
        fun toViewState() = PlayerViewState(
            viewSwitcherIndex = if (matches == null) 0 else 1,
        )
    }

    sealed interface Command {
        data class ShowToast(val message: String) : Command
        data class ShowReportMatchDialog(val otherPlayers: List<Player>, val currentPlayer: Player) : Command
        data object ShowRequestInviteDialog : Command
    }
}

data class PlayerViewState(
    val viewSwitcherIndex: Int,
)