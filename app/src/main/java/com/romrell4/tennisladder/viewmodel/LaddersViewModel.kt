package com.romrell4.tennisladder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.repository.Repository
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

class LaddersViewModel : ViewModel() {
    private val repository = Repository()

    private val _commandFlow = MutableSharedFlow<Command>()
    val commandFlow = _commandFlow.asSharedFlow()

    private val state = MutableStateFlow(State())
    val viewState: StateFlow<LaddersViewState> = state.map { it.toViewState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, state.value.toViewState())

    init {
        loadLadders()
    }

    fun loadLadders() {
        viewModelScope.launch(Dispatchers.IO) {
            state.update { it.copy(isLoading = true) }
            repository.getLadders()
                .also { state.update { it.copy(isLoading = false) } }
                .onSuccess { ladders ->
                    state.update { it.copy(ladders = ladders) }
                }
                .onFailure {
                    _commandFlow.emit(Command.ShowToast(it.readableMessage()))
                }
        }
    }

    private data class State(
        val ladders: List<Ladder> = emptyList(),
        val isLoading: Boolean = true,
    ) {
        fun toViewState(): LaddersViewState {
            val ladders = ladders
            return LaddersViewState(
                ladders = ladders,
                viewSwitcherIndex = if (isLoading && ladders.isEmpty()) 0 else 1,
                swipeLoadingDisplayed = isLoading,
            )
        }
    }

    sealed interface Command {
        data class ShowToast(val message: String) : Command
    }
}

data class LaddersViewState(
    val ladders: List<Ladder>,
    val viewSwitcherIndex: Int,
    val swipeLoadingDisplayed: Boolean,
)