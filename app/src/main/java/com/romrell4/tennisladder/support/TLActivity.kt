package com.romrell4.tennisladder.support

import android.annotation.SuppressLint
import android.support.annotation.LayoutRes
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.toolbar.view.*

@SuppressLint("Registered")
open class TLActivity: AppCompatActivity() {
	override fun setTitle(title: CharSequence?) {
		supportActionBar?.title = title
	}

	override fun setContentView(@LayoutRes layoutResID: Int) {
		val view = layoutInflater.inflate(layoutResID, null)
		setSupportActionBar(view.toolbar)
		super.setContentView(view)
	}
}