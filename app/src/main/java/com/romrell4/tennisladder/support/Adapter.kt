package com.romrell4.tennisladder.support

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.romrell4.tennisladder.databinding.CardEmptyTextBinding
import kotlin.math.max

abstract class Adapter<T>(private val activity: AppCompatActivity, @StringRes private val emptyTextRes: Int): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
	var list: List<T> = emptyList()
		set(value) {
			field = value
			notifyDataSetChanged()
		}

	override fun getItemCount() = max(list.size, 1)

	abstract fun createViewHolder(parent: ViewGroup): RecyclerView.ViewHolder
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
		if (list.isEmpty()) EmptyViewHolder(CardEmptyTextBinding.inflate(activity.layoutInflater, parent, false))
		else createViewHolder(parent)

	abstract fun bind(viewHolder: RecyclerView.ViewHolder, item: T)
	override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
		if (viewHolder is Adapter<*>.EmptyViewHolder) {
			viewHolder.cardBinding.title.text = activity.getString(emptyTextRes)
		} else {
			bind(viewHolder, list[position])
		}
	}

	private inner class EmptyViewHolder(val cardBinding: CardEmptyTextBinding): RecyclerView.ViewHolder(cardBinding.root)
}
