package com.romrell4.tennisladder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.repository.Repository
import com.romrell4.tennisladder.support.App
import com.romrell4.tennisladder.support.readableMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class LadderViewModel(private val ladder: Ladder) : ViewModel() {
    private val repository = Repository()

    private val _commandFlow = MutableSharedFlow<Command>()
    val commandFlow = _commandFlow.asSharedFlow()

    private val state = MutableStateFlow(State())
    val viewState: StateFlow<LadderViewState> = state.map { it.toViewState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, state.value.toViewState())

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        state.update { it.copy(currentUser = user) }
    }

    init {
        loadPlayers()
        setupAuthListener()
    }

    private fun setupAuthListener() {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }

    fun loadPlayers() {
        viewModelScope.launch(Dispatchers.IO) {
            state.update { it.copy(isLoading = true) }
            repository.getPlayers(ladder.ladderId)
                .also { state.update { it.copy(isLoading = false) } }
                .onSuccess { players ->
                    state.update { it.copy(players = players) }
                }.onFailure {
                    _commandFlow.emit(Command.ShowToast(it.readableMessage()))
                }
        }
    }

    fun updatePlayerOrder(generateBorrowedPoints: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            state.update { it.copy(isLoading = true) }
            val currentPlayers = state.value.players
            repository.updatePlayerOrder(ladder.ladderId, generateBorrowedPoints, currentPlayers)
                .also { state.update { it.copy(isLoading = false) } }
                .onSuccess { players ->
                    state.update { it.copy(players = players) }
                }.onFailure {
                    _commandFlow.emit(Command.ShowToast(it.readableMessage()))
                }
        }
    }

    fun addPlayerToLadder(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            state.update { it.copy(isLoading = true) }
            repository.addPlayerToLadder(ladder.ladderId, code)
                .also { state.update { it.copy(isLoading = false) } }
                .onSuccess { players ->
                    state.update { it.copy(players = players) }
                    _commandFlow.emit(Command.ShowToast(App.context.getString(R.string.ladder_invite_success_message)))
                }.onFailure {
                    _commandFlow.emit(Command.ShowToast(it.readableMessage()))
                }
        }
    }

    fun updatePlayer(player: Player) {
        viewModelScope.launch(Dispatchers.IO) {
            state.update { it.copy(isLoading = true) }
            repository.updatePlayer(ladder.ladderId, player.user.userId, player)
                .also { state.update { it.copy(isLoading = false) } }
                .onSuccess { players ->
                    state.update { it.copy(players = players) }
                }.onFailure {
                    _commandFlow.emit(Command.ShowToast(it.readableMessage()))
                }
        }
    }

    fun reportMatchClicked() {
        val me = state.value.me ?: return
        val otherPlayers = state.value.players.filter { it != me }.sortedBy { it.user.name.toLowerCase(Locale.getDefault()) }

        viewModelScope.launch {
            _commandFlow.emit(Command.ShowReportMatchDialog(otherPlayers, me))
        }
    }

    fun requestInviteClicked() {
        viewModelScope.launch {
            _commandFlow.emit(Command.ShowRequestInviteDialog)
        }
    }

    fun getCurrentPlayer(): Player? = state.value.me

    private data class State(
        val players: List<Player> = emptyList(),
        val isLoading: Boolean = true,
        val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser,
    ) {
        val me: Player? = players.firstOrNull { currentUser?.uid == it.user.userId }
        
        fun toViewState(): LadderViewState {
            val bottomButtonState = when {
                isLoading -> null
                // If we're logged in AND have joined the ladder already (a player with our ID exists in the player list) - we can report a match
                me != null -> BottomButtonState.ReportMatch
                // If we're logged in, but not yet in the ladder - we can request an invite
                currentUser != null -> BottomButtonState.RequestInvite
                // If we're not logged in - we can login
                else -> BottomButtonState.Login
            }
            
            return LadderViewState(
                players = players,
                viewSwitcherIndex = if (isLoading && players.isEmpty()) 0 else 1,
                swipeLoadingDisplayed = isLoading,
                bottomButtonState = bottomButtonState
            )
        }
    }

    sealed interface Command {
        data class ShowToast(val message: String) : Command
        data class ShowReportMatchDialog(val otherPlayers: List<Player>, val currentPlayer: Player) : Command
        data object ShowRequestInviteDialog : Command
    }
}

data class LadderViewState(
    val players: List<Player>,
    val viewSwitcherIndex: Int,
    val swipeLoadingDisplayed: Boolean,
    val bottomButtonState: BottomButtonState?
)

sealed interface BottomButtonState {
    data object ReportMatch : BottomButtonState
    data object RequestInvite : BottomButtonState
    data object Login : BottomButtonState
    
    val buttonText: Int
        get() = when (this) {
            is ReportMatch -> R.string.report_match_button_text
            is RequestInvite -> R.string.request_ladder_invite_text
            is Login -> R.string.login_to_report_match_button_text
        }
} 