package com.romrell4.tennisladder.model

import java.util.*

data class Match(
	val matchId: Int = 0,
	val ladderId: Int,
	val matchDate: Date = Date(),
	val winner: Player,
	val loser: Player,
	val winnerSet1Score: Int,
	val loserSet1Score: Int,
	val winnerSet2Score: Int,
	val loserSet2Score: Int,
	val winnerSet3Score: Int? = null,
	val loserSet3Score: Int? = null,
	val winnerPoints: Int = 0,
	val loserPoints: Int = 0,
) {
	fun scoreText(player: Player) = if (player == winner) {
		"$winnerSet1Score-$loserSet1Score, $winnerSet2Score-$loserSet2Score${if (winnerSet3Score != null || loserSet3Score != null) ", $winnerSet3Score-$loserSet3Score" else ""}"
	} else {
		"$loserSet1Score-$winnerSet1Score, $loserSet2Score-$winnerSet2Score${if (winnerSet3Score != null || loserSet3Score != null) ", $loserSet3Score-$winnerSet3Score" else ""}"
	}

	fun earnedPoints(player: Player) = if (player == winner) winnerPoints else loserPoints
}