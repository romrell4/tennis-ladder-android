package com.romrell4.tennisladder.model

data class Match(
    val matchId: Int,
    val ladderId: Int,
    val winnerId: Int,
    val loserId: Int,
    val winnerSet1Score: Int,
    val loserSet1Score: Int,
    val winnerSet2Score: Int,
    val loserSet2Score: Int,
    val winnerSet3Score: Int?,
    val loserSet3Score: Int?
)