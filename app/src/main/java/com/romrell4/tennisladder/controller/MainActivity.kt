package com.romrell4.tennisladder.controller

import android.content.Intent
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.romrell4.tennisladder.BuildConfig
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.support.Adapter
import com.romrell4.tennisladder.support.SuccessCallback
import com.romrell4.tennisladder.support.TLActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.card_ladder.view.*
import kotlinx.android.synthetic.main.nav_header.view.*
import java.text.SimpleDateFormat
import java.util.*

private const val RC_SIGN_IN = 1
private const val VS_LIST_INDEX = 1
private val DATE_FORMAT = SimpleDateFormat("M/d/yyyy", Locale.US)

class MainActivity: TLActivity() {
	private var logInMenuItem: MenuItem? = null
	private var logOutMenuItem: MenuItem? = null
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
		}

		recycler_view.layoutManager = LinearLayoutManager(this)
		recycler_view.adapter = adapter

		loadLadders()
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		FirebaseAuth.getInstance().currentUser?.let {
			onLoggedIn(false)
		} ?: run {
			onLoggedOut(false)
		}
		return super.onCreateOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
		android.R.id.home -> {
			drawer_layout.openDrawer(GravityCompat.START)
			true
		}
		R.id.nav_menu_login -> {
			startActivityForResult(
				AuthUI.getInstance().createSignInIntentBuilder()
					.setLogo(R.drawable.ic_tennis_ladder)
					.setIsSmartLockEnabled(!BuildConfig.DEBUG)
					.setAvailableProviders(listOf(
						AuthUI.IdpConfig.GoogleBuilder(),
						AuthUI.IdpConfig.EmailBuilder()
					).map { it.build() }).build(), RC_SIGN_IN
			)
			true
		}
		R.id.nav_menu_logout -> {
			AuthUI.getInstance().signOut(this)
			onLoggedOut()
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
		Client.api.getLadders().enqueue(object: SuccessCallback<List<Ladder>>(this) {
			override fun onSuccess(data: List<Ladder>) {
				view_switcher.displayedChild = VS_LIST_INDEX
				adapter.list = data
			}
		})
	}

	private fun onLoggedIn(displayToast: Boolean = true) {
		logInMenuItem?.isVisible = false
		logOutMenuItem?.isVisible = true

		FirebaseAuth.getInstance().currentUser?.displayName.let {
			nav_view.nav_header_subtitle.text = it
			if (displayToast) Toast.makeText(this, "Logged in as $it", Toast.LENGTH_SHORT).show()
		}
	}

	private fun onLoggedOut(displayToast: Boolean = true) {
		logInMenuItem?.isVisible = true
		logOutMenuItem?.isVisible = false

		nav_view.nav_header_subtitle.text = getString(R.string.not_logged_in)
		if (displayToast) Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
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
					startActivity(
						Intent(this@MainActivity, LadderActivity::class.java).putExtra(LadderActivity.LADDER_EXTRA, ladder)
					)
				}
			}
		}
	}
}
