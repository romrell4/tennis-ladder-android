package com.romrell4.tennisladder.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.Ladder
import com.romrell4.tennisladder.model.ServerError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

class LadderRepository {
    suspend fun getLadders(): Result<List<Ladder>> = try {
        val ladders = Client.api.getLadders()
        Result.success(ladders)
    } catch (e: Exception) {
        if (e is HttpException) {
            val body = e.response()?.errorBody()?.string()
            Result.failure(ServiceException(
                error = Client.gson.fromJson<ServerError>(body),
                throwable = e
            ))
        } else Result.failure(e)
    }

    private inline fun <reified T> Gson.fromJson(json: String?): T = this.fromJson(json, object: TypeToken<T>() {}.type)
}

data class ServiceException(
    val error: ServerError?,
    val throwable: Throwable
): Exception()