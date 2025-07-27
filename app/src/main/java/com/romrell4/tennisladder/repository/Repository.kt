package com.romrell4.tennisladder.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.model.Match
import com.romrell4.tennisladder.model.Player
import com.romrell4.tennisladder.model.ServerError
import com.romrell4.tennisladder.model.User
import retrofit2.HttpException

class Repository {
    private inline fun <T> execute(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        if (e is HttpException) {
            val body = e.response()?.errorBody()?.string()
            Result.failure(
                ServiceException(
                    error = Client.gson.fromJson<ServerError>(body),
                    throwable = e
                )
            )
        } else Result.failure(e)
    }

    suspend fun getLadders(): Result<List<Ladder>> = execute { Client.api.getLadders() }

    suspend fun getPlayers(ladderId: Int): Result<List<Player>> =
        execute { Client.api.getPlayers(ladderId) }

    suspend fun updatePlayerOrder(
        ladderId: Int,
        generateBorrowedPoints: Boolean,
        players: List<Player>
    ): Result<List<Player>> = execute {
        Client.api.updatePlayerOrder(ladderId, generateBorrowedPoints, players)
    }

    suspend fun addPlayerToLadder(ladderId: Int, code: String): Result<List<Player>> = execute {
        Client.api.addPlayerToLadder(ladderId, code)
    }

    suspend fun updatePlayer(ladderId: Int, userId: String, player: Player): Result<List<Player>> =
        execute {
            Client.api.updatePlayer(ladderId, userId, player)
        }

    suspend fun reportMatch(ladderId: Int, match: Match): Result<Match> = execute {
        Client.api.reportMatch(ladderId, match)
    }

    suspend fun getUser(userId: String): Result<User> = execute {
        Client.api.getUser(userId)
    }

    suspend fun updateUser(userId: String, user: User): Result<User> = execute {
        Client.api.updateUser(userId, user)
    }

    private inline fun <reified T> Gson.fromJson(json: String?): T =
        this.fromJson(json, object : TypeToken<T>() {}.type)
}

data class ServiceException(
    val error: ServerError?,
    val throwable: Throwable
) : Exception()