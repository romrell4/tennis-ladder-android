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
	var winnerSet3Score: Int? = null,
	var loserSet3Score: Int? = null
) {
	fun scoreText(player: Player): String {
		if (player == winner) {
			return "$winnerSet1Score-$loserSet1Score, $winnerSet2Score-$loserSet2Score${if (winnerSet3Score != null || loserSet3Score != null) ", $winnerSet3Score-$loserSet3Score" else ""}"
		} else {
			return "$loserSet1Score-$winnerSet1Score, $loserSet2Score-$winnerSet2Score${if (winnerSet3Score != null || loserSet3Score != null) ", $loserSet3Score-$winnerSet3Score" else ""}"
		}
	}
}