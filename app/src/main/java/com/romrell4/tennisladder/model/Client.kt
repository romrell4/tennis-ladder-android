package com.romrell4.tennisladder.model

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST

class Client {
	companion object {
		val api: Api
			get() {
				return Retrofit.Builder()
					.baseUrl("https://lxlwvoenil.execute-api.us-west-2.amazonaws.com/prod/")
					.addConverterFactory(
						GsonConverterFactory.create(
							GsonBuilder()
								.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
								.setDateFormat("yyyy-MM-dd HH:mm:ss")
								.create()
						)
					)
					.client(OkHttpClient.Builder()
						.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
						.addInterceptor { chain ->
							chain.proceed(chain.request().newBuilder().apply { FirebaseAuth.getInstance().currentUser?.let { addHeader("Authorization", it.uid) } }.build())
						}
						.build()
					)
					.build()
					.create(Client.Api::class.java)
			}
	}

	interface Api {
		@POST("users")
		fun login(): Call<Unit>

		@GET("ladders")
		fun getLadders(): Call<List<Ladder>>
	}
}