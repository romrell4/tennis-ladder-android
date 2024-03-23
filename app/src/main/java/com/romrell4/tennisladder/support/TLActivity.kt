package com.romrell4.tennisladder.support

import android.annotation.SuppressLint
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
