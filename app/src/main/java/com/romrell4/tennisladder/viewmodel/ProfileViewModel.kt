package com.romrell4.tennisladder.viewmodel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.User
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

class ProfileViewModel(
    private val myUserId: String?,
    private val profileUserId: String
) : ViewModel() {
    private val repository = Repository()

    private val _commandFlow = MutableSharedFlow<Command>()
    val commandFlow = _commandFlow.asSharedFlow()

    private val state = MutableStateFlow(
        State(
            myUserId = myUserId,
            profileUserId = profileUserId
        )
    )
    val viewState: StateFlow<ProfileViewState> = state.map { it.toViewState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, state.value.toViewState())

    val isEditable: Boolean
        get() = myUserId == profileUserId

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch(Dispatchers.IO) {
            state.update { it.copy(isLoading = true) }
            repository.getUser(profileUserId)
                .also { state.update { it.copy(isLoading = false) } }
                .onSuccess { user ->
                    state.update { it.copy(user = user) }
                }
                .onFailure { error ->
                    _commandFlow.emit(Command.ShowToast(error.readableMessage(), Toast.LENGTH_LONG))
                }
        }
    }

    fun updateUserField(fieldType: ProfileViewState.FieldType, value: String) {
        val currentUser = state.value.user ?: return
        val updatedUser = when (fieldType) {
            ProfileViewState.FieldType.Email -> currentUser.copy(email = value)
            ProfileViewState.FieldType.Name -> currentUser.copy(name = value)
            ProfileViewState.FieldType.PhoneNumber -> currentUser.copy(phoneNumber = value)
            ProfileViewState.FieldType.Availability -> currentUser.copy(availabilityText = value)
            ProfileViewState.FieldType.PhotoUrl -> currentUser.copy(photoUrl = value.takeIf { it.isNotBlank() })
        }
        state.update { it.copy(user = updatedUser) }
    }

    fun saveProfile() {
        val currentUser = state.value.user ?: return
        if (state.value.isSaving) return // Prevent duplicate calls
        
        viewModelScope.launch(Dispatchers.IO) {
            state.update { it.copy(isSaving = true) }
            repository.updateUser(currentUser.userId, currentUser)
                .also { state.update { it.copy(isSaving = false) } }
                .onSuccess { updatedUser ->
                    state.update { it.copy(user = updatedUser) }
                    _commandFlow.emit(Command.ShowToast("Profile successfully updated", Toast.LENGTH_SHORT))
                    _commandFlow.emit(Command.FinishActivity)
                }
                .onFailure { error ->
                    _commandFlow.emit(Command.ShowToast(error.readableMessage(), Toast.LENGTH_LONG))
                }
        }
    }

    private data class State(
        val myUserId: String?,
        val profileUserId: String,
        val user: User? = null,
        val isLoading: Boolean = false,
        val isSaving: Boolean = false
    ) {
        fun toViewState(): ProfileViewState {
            val isEditable = myUserId == profileUserId
            val viewSwitcherIndex = when {
                (isLoading && user == null) || isSaving -> 0 // VS_SPINNER_INDEX
                else -> 1 // VS_LIST_INDEX
            }
            return ProfileViewState(
                userName = user?.name,
                userPhotoUrl = user?.photoUrl?.takeIf { it.isNotBlank() },
                isEditable = isEditable,
                viewSwitcherIndex = viewSwitcherIndex,
                profileRows = user?.let { createProfileRows(it) } ?: emptyList()
            )
        }

        private fun createProfileRows(user: User): List<ProfileViewState.RowData> {
            return listOf(
                ProfileViewState.RowData(ProfileViewState.FieldType.Email, user.email),
                ProfileViewState.RowData(ProfileViewState.FieldType.Name, user.name),
                ProfileViewState.RowData(ProfileViewState.FieldType.PhoneNumber, user.phoneNumber),
                ProfileViewState.RowData(ProfileViewState.FieldType.Availability, user.availabilityText)
            )
        }
    }

    sealed interface Command {
        data class ShowToast(val message: String, val duration: Int) : Command
        data object FinishActivity : Command
    }
}

data class ProfileViewState(
    val userName: String?,
    val userPhotoUrl: String?,
    val isEditable: Boolean,
    val viewSwitcherIndex: Int,
    val profileRows: List<RowData>
) {
    data class RowData(
        val fieldType: FieldType,
        val value: String?
    ) {
        val label: String get() = fieldType.label
    }
    
    sealed class FieldType(val label: String) {
        data object Email : FieldType("Email")
        data object Name : FieldType("Name")
        data object PhoneNumber : FieldType("Phone Number")
        data object Availability : FieldType("Availability")
        data object PhotoUrl : FieldType("Photo URL")
    }
} 