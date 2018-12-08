package com.romrell4.tennisladder.controller

import android.os.Bundle
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.support.TLActivity
import kotlinx.android.synthetic.main.activity_player.*

class PlayerActivity: TLActivity() {
    companion object {
        const val PLAYER_EXTRA = "player"
    }

    private lateinit var player: Player

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        player = intent.getParcelableExtra(PLAYER_EXTRA)
        title = player.name
        ranking_text.text = getString(R.string.ranking_text_format, player.ranking)
        record_text.text = getString(R.string.record_text_format, player.wins, player.losses)

        report_match_button.setOnClickListener {
            //TODO: Create alert or activity to report the scores
        }
    }
}
