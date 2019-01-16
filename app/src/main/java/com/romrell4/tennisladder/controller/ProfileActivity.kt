package com.romrell4.tennisladder.controller

import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.User
import com.romrell4.tennisladder.support.Adapter
import com.romrell4.tennisladder.support.Callback
import com.romrell4.tennisladder.support.TLActivity
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.card_profile_info.view.*
import kotlinx.android.synthetic.main.dialog_profile_edit_value.view.*

private const val VS_SPINNER_INDEX = 0
private const val VS_LIST_INDEX = 1

class ProfileActivity: TLActivity() {
	companion object {
		const val USER_ID_EXTRA = "USER_ID"
	}

	private var user: User? = null
		set(value) {
			field = value
			value?.let {
				Picasso.get().load(it.photoUrl).placeholder(R.drawable.ic_default_user).into(image_view)
				adapter.list = listOf(
					RowData("Email", it.email) { email ->
						user?.email = email
					},
					RowData("Name", it.name) { name ->
						user?.name = name
					},
					RowData("Phone Number", it.phoneNumber) { phoneNumber ->
						user?.phoneNumber = phoneNumber
					}
				)
			}
		}
	private val adapter = ProfileInfoAdapter()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_profile)

		image_view.setOnClickListener {
			showEditDialog("Photo URL") {
				user?.photoUrl = it
				Picasso.get().load(it).placeholder(R.drawable.ic_default_user).into(image_view)
			}
		}

		recycler_view.layoutManager = LinearLayoutManager(this)
		recycler_view.adapter = adapter

		Client.api.getUser(intent.getStringExtra(USER_ID_EXTRA)).enqueue(object: Callback<User>(this) {
			override fun onSuccess(data: User) {
				view_switcher.displayedChild = VS_LIST_INDEX
				user = data
			}
		})
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.profile_menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
		R.id.save -> {
			user?.let {
				view_switcher.displayedChild = VS_SPINNER_INDEX
				Client.api.updateUser(it.userId, it).enqueue(object: Callback<User>(this) {
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
		val alertView = layoutInflater.inflate(R.layout.dialog_profile_edit_value, null)
		val editText = alertView.edit_text
		AlertDialog.Builder(this)
			.setTitle(getString(R.string.profile_edit_dialog_title))
			.setMessage(getString(R.string.profile_edit_dialog_message, label))
			.setView(alertView)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val newValue = editText.text.toString()
				action(newValue)
			}
			.setNegativeButton(android.R.string.cancel, null)
			.show()
	}

	private inner class ProfileInfoAdapter: Adapter<RowData>(this, R.string.no_user_text) {
		override fun createViewHolder(parent: ViewGroup) = RowViewHolder(layoutInflater.inflate(R.layout.card_profile_info, parent, false))
		override fun bind(viewHolder: RecyclerView.ViewHolder, item: RowData) {
			(viewHolder as? RowViewHolder)?.bind(item)
		}

		private inner class RowViewHolder(view: View): RecyclerView.ViewHolder(view) {
			private val labelText = view.label_text
			private val valueText = view.value_text

			fun bind(rowData: RowData) {
				labelText.text = rowData.label
				valueText.text = rowData.value ?: "Tap to set"
				valueText.setTextColor(ContextCompat.getColor(this@ProfileActivity, if (rowData.value != null) R.color.black else android.R.color.darker_gray))
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
