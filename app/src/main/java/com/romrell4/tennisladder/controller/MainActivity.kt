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
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Ladder
import kotlinx.android.synthetic.main.activity_main.*

private const val RC_SIGN_IN = 1

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
        nav_view.setNavigationItemSelectedListener {
            drawer_layout.closeDrawers()
            onOptionsItemSelected(it)
        }

        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = LaddersAdapter(
            listOf(
                Ladder(1, "Rebecca's Ladder"),
                Ladder(2, "Eric's Ladder"),
                Ladder(3, "Cole's Ladder")
            )
        )
    }

    override fun onStart() {
        super.onStart()
        user?.let {
            onLoggedIn()
        } ?: run {
            onLoggedOut()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        nav_view.menu.findItem(R.id.nav_menu_login)?.let { logInMenuItem = it }
        nav_view.menu.findItem(R.id.nav_menu_logout)?.let { logOutMenuItem = it }
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
                    .setAvailableProviders(listOf(AuthUI.IdpConfig.GoogleBuilder().build()))
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

    private fun onLoggedIn() {
        logInMenuItem?.isVisible = false
        logOutMenuItem?.isVisible = true

        //TODO: Update UI
    }

    private fun onLoggedOut() {
        logInMenuItem?.isVisible = true
        logOutMenuItem?.isVisible = false

        //TODO: Update UI
    }

    private inner class LaddersAdapter(var ladders: List<Ladder>):
        RecyclerView.Adapter<LaddersAdapter.LadderViewHolder>() {
        override fun getItemCount() = ladders.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            LadderViewHolder(layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false))

        override fun onBindViewHolder(holder: LadderViewHolder, position: Int) {
            holder.bind(ladders[position])
        }

        private inner class LadderViewHolder(view: View): RecyclerView.ViewHolder(view) {
            private val textView: TextView = view.findViewById(android.R.id.text1)

            fun bind(ladder: Ladder) {
                textView.text = ladder.name
                itemView.setOnClickListener {
                    startActivity(
                        Intent(this@MainActivity, LadderActivity::class.java).putExtra(LadderActivity.LADDER_EXTRA, ladder)
                    )
                }
            }
        }
    }
}
