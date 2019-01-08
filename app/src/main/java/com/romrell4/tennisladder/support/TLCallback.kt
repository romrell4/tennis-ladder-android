package com.romrell4.tennisladder.support

import android.content.Context
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.romrell4.tennisladder.R
import com.romrell4.tennisladder.model.Client
import com.romrell4.tennisladder.model.ServerError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class Callback<T>(private val context: Context): Callback<T> {
	abstract fun onSuccess(data: T)
	open fun onError(error: ServerError?, t: Throwable) {
		Toast.makeText(context, error?.error ?: context.getString(R.string.error_message, t.message), Toast.LENGTH_LONG).show()
	}

	override fun onResponse(call: Call<T>, response: Response<T>) {
		if (response.isSuccessful) {
			response.body()?.let {
				onSuccess(it)
			} ?: onFailure(call, Throwable("No body found in the message"))
		} else {
			val body = response.errorBody()?.string()
			val error = Client.gson.fromJson<ServerError>(body)
			onError(error, Throwable(body))
		}
	}

	override fun onFailure(call: Call<T>, t: Throwable) {
		onError(null, t)
	}

	private inline fun <reified T> Gson.fromJson(json: String?): T = this.fromJson<T>(json, object: TypeToken<T>() {}.type)
}