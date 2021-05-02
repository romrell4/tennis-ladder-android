package com.romrell4.tennisladder.controller

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.databinding.ActivityProfileBinding
import com.romrell4.tennisladder.databinding.CardProfileInfoBinding
import com.romrell4.tennisladder.databinding.DialogProfileEditValueBinding
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.User
import com.romrell4.tennisladder.support.*
import com.squareup.picasso.Picasso

private const val VS_SPINNER_INDEX = 0
private const val VS_LIST_INDEX = 1

class ProfileActivity : TLActivity() {
    companion object {
        const val MY_ID_EXTRA = "MY_ID"
        const val USER_ID_EXTRA = "USER_ID"
    }

    private lateinit var binding: ActivityProfileBinding

    private val editable
        get() = intent.getExtra<String>(MY_ID_EXTRA) == intent.getExtra<String>(USER_ID_EXTRA)

    private var user: User? = null
        set(value) {
            field = value
            value?.let {
                title = it.name
                Picasso.get().load(it.photoUrl).placeholder(R.drawable.ic_default_user).into(binding.imageView)
                adapter.list = listOf(
                    RowData("Email", it.email) { email ->
                        user?.email = email
                    },
                    RowData("Name", it.name) { name ->
                        user?.name = name
                    },
                    RowData("Phone Number", it.phoneNumber) { phoneNumber ->
                        user?.phoneNumber = phoneNumber
                    },
                    RowData("Availability", it.availabilityText) { availabilityText ->
                        user?.availabilityText = availabilityText
                    }
                )
            }
        }
    private val adapter = ProfileInfoAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityProfileBinding.inflate(layoutInflater).also { binding = it }.root)

        //Configure the view differently if they're looking at their own profile
        if (editable) {
            binding.headerText.visibility = View.VISIBLE
            binding.imageView.setOnClickListener {
                showEditDialog("Photo URL") { photoUrl ->
                    //If they didn't add a URL, default to null
                    photoUrl.takeIf { it.isNotBlank() }.let {
                        user?.photoUrl = photoUrl
                        Picasso.get().load(it).placeholder(R.drawable.ic_default_user).into(binding.imageView)
                    }
                }
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        Client.api.getUser(intent.requireExtra(USER_ID_EXTRA)).enqueue(object : Callback<User>(this) {
            override fun onSuccess(data: User) {
                binding.viewSwitcher.displayedChild = VS_LIST_INDEX
                user = data
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //Only display the save button if the user is looking at their own profile
        if (editable) {
            menuInflater.inflate(R.menu.profile_menu, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.save -> {
            user?.let {
                binding.viewSwitcher.displayedChild = VS_SPINNER_INDEX
                Client.api.updateUser(it.userId, it).enqueue(object : Callback<User>(this) {
                    override fun onSuccess(data: User) {
                        Toast.makeText(this@ProfileActivity, "Profile successfully updated", Toast.LENGTH_SHORT).show()
                        onBackPressed()
                    }
                })
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private data class RowData(
        val label: String,
        var value: String?,
        val action: (String) -> Unit
    )

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

    private inner class ProfileInfoAdapter : Adapter<RowData>(this, R.string.no_user_text) {
        override fun createViewHolder(parent: ViewGroup) = RowViewHolder(CardProfileInfoBinding.inflate(layoutInflater, parent, false))
        override fun bind(viewHolder: RecyclerView.ViewHolder, item: RowData) {
            (viewHolder as? RowViewHolder)?.bind(item)
        }

        private inner class RowViewHolder(private val cardBinding: CardProfileInfoBinding) : RecyclerView.ViewHolder(cardBinding.root) {

            fun bind(rowData: RowData) {
                cardBinding.labelText.text = rowData.label
                cardBinding.valueText.text = rowData.value ?: "Tap to set"
                cardBinding.valueText.setTextColor(ContextCompat.getColor(this@ProfileActivity, if (rowData.value != null) R.color.black else android.R.color.darker_gray))
                if (editable) {
                    itemView.setOnClickListener {
                        showEditDialog(rowData.label) {
                            rowData.action(it)
                            list[list.indexOf(rowData)].value = it
                            notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }
}
