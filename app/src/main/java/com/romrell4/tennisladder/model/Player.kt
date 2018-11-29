package com.romrell4.tennisladder.model

data class Player(
    val userId: Int,
    val ladderId: Int,
    val name: String,
    val score: Int
)