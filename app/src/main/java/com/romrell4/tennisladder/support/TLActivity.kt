package com.romrell4.tennisladder.support

import android.annotation.SuppressLint
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.romrell4.tennisladder.BuildConfig
import com.romrell4.tennisladder.R
import kotlinx.android.synthetic.main.toolbar.view.*

@SuppressLint("Registered")
open class TLActivity: AppCompatActivity() {
	companion object {
		const val RC_SIGN_IN = 200
	}
	override fun setTitle(title: CharSequence?) {
		supportActionBar?.title = title
	}

	override fun setContentView(@LayoutRes layoutResID: Int) {
		val view = layoutInflater.inflate(layoutResID, null)
		setSupportActionBar(view.toolbar)
		super.setContentView(view)
	}

	fun startLoginActivity() {
		startActivityForResult(
			AuthUI.getInstance().createSignInIntentBuilder()
				.setLogo(R.drawable.ic_tennis_ladder)
				.setIsSmartLockEnabled(!BuildConfig.DEBUG)
				.setAvailableProviders(listOf(
					AuthUI.IdpConfig.GoogleBuilder(),
					AuthUI.IdpConfig.EmailBuilder()
				).map { it.build() }).build(), RC_SIGN_IN
		)
	}
}