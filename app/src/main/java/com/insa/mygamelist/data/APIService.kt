package com.insa.mygamelist.data
import androidx.lifecycle.ViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.gson.GsonConverterFactory

private const val BASE_URL = "https://api.igdb.com/v4/"

interface APIService {

    @POST("games")
    suspend fun getGames(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authToken: String,
        @Header("Accept") accept: String,
        @Body body: RequestBody
    ): MutableList<Game>

    @POST("covers")
    suspend fun getCovers(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authToken: String,
        @Header("Accept") accept: String,
        @Body body: RequestBody
    ): MutableList<Cover>

    @POST("platform_logos")
    suspend fun getLogos(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authToken: String,
        @Header("Accept") accept: String,
        @Body body: RequestBody
    ): MutableList<Logo>

    @POST("genres")
    suspend fun getGenres(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authToken: String,
        @Header("Accept") accept: String,
        @Body body: RequestBody
    ): MutableList<Genre>

    @POST("platforms")
    suspend fun getPlatforms(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authToken: String,
        @Header("Accept") accept: String,
        @Body body: RequestBody
    ): MutableList<Platform>

    @POST("games")
    suspend fun addGame(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") authToken: String,
        @Header("Accept") accept: String,
        @Body body: RequestBody
    ): MutableList<Game>

}

object RetrofitInstance {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: APIService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(APIService::class.java)
    }
}

fun extractPlatformIds(games: List<Game>): List<Long> {
    return games.flatMap { it.platforms ?: emptyList() }.distinct()
}


fun extractGenreIds(games: List<Game>): List<Long> {
    return games.flatMap { it.genres?: emptyList() }.distinct()
}

fun extractCoverIds(games: List<Game>): List<Long> {
    return games.map { it.cover!! }
}

fun extractLogoIds(platforms: List<Platform>): List<Long> {
    return platforms.map { it.platform_logo }
}

class IGDBViewModel : ViewModel() {

    private val clientId = "..."
    private val accessToken = "Bearer ..."
    private val accept = "application/json"

    var idCovers: List<Long> = mutableListOf()
    var idGenres: List<Long> = mutableListOf()
    var idLogos: List<Long> = mutableListOf()
    var idPlatforms: List<Long> = mutableListOf()
    var listGames : List<Game> = mutableListOf()
    var listPlatforms : List<Platform> = mutableListOf()

    suspend fun getGames():MutableList<Game>{
        var response : MutableList<Game> = mutableListOf()
         try {
                val requestBody = "fields name, cover, first_release_date, genres, platforms, summary, total_rating;".toRequestBody("text/plain".toMediaTypeOrNull())

                response = RetrofitInstance.apiService.getGames(clientId, accessToken, accept, requestBody)
                listGames=response
            } catch (e: Exception) {
                println("error : ${e.message}")
            }
        return response
    }


    suspend fun getGenres(id: Long? = null): MutableList<Genre> {
        val response: MutableList<Genre> = mutableListOf()
        try {
            val requestBody = if (id == null) {
                idGenres = extractGenreIds(listGames)
                val idString = idGenres.joinToString(",")
                "fields name; where id = ($idString);limit ${idGenres.size};".toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                "fields name; where id = ($id);".toRequestBody("text/plain".toMediaTypeOrNull())
            }

            response.addAll(RetrofitInstance.apiService.getGenres(clientId, accessToken, accept, requestBody))
        } catch (e: Exception) {
            println("Erreur lors de la récupération des genres : ${e.message}")
        }
        return response
    }

    suspend fun getCovers(id: Long? = null): MutableList<Cover> {
        val response: MutableList<Cover> = mutableListOf()
        try {
            val requestBody = if (id == null) {
                idCovers = extractCoverIds(listGames)
                val idString = idCovers.joinToString(",")
                "fields url; where id = ($idString);".toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                "fields url; where id = ($id);".toRequestBody("text/plain".toMediaTypeOrNull())
            }

            response.addAll(RetrofitInstance.apiService.getCovers(clientId, accessToken, accept, requestBody))
        } catch (e: Exception) {
            println("Erreur lors de la récupération des covers : ${e.message}")
        }
        return response
    }

    suspend fun getPlatforms(id: Long? = null): MutableList<Platform> {
        val response: MutableList<Platform> = mutableListOf()
        try {
            val requestBody = if (id == null) {
                idPlatforms = extractPlatformIds(listGames)
                val idString = idPlatforms.joinToString(",")
                "fields name, platform_logo; where id = ($idString);limit ${idPlatforms.size};".toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                "fields name, platform_logo; where id = ($id);".toRequestBody("text/plain".toMediaTypeOrNull())
            }

            response.addAll(RetrofitInstance.apiService.getPlatforms(clientId, accessToken, accept, requestBody))
            listPlatforms=response
        } catch (e: Exception) {
            println("Erreur lors de la récupération des plateformes : ${e.message}")
        }
        return response
    }

    suspend fun getLogos(id: Long? = null): MutableList<Logo> {
        val response: MutableList<Logo> = mutableListOf()
        try {
            val requestBody = if (id == null) {
                idLogos = extractLogoIds(listPlatforms)
                val idString = idLogos.joinToString(",")
                "fields url; where id = ($idString);limit ${idLogos.size};".toRequestBody("text/plain".toMediaTypeOrNull())
            } else {
                "fields url; where id = ($id);".toRequestBody("text/plain".toMediaTypeOrNull())
            }

            response.addAll(RetrofitInstance.apiService.getLogos(clientId, accessToken, accept, requestBody))
        } catch (e: Exception) {
            println("Erreur lors de la récupération des logos : ${e.message}")
        }
        return response
    }

    suspend fun addGame(name: String): MutableList<Game> {
        val response = mutableListOf<Game>()
        try {
            val requestBody = "fields name, cover, first_release_date, genres, platforms, summary, total_rating; search \"$name\"; limit 1;"
                .toRequestBody("text/plain".toMediaTypeOrNull())
            val apiResponse = RetrofitInstance.apiService.addGame(clientId, accessToken, accept, requestBody)

            if (apiResponse.isNotEmpty()) {
                response.addAll(apiResponse)
            } else {
                println("API returned an empty response for addGame")
            }
        } catch (e: Exception) {
            println("Error in addGame: ${e.message}")
        }
        return response
    }



}
