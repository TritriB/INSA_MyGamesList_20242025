package com.insa.mygamelist.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope

interface DataLoadedCallback {
    fun onDataLoaded()
}


object IGDB {

    lateinit var covers: MutableList<Cover>
    lateinit var games: MutableList<Game>
    lateinit var genres: MutableList<Genre>
    lateinit var logos: MutableList<Logo>
    lateinit var platforms: MutableList<Platform>
    val api = IGDBViewModel()
    var list_favoris: MutableList<Game> = mutableListOf()

    fun saveFavorites(context: Context, list: List<Game>) {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val gson = Gson()
        val json = gson.toJson(list)

        editor.putString("list_favoris", json)
        editor.apply()
    }

    fun loadFavorites(context: Context): MutableList<Game> {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("list_favoris", null) ?: return mutableListOf()

        val gson = Gson()
        val type = object : TypeToken<MutableList<Game>>() {}.type
        return gson.fromJson(json, type) ?: mutableListOf()

    }


    fun favoriteGame(game: Game):Boolean{
        return game in list_favoris
    }

    fun findCoverById(covers: List<Cover>, id_: Long): Cover? {
        for(cover in covers)
            if(cover.id == id_)
                return cover
        return null
    }

    fun findGameById(games: List<Game>, id_: Long): Game? {
        for(game in games)
            if(game.id == id_)
                return game
        return null
    }

    fun findGenreById(genres: List<Genre>, id_: Long): Genre? {
        for(genre in genres)
            if(genre.id == id_)
                return genre
        return null
    }

    fun findPlatformsById(id:Long):List<Logo>?{
        val platList : MutableList<Logo> = mutableListOf()
        val game = findGameById(IGDB.games,id)
        val ids = game?.platforms
        if (ids!=null) {
            for (id_plat in ids) {
                for (plat in IGDB.platforms){
                    if(id_plat == plat.id){
                        for(logo in IGDB.logos){
                            if(plat.platform_logo==logo.id){
                                platList.add(logo)
                            }

                        }
                    }
                }
            }
        }
        return platList
    }

    suspend fun getNewGame(name: String) {
        val list = api.addGame(name)
        if(list.isEmpty()){
            print("erreur lors du chargement de votre nouveau jeu")
            return
        }
        else{
            val newGame = list[0]
            games.add(newGame)

            newGame.genres?.forEach { id ->
                val genreId = id.toInt()
                if (genres.none { it.id.toInt() == genreId }) {
                    val newGenres = api.getGenres(id)
                    genres.addAll(newGenres)
                }
            }

            newGame.cover?.let { coverId ->
                if (covers.none { it.id == coverId }) {
                    val newCovers = api.getCovers(coverId)
                    covers.addAll(newCovers)
                }
            }

            newGame.platforms?.forEach { id ->
                val platformId = id.toInt()
                if (platforms.none { it.id.toInt() == platformId }) {
                    val newPlatforms = api.getPlatforms(id)
                    platforms.addAll(newPlatforms)

                    newPlatforms.forEach { platform ->
                        if (logos.none { it.id == platform.platform_logo }) {
                            val newLogos = api.getLogos(platform.platform_logo)
                            logos.addAll(newLogos)
                        }
                    }
                }
            }
        }
    }



    suspend fun load(context: CoroutineScope, callback: DataLoadedCallback) {
        games = api.getGames()
        covers = api.getCovers()
        genres = api.getGenres()
        platforms = api.getPlatforms()
        logos = api.getLogos()
        callback.onDataLoaded()
    }


}

data class Cover(@SerializedName("id") val id: Long, @SerializedName("url") val url: String)
data class Game(
    @SerializedName("id") val id: Long,
    @SerializedName("cover") val cover: Long?,
    @SerializedName("first_release_date") val releaseDate: Long?,
    @SerializedName("genres") val genres: List<Long>?,
    @SerializedName("name") val name: String,
    @SerializedName("platforms") val platforms: List<Long>?,
    @SerializedName("summary") val summary:String,
    @SerializedName("total_rating") val totalRating: Float?
){
    fun findGenresByGame(): List<String> {
        return genres?.mapNotNull { genreId ->
            IGDB.genres.find { it.id == genreId }?.name
        } ?: emptyList()
    }

    fun findPlatformsByGame(): List<String> {
        return platforms?.mapNotNull { platformId ->
            IGDB.platforms.find { it.id == platformId }?.name
        } ?: emptyList()
    }

    override fun toString(): String {
        return "Game(name='$name', " +
                "genres=${genres?.joinToString(", ") ?: "Unknown"}, " +
                "platforms=${platforms?.joinToString(", ") ?: "Unknown"}, " +
                "rating=${totalRating ?: "N/A"})"
    }


}

data class Genre(@SerializedName("id")val id: Long,@SerializedName("name") val name: String)
data class Logo(@SerializedName("id")val id: Long,@SerializedName("url") val url: String)
data class Platform(@SerializedName("id")val id: Long,@SerializedName("name") val name: String,@SerializedName("platform_logo") val platform_logo: Long)