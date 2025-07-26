package com.romrell4.tennisladder.controller

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.lifecycle.lifecycleScope
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
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.support.Adapter
import com.romrell4.tennisladder.support.TLActivity
import com.romrell4.tennisladder.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val DATE_FORMAT = SimpleDateFormat("M/d/yyyy", Locale.US)

class MainActivity : TLActivity() {
    private lateinit var binding: ActivityMainBinding

    private val adapter = LadderAdapter()
    private val viewModel: MainViewModel by viewModels()

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
            // Programmatically set the inset padding of the header view to match the system insets
            ViewCompat.setOnApplyWindowInsetsListener(getHeaderView(0)) { v, insets ->
                val statusBarHeight = insets.getInsets(Type.statusBars()).top
                v.setPadding(v.paddingLeft, statusBarHeight, v.paddingRight, v.paddingBottom)
                WindowInsetsCompat.CONSUMED
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.loadLadders() }

        // Collect flows from the ViewModel
        lifecycleScope.launch {
            viewModel.viewState.collect { viewState ->
                binding.viewSwitcher.displayedChild = viewState.viewSwitcherIndex
                binding.swipeRefreshLayout.isRefreshing = viewState.swipeLoadingDisplayed
                adapter.list = viewState.ladders
            }
        }
        lifecycleScope.launch {
            viewModel.commandFlow.collect {
                when (it) {
                    is MainViewModel.Command.ShowToast -> {
                        Toast.makeText(this@MainActivity, it.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
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
        R.id.rules -> {
            val webView = WebView(this)
            webView.loadUrl("https://romrell4.github.io/tennis-ladder-ws/rules.html")

            AlertDialog.Builder(this)
                .setView(webView)
                .setNeutralButton(android.R.string.ok, null)
                .show()
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

    private fun onLoggedIn() {
        binding.root.post {
            binding.navView.menu.run {
                findItem(R.id.nav_menu_login).isVisible = false
                findItem(R.id.nav_menu_logout).isVisible = true
                findItem(R.id.nav_menu_profile).isVisible = true
            }
        }

        FirebaseAuth.getInstance().currentUser?.displayName.let {
            val text = getString(R.string.logged_in_text, it)
            supportActionBar?.subtitle = text
            NavHeaderBinding.bind(binding.navView.getHeaderView(0)).navHeaderSubtitle.text = text
        }
    }

    private fun onLoggedOut() {
        binding.root.post {
            binding.navView.menu.run {
                findItem(R.id.nav_menu_login).isVisible = true
                findItem(R.id.nav_menu_logout).isVisible = false
                findItem(R.id.nav_menu_profile).isVisible = false
            }
        }

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
