package com.romrell4.tennisladder.model

import java.util.*

data class Match(
    val matchId: Int,
    val ladderId: Int,
    val matchDate: Date,
    val winner: Player,
    val loser: Player,
    val winnerSet1Score: Int,
    val loserSet1Score: Int,
    val winnerSet2Score: Int,
    val loserSet2Score: Int,
    val winnerSet3Score: Int? = null,
    val loserSet3Score: Int? = null
) {
    val scoreText = "$winnerSet1Score-$loserSet1Score, $winnerSet2Score-$loserSet2Score${if (winnerSet3Score != null && loserSet3Score != null) "$winnerSet3Score-$loserSet3Score" else ""}"
}