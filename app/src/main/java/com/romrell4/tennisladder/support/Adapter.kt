package com.romrell4.tennisladder.support

import android.support.annotation.StringRes
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.romrell4.tennisladder.R
import kotlinx.android.synthetic.main.card_empty_text.view.*
import kotlin.math.max

abstract class Adapter<T>(private val activity: AppCompatActivity, @StringRes private val emptyTextRes: Int): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	var list: List<T> = emptyList()

	override fun getItemCount() = max(list.size, 1)

	abstract fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		if (list.isEmpty()) EmptyViewHolder(activity.layoutInflater.inflate(R.layout.card_empty_text, parent, false))
		else createViewHolder(parent)

	abstract fun bind(viewHolder: RecyclerView.ViewHolder, item: T)
	override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
		if (viewHolder is Adapter<*>.EmptyViewHolder) {
			viewHolder.title.text = activity.getString(emptyTextRes)
		} else {
			bind(viewHolder, list[position])
		}
	}

	private inner class EmptyViewHolder(view: View): RecyclerView.ViewHolder(view) {
		val title: TextView = view.title
	}
}