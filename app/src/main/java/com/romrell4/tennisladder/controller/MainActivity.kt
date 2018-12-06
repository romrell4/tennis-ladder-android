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
import android.widget.TextView
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Ladder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.nav_header.view.*
import java.text.SimpleDateFormat
import java.util.*

private const val RC_SIGN_IN = 1
private val DATE_FORMAT = SimpleDateFormat("MM/dd/yyyy", Locale.US)

class MainActivity: AppCompatActivity() {
    private var logInMenuItem: MenuItem? = null
    private var logOutMenuItem: MenuItem? = null
    private val user: FirebaseUser?
        get() = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)

        setSupportActionBar(findViewById(R.id.toolbar))
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
        recycler_view.adapter = LaddersAdapter(
            listOf(
                Ladder(1, "Rebecca's Ladder", Date(), Date()),
                Ladder(2, "Eric's Ladder", Date(), Date()),
                Ladder(3, "Cole's Ladder", Date(), Date())
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        user?.let {
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
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(listOf(
                        AuthUI.IdpConfig.GoogleBuilder(),
                        AuthUI.IdpConfig.EmailBuilder()
                    ).map { it.build() })
                    .build(), RC_SIGN_IN
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

    private fun onLoggedIn(displayToast: Boolean = true) {
        logInMenuItem?.isVisible = false
        logOutMenuItem?.isVisible = true

        user?.displayName.let {
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

    private inner class LaddersAdapter(var ladders: List<Ladder>): RecyclerView.Adapter<LaddersAdapter.LadderViewHolder>() {
        override fun getItemCount() = ladders.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LadderViewHolder(layoutInflater.inflate(R.layout.card_ladder, parent, false))

        override fun onBindViewHolder(holder: LadderViewHolder, position: Int) {
            holder.bind(ladders[position])
        }

        private inner class LadderViewHolder(view: View): RecyclerView.ViewHolder(view) {
            private val nameText: TextView = view.findViewById(R.id.name_text)
            private val dateText: TextView = view.findViewById(R.id.date_text)

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
