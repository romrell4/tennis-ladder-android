package com.romrell4.tennisladder.controller

import android.content.Intent
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.support.Adapter
import com.romrell4.tennisladder.support.Callback
import com.romrell4.tennisladder.support.TLActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.card_ladder.view.*
import kotlinx.android.synthetic.main.nav_header.view.*
import java.text.SimpleDateFormat
import java.util.*

private const val VS_LIST_INDEX = 1
private val DATE_FORMAT = SimpleDateFormat("M/d/yyyy", Locale.US)

class MainActivity: TLActivity() {
	private var logInMenuItem: MenuItem? = null
	private var logOutMenuItem: MenuItem? = null
	private var profileMenuItem: MenuItem? = null
	private val adapter = LadderAdapter()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		FirebaseApp.initializeApp(this)

		supportActionBar?.apply {
			setDisplayHomeAsUpEnabled(true)
			setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp)
		}

		nav_view.apply {
			setNavigationItemSelectedListener {
				drawer_layout.closeDrawers()
				onOptionsItemSelected(it)
			}
			menu.findItem(R.id.nav_menu_login)?.let { logInMenuItem = it }
			menu.findItem(R.id.nav_menu_logout)?.let { logOutMenuItem = it }
			menu.findItem(R.id.nav_menu_profile)?.let { profileMenuItem = it }
		}

		recycler_view.layoutManager = LinearLayoutManager(this)
		recycler_view.adapter = adapter

		loadLadders()
	}

	override fun onStart() {
		super.onStart()

		//Reload the logged in state (in case they logged in on a deeper screen)
		FirebaseAuth.getInstance().currentUser?.let {
			onLoggedIn()
		} ?: run {
			onLoggedOut()
		}
	}

	override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
		android.R.id.home -> {
			drawer_layout.openDrawer(GravityCompat.START)
			true
		}
		R.id.nav_menu_login -> {
			startLoginActivity()
			true
		}
		R.id.nav_menu_logout -> {
			AuthUI.getInstance().signOut(this)
			onLoggedOut()
			true
		}
		R.id.nav_menu_profile -> {
			FirebaseAuth.getInstance().currentUser?.uid?.let {
				startActivity(Intent(this, ProfileActivity::class.java).putExtra(ProfileActivity.USER_ID_EXTRA, it))
			}
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)

		if (requestCode == RC_SIGN_IN) {
			if (resultCode == AppCompatActivity.RESULT_OK) {
				onLoggedIn()
			} else {
				val response = IdpResponse.fromResultIntent(data)
				println("Failed login with error code: ${response?.error?.errorCode}")
			}
		}
	}

	private fun loadLadders() {
		Client.api.getLadders().enqueue(object: Callback<List<Ladder>>(this) {
			override fun onSuccess(data: List<Ladder>) {
				view_switcher.displayedChild = VS_LIST_INDEX
				adapter.list = data
			}
		})
	}

	private fun onLoggedIn() {
		logInMenuItem?.isVisible = false
		logOutMenuItem?.isVisible = true
		profileMenuItem?.isVisible = true

		FirebaseAuth.getInstance().currentUser?.displayName.let {
			val text = getString(R.string.logged_in_text, it)
			supportActionBar?.subtitle = text
			nav_view.nav_header_subtitle?.text = text
		}
	}

	private fun onLoggedOut() {
		logInMenuItem?.isVisible = true
		logOutMenuItem?.isVisible = false
		profileMenuItem?.isVisible = false

		val text = getString(R.string.not_logged_in)
		supportActionBar?.subtitle = text
		nav_view.nav_header_subtitle?.text = text
	}

	private inner class LadderAdapter: Adapter<Ladder>(this, R.string.no_ladders_text) {
		override fun createViewHolder(parent: ViewGroup) = LadderViewHolder(layoutInflater.inflate(R.layout.card_ladder, parent, false))
		override fun bind(viewHolder: RecyclerView.ViewHolder, item: Ladder) {
			(viewHolder as? LadderViewHolder)?.bind(item)
		}

		private inner class LadderViewHolder(view: View): RecyclerView.ViewHolder(view) {
			private val nameText = view.name_text
			private val dateText = view.date_text

			fun bind(ladder: Ladder) {
				nameText.text = ladder.name
				dateText.text = getString(R.string.date_format, DATE_FORMAT.format(ladder.startDate), DATE_FORMAT.format(ladder.endDate))
				itemView.setOnClickListener {
					startActivity(Intent(this@MainActivity, LadderActivity::class.java).putExtra(LadderActivity.LADDER_EXTRA, ladder))
				}
			}
		}
	}
}
