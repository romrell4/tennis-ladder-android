package com.romrell4.tennisladder.controller

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.databinding.ActivityMainBinding
import com.romrell4.tennisladder.databinding.CardLadderBinding
import com.romrell4.tennisladder.databinding.NavHeaderBinding
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.support.*
import java.text.SimpleDateFormat
import java.util.*

private const val VS_LIST_INDEX = 1
private val DATE_FORMAT = SimpleDateFormat("M/d/yyyy", Locale.US)

class MainActivity : TLActivity() {
    private lateinit var binding: ActivityMainBinding

    private var logInMenuItem: MenuItem? = null
    private var logOutMenuItem: MenuItem? = null
    private var profileMenuItem: MenuItem? = null
    private val adapter = LadderAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(layoutInflater).also { binding = it }.root)

        FirebaseApp.initializeApp(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.navView.apply {
            setNavigationItemSelectedListener {
                binding.drawerLayout.closeDrawers()
                onOptionsItemSelected(it)
            }
            menu.findItem(R.id.nav_menu_login)?.let { logInMenuItem = it }
            menu.findItem(R.id.nav_menu_logout)?.let { logOutMenuItem = it }
            menu.findItem(R.id.nav_menu_profile)?.let { profileMenuItem = it }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

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

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            binding.drawerLayout.openDrawer(GravityCompat.START)
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
                startActivity(
                    Intent(this, ProfileActivity::class.java)
                        .putExtra(ProfileActivity.MY_ID_EXTRA, it)
                        .putExtra(ProfileActivity.USER_ID_EXTRA, it)
                )
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                onLoggedIn()
            } else {
                val response = IdpResponse.fromResultIntent(data)
                println("Failed login with error code: ${response?.error?.errorCode}")
            }
        }
    }

    private fun loadLadders() {
        Client.api.getLadders().enqueue(object : Callback<List<Ladder>>(this) {
            override fun onSuccess(data: List<Ladder>) {
                binding.viewSwitcher.displayedChild = VS_LIST_INDEX
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
            NavHeaderBinding.bind(binding.navView.getHeaderView(0)).navHeaderSubtitle.text = text
        }
    }

    private fun onLoggedOut() {
        logInMenuItem?.isVisible = true
        logOutMenuItem?.isVisible = false
        profileMenuItem?.isVisible = false

        val text = getString(R.string.not_logged_in)
        supportActionBar?.subtitle = text
        NavHeaderBinding.bind(binding.navView.getHeaderView(0)).navHeaderSubtitle.text = text
    }

    private inner class LadderAdapter : Adapter<Ladder>(this, R.string.no_ladders_text) {
        override fun createViewHolder(parent: ViewGroup) = LadderViewHolder(CardLadderBinding.inflate(layoutInflater, parent, false))
        override fun bind(viewHolder: RecyclerView.ViewHolder, item: Ladder) {
            (viewHolder as? LadderViewHolder)?.bind(item)
        }

        private inner class LadderViewHolder(private val cardBinding: CardLadderBinding) : RecyclerView.ViewHolder(cardBinding.root) {

            fun bind(ladder: Ladder) {
                cardBinding.nameText.text = ladder.name
                cardBinding.dateText.text = getString(R.string.date_format, DATE_FORMAT.format(ladder.startDate), DATE_FORMAT.format(ladder.endDate))
                itemView.setOnClickListener {
                    startActivity(Intent(this@MainActivity, LadderActivity::class.java).putExtra(LadderActivity.LADDER_EXTRA, ladder))
                }
            }
        }
    }
}
