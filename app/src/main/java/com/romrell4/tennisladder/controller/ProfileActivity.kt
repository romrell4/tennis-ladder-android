package com.romrell4.tennisladder.controller

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.databinding.ActivityProfileBinding
import com.romrell4.tennisladder.databinding.CardProfileInfoBinding
import com.romrell4.tennisladder.databinding.DialogProfileEditValueBinding
import com.romrell4.tennisladder.support.*
import com.romrell4.tennisladder.viewmodel.ProfileViewModel
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

private const val VS_SPINNER_INDEX = 0
private const val VS_LIST_INDEX = 1

class ProfileActivity : TLActivity() {
    companion object {
        const val MY_ID_EXTRA = "MY_ID"
        const val USER_ID_EXTRA = "USER_ID"
    }

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(
                    myUserId = intent.getExtra(MY_ID_EXTRA),
                    profileUserId = intent.requireExtra(USER_ID_EXTRA)
                ) as T
            }
        }
    }

    private val adapter = ProfileInfoAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityProfileBinding.inflate(layoutInflater).also { binding = it }.root)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.viewState.collect { viewState ->
                // Update UI based on viewState
                binding.viewSwitcher.displayedChild = viewState.viewSwitcherIndex

                // Update user-dependent UI
                title = viewState.userName ?: ""
                Picasso.get().load(viewState.userPhotoUrl)
                    .placeholder(R.drawable.ic_default_user)
                    .into(binding.imageView)

                // Configure editable mode
                binding.headerText.isVisible = viewState.isEditable
                if (viewState.isEditable) {
                    binding.imageView.setOnClickListener {
                        showEditDialog(ProfileViewState.FieldType.PhotoUrl.label) { photoUrl ->
                            viewModel.updateUserField(ProfileViewState.FieldType.PhotoUrl, photoUrl)
                        }
                    }
                } else {
                    binding.imageView.setOnClickListener(null)
                }

                // Update adapter with profile rows
                adapter.list = viewState.profileRows
            }
        }

        lifecycleScope.launch {
            viewModel.commandFlow.collect { command ->
                when (command) {
                    is ProfileViewModel.Command.ShowToast -> {
                        Toast.makeText(
                            this@ProfileActivity,
                            command.message,
                            command.duration
                        ).show()
                    }
                    is ProfileViewModel.Command.FinishActivity -> {
                        onBackPressed()
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Only display the save button if the user is looking at their own profile
        if (viewModel.isEditable) {
            menuInflater.inflate(R.menu.profile_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.save -> {
            viewModel.saveProfile()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showEditDialog(label: String, action: (String) -> Unit) {
        val alertBinding = DialogProfileEditValueBinding.inflate(layoutInflater, null, false)
        val editText = alertBinding.editText
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.profile_edit_dialog_title))
            .setMessage(getString(R.string.profile_edit_dialog_message, label))
            .setView(alertBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newValue = editText.text.toString()
                action(newValue)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private inner class ProfileInfoAdapter : Adapter<ProfileViewState.RowData>(this, R.string.no_user_text) {
        override fun createViewHolder(parent: ViewGroup) = RowViewHolder(CardProfileInfoBinding.inflate(layoutInflater, parent, false))
        override fun bind(viewHolder: RecyclerView.ViewHolder, item: ProfileViewState.RowData) {
            (viewHolder as? RowViewHolder)?.bind(item)
        }

        private inner class RowViewHolder(private val cardBinding: CardProfileInfoBinding) : RecyclerView.ViewHolder(cardBinding.root) {

            fun bind(rowData: ProfileViewState.RowData) {
                cardBinding.labelText.text = rowData.label
                cardBinding.valueText.text = rowData.value ?: "Tap to set"
                cardBinding.valueText.setTextColor(ContextCompat.getColor(this@ProfileActivity, if (rowData.value != null) R.color.black else android.R.color.darker_gray))
                if (viewModel.isEditable) {
                    itemView.setOnClickListener {
                        showEditDialog(rowData.label) { newValue ->
                            viewModel.updateUserField(rowData.fieldType, newValue)
                        }
                    }
                } else {
                    itemView.setOnClickListener(null)
                }
            }
        }
    }
}
