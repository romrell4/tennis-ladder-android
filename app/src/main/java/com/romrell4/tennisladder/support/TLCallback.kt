package com.romrell4.tennisladder.support

import android.content.Context
import android.widget.Toast
import com.romrell4.tennisladder.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class SuccessCallback<T>(private val context: Context): Callback<T> {
	abstract fun onSuccess(data: T)

	override fun onResponse(call: Call<T>, response: Response<T>) {
		if (response.isSuccessful) {
			response.body()?.let {
				onSuccess(it)
			} ?: onFailure(call, Throwable("No body found in the message"))
		} else {
			onFailure(call, Throwable(response.errorBody()?.string()))
		}
	}

	override fun onFailure(call: Call<T>, t: Throwable) {
		Toast.makeText(context, context.getString(R.string.error_message, t.message), Toast.LENGTH_LONG).show()
	}
}