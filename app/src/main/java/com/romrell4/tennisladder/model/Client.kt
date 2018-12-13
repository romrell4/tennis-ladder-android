package com.romrell4.tennisladder.model

import com.google.android.gms.tasks.Tasks
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
import retrofit2.http.Path

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
							val requestBuilder = chain.request().newBuilder()

							//If the user is logged in, attach a token to the request
							FirebaseAuth.getInstance().currentUser?.let {
								Tasks.await(it.getIdToken(true)).token?.let {
									//This line will allow you to view and copy the token, for debug purposes
									println(it)
									chain.proceed(requestBuilder.addHeader("X-Firebase-Token", it).build())
								} ?: throw Exception("Unable to retrieve token")
							} ?: run {
								//If the user isn't logged in, let them call the public endpoints
								chain.proceed(requestBuilder.build())
							}
						}
						.build()
					)
					.build()
					.create(Client.Api::class.java)
			}
	}

	interface Api {
		@GET("ladders")
		fun getLadders(): Call<List<Ladder>>

		@GET("ladders/{ladder_id}/players")
		fun getPlayers(@Path("ladder_id") ladderId: Int): Call<List<Player>>

		@GET("ladders/{ladder_id}/players/{user_id}/matches")
		fun getMatches(@Path("ladder_id") ladderId: Int, @Path("user_id") userId: String): Call<List<Match>>
	}
}