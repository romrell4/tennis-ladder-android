package com.romrell4.tennisladder.support

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat.Type
import com.firebase.ui.auth.AuthUI
import com.romrell4.tennisladder.BuildConfig
import com.romrell4.tennisladder.R

@SuppressLint("Registered")
open class TLActivity: AppCompatActivity() {
	companion object {
		const val RC_SIGN_IN = 200
	}
	override fun setTitle(title: CharSequence?) {
		supportActionBar?.title = title
	}

	override fun setContentView(view: View) {
		setSupportActionBar(view.findViewById(R.id.toolbar))
		super.setContentView(view)

		view.findViewById<View>(R.id.status_bar_background)?.let { statusBarBackground ->
			// Programmatically set the height of the status bar background to match the system insets
			ViewCompat.setOnApplyWindowInsetsListener(statusBarBackground) { v, insets ->
				val statusBarHeight = insets.getInsets(Type.statusBars()).top
				v.layoutParams = ViewGroup.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					statusBarHeight
				)
				insets
			}
		}

		ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
			// Apply the padding to account for the navigation bars
			v.setPadding(
				0, 0, 0, insets.getInsets(Type.navigationBars()).bottom
			)
			insets
		}
	}

	fun startLoginActivity() {
		startActivityForResult(
			AuthUI.getInstance().createSignInIntentBuilder()
				.setLogo(R.drawable.ic_tennis_ladder)
				.setIsSmartLockEnabled(!BuildConfig.DEBUG)
				.setTheme(R.style.AppTheme)
				.setAvailableProviders(listOf(
					AuthUI.IdpConfig.GoogleBuilder(),
					AuthUI.IdpConfig.EmailBuilder()
				).map { it.build() }).build(), RC_SIGN_IN
		)
	}
}
