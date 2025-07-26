package com.romrell4.tennisladder.model

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

class Client {
	companion object {
		val gson: Gson = GsonBuilder()
			.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
			.setDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
			.create()
		val api: Api
			get() {
				return Retrofit.Builder()
					.baseUrl("https://lxlwvoenil.execute-api.us-west-2.amazonaws.com/prod/")
					.addConverterFactory(
						GsonConverterFactory.create(gson)
					)
					.client(OkHttpClient.Builder()
						.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
						.addInterceptor { chain ->
							val requestBuilder = chain.request().newBuilder()

							//If the user is logged in, attach a token to the request
							FirebaseAuth.getInstance().currentUser?.let { user ->
								Tasks.await(user.getIdToken(true)).token?.let {
									//This line will allow you to view and copy the token, for debug purposes
//									println(it)
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
					.create(Api::class.java)
			}
	}

	interface Api {
		@GET("users/{user_id}")
		fun getUser(@Path("user_id") userId: String): Call<User>

		@PUT("users/{user_id}")
		fun updateUser(@Path("user_id") userId: String, @Body user: User): Call<User>

		@GET("ladders")
		suspend fun getLadders(): List<Ladder>

		@GET("ladders/{ladder_id}/players")
		fun getPlayers(@Path("ladder_id") ladderId: Int): Call<List<Player>>

		@POST("ladders/{ladder_id}/players")
		fun addPlayerToLadder(@Path("ladder_id") ladderId: Int, @Query("code") code: String): Call<List<Player>>

		@PUT("ladders/{ladder_id}/players")
		fun updatePlayerOrder(@Path("ladder_id") ladderId: Int, @Query("generate_borrowed_points") generateBorrowedPoints: Boolean, @Body players: List<Player>): Call<List<Player>>

		@PUT("ladders/{ladder_id}/players/{user_id}")
		fun updatePlayer(@Path("ladder_id") ladderId: Int, @Path("user_id") userId: String, @Body player: Player): Call<List<Player>>

		@GET("ladders/{ladder_id}/players/{user_id}/matches")
		fun getMatches(@Path("ladder_id") ladderId: Int, @Path("user_id") userId: String): Call<List<Match>>

		@POST("ladders/{ladder_id}/matches")
		fun reportMatch(@Path("ladder_id") ladderId: Int, @Body match: Match): Call<Match>

		@PUT("ladders/{ladder_id}/matches/{match_id}")
		fun updateMatchScores(@Path("ladder_id") ladderId: Int, @Path("match_id") matchId: Int, @Body match: Match): Call<Match>

		@DELETE("ladders/{ladder_id}/matches/{match_id}")
		fun deleteMatch(@Path("ladder_id") ladderId: Int, @Path("match_id") matchId: Int): Call<ResponseBody>
	}
}
