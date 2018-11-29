package com.romrell4.tennisladder.controller

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Ladder
import kotlinx.android.synthetic.main.activity_ladders.*

class LaddersActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ladders)

        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.adapter = LaddersAdapter(listOf(
            Ladder(1, "Rebecca's Ladder"),
            Ladder(2, "Eric's Ladder"),
            Ladder(3, "Cole's Ladder")
        ))
    }

    private inner class LaddersAdapter(var ladders: List<Ladder>): RecyclerView.Adapter<LaddersAdapter.LadderViewHolder>() {
        override fun getItemCount() = ladders.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LadderViewHolder(layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false))
        override fun onBindViewHolder(holder: LadderViewHolder, position: Int) {
            holder.bind(ladders[position])
        }

        private inner class LadderViewHolder(view: View): RecyclerView.ViewHolder(view) {
            private val textView: TextView = view.findViewById(android.R.id.text1)

            fun bind(ladder: Ladder) {
                textView.text = ladder.name
                itemView.setOnClickListener {
                    startActivity(Intent(this@LaddersActivity, LadderActivity::class.java).putExtra("LADDER", ladder))
                }
            }
        }
    }
}
