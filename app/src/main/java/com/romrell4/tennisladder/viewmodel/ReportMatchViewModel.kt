package com.romrell4.tennisladder.viewmodel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Match
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.repository.Repository
import com.romrell4.tennisladder.repository.ServiceException
import com.romrell4.tennisladder.support.App
import com.romrell4.tennisladder.support.readableMessage
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

class ReportMatchViewModel(
    private val me: Player,
    private val opponent: Player
) : ViewModel() {
    private val repository = Repository()

    private val _commandFlow = MutableSharedFlow<Command>()
    val commandFlow = _commandFlow.asSharedFlow()

    private val state = MutableStateFlow(State(me = me, opponent = opponent))
    val viewState: StateFlow<ReportMatchViewState> = state.map { it.toViewState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, state.value.toViewState())

    fun reportMatch(match: Match) {
        if (state.value.isReporting) return // Prevent duplicate calls
        
        viewModelScope.launch(Dispatchers.IO) {
            state.update { it.copy(isReporting = true) }
            repository.reportMatch(me.ladderId, match)
                .also { state.update { it.copy(isReporting = false) } }
                .onSuccess { reportedMatch ->
                    _commandFlow.emit(Command.ShowToast(App.context.getString(R.string.report_success_message), Toast.LENGTH_SHORT))
                    _commandFlow.emit(Command.FinishWithResult)
                }
                .onFailure { error ->
                    _commandFlow.emit(Command.ShowToast(error.readableMessage(), Toast.LENGTH_LONG))
                }
        }
    }

    private data class State(
        val me: Player,
        val opponent: Player,
        val isReporting: Boolean = false
    ) {
        fun toViewState(): ReportMatchViewState {
            return ReportMatchViewState(
                meName = me.user.name,
                opponentName = opponent.user.name,
                meImageUrl = me.user.photoUrl,
                opponentImageUrl = opponent.user.photoUrl
            )
        }
    }

    sealed interface Command {
        data class ShowToast(val message: String, val duration: Int) : Command
        data object FinishWithResult : Command
    }
}

data class ReportMatchViewState(
    val meName: String,
    val opponentName: String,
    val meImageUrl: String?,
    val opponentImageUrl: String?
) 