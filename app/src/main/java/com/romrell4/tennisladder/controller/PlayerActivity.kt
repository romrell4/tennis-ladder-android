package com.romrell4.tennisladder.controller

import android.os.Bundle
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.support.TLActivity

class PlayerActivity: TLActivity() {
    companion object {
        const val PLAYER_EXTRA = "player"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
    }
}
