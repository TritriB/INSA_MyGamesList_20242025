package com.insa.mygamelist
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.serialization.Serializable
import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.compose.AsyncImage
import com.insa.mygamelist.data.DataLoadedCallback
import com.insa.mygamelist.data.Game
import com.insa.mygamelist.data.IGDB
import com.insa.mygamelist.ui.theme.MyGamesListTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainViewModel = MainViewModel()

        mainViewModel.loadGames(this)

        createNotificationChannel(this)

        enableEdgeToEdge()

        setContent {
            MyGamesListTheme {
                MyApp(viewModel = mainViewModel)
            }
        }
    }
}

class MainViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _games = MutableStateFlow<List<Game>>(emptyList())
    val games = _games.asStateFlow()

    private val _isLoadingNewGame = MutableStateFlow(false)
    val isLoadingNewGame: StateFlow<Boolean> = _isLoadingNewGame

    fun searchNewGame(query: String) {
        viewModelScope.launch {
            _isLoadingNewGame.value = true
            IGDB.getNewGame(query)
            _isLoadingNewGame.value = false
            _games.value = IGDB.games
        }
    }

    fun loadGames(context: Context) {
        viewModelScope.launch {
            IGDB.load(object : DataLoadedCallback {
                override fun onDataLoaded() {
                    _isLoading.value = false
                    _games.value = IGDB.games
                    IGDB.list_favoris = IGDB.loadFavorites(context)
                }
            })
        }
    }

    fun filter_croissant() {
        _games.value = _games.value.sortedBy { it.name.lowercase() }
    }

    fun filter_decroissant() {
        _games.value = _games.value.sortedByDescending { it.name.lowercase() }
    }
}


@Composable
fun MyApp(viewModel: MainViewModel){
    val isLoading by viewModel.isLoading.collectAsState()

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No content", fontSize = 24.sp, color = Color.Gray)
        }
    } else {

        val navController = rememberNavController()

        NavHost(navController, startDestination = Home) {
            composable<Home> {
                HomeScreen(navController,viewModel)
            }
            composable<GameDetails> { backStackEntry ->
                val game: GameDetails = backStackEntry.toRoute()
                GameDetailsScreen(game.id, navController)
            }
        }
    }
}

@Composable
fun GameCellule(id: Long, onNavigateToGameDetails:(Long)->Unit) {
    val context = LocalContext.current
    val game = IGDB.findGameById(IGDB.games, id)
    val coverUrl = game?.let { IGDB.findCoverById(IGDB.covers, it.cover!!)?.url }
    val genres = game?.genres?.mapNotNull { IGDB.findGenreById(IGDB.genres, it)?.name } ?: emptyList()
    var model by rememberSaveable { mutableIntStateOf(R.raw.no_favori) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black)
            .background(Color.LightGray)
            .padding(0.dp)
            .clickable { onNavigateToGameDetails(id) }
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "https:$coverUrl",
                contentDescription = "logo",
                modifier = Modifier
                    .height(100.dp)
                    .aspectRatio(1f)
            )
            Column(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                game?.let { Text(it.name, fontSize = 24.sp, fontWeight = FontWeight.Bold) }

                val ratingText = game?.totalRating?.let { String.format("%.1f", it) } ?: "N/A"
                Text(
                    text = "Note: $ratingText",
                    fontSize = 18.sp,
                    fontStyle = FontStyle.Italic,
                    color = Color.Magenta
                )

                Text(
                    text = "Genres: " + genres.joinToString(", "),
                    fontSize = 18.sp
                )
            }

            if(game?.let { IGDB.favoriteGame(it) } == true){
                model=R.raw.favori
            }
            else{
                model=R.raw.no_favori
            }
            AsyncImage(
                model = model,
                contentDescription = "",
                modifier = Modifier
                    .clickable(onClick = {
                        if (game?.let { IGDB.favoriteGame(it) } == true) {
                            model = R.raw.no_favori
                            sendNotification(
                                context,
                                "${game.name}",
                                "${game.name} a été supprimé des favoris."
                            )
                            IGDB.list_favoris.remove(game)
                            IGDB.saveFavorites(context, IGDB.list_favoris)
                        } else {
                            model = R.raw.favori
                            if (game != null) {
                                sendNotification(
                                    context,
                                    "${game.name}",
                                    "${game.name} a été ajouté aux favoris."
                                )
                                IGDB.list_favoris.add(game)
                                IGDB.saveFavorites(context, IGDB.list_favoris)

                            }
                        }
                    })
                    .size(30.dp)
            )
        }
    }
}

@Composable
fun SearchBar(
    query : String,
    onQueryChange : (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        placeholder = {
            Text(stringResource(R.string.app_name))
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    )
}

@Serializable
object Home

@Serializable
data class GameDetails(val id: Long)

@SuppressLint("ResourceType")
@Composable
fun FilterButton(viewModel: MainViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Image(
            bitmap = ImageBitmap.imageResource(R.raw.filter),
            contentDescription = "Filter",
            modifier = Modifier.clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            DropdownMenuItem(
                text = { Text("Ordre Croissant") },
                onClick = {
                    viewModel.filter_croissant()
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Ordre Décroissant") },
                onClick = {
                    viewModel.filter_decroissant()
                    expanded = false
                }
            )
        }
    }
}

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Channel Name"
        val descriptionText = "Channel Description"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("MY_CHANNEL_ID", name, importance).apply {
            description = descriptionText
        }

        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun sendNotification(context: Context, textTitle: String, textContent: String) {
    val CHANNEL_ID = "MY_CHANNEL_ID"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "MyChannel"
        val descriptionText = "Description du canal de notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }


    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.notification)
        .setContentTitle(textTitle)
        .setContentText(textContent)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)

    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) {
        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
        return
    }

    with(NotificationManagerCompat.from(context)) {
        notify(1, builder.build())
    }
}


@SuppressLint("ResourceType")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: MainViewModel) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val selectedGenres = rememberSaveable { mutableStateOf(setOf<String>()) }
    val selectedPlatforms = rememberSaveable { mutableStateOf(setOf<String>()) }
    val games by viewModel.games.collectAsState()
    val isLoadingNewGame by viewModel.isLoadingNewGame.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val allGenres = IGDB.genres.map { it.name }
    val allPlatforms = IGDB.platforms.map { it.name }

    val filteredGames = games.filter { game ->
        val query = searchQuery.lowercase().trim()
        val gameGenres = game.findGenresByGame()
        val gamePlatforms = game.findPlatformsByGame()

        val matchesGenre = selectedGenres.value.isEmpty() || selectedGenres.value.all { it in gameGenres }
        val matchesPlatform = selectedPlatforms.value.isEmpty() || selectedPlatforms.value.all { it in gamePlatforms }
        val matchesSearch = game.name.lowercase().contains(query) || gameGenres.any { it.lowercase().contains(query) } || gamePlatforms.any { it.lowercase().contains(query) }

        matchesGenre && matchesPlatform && matchesSearch
    }

    LaunchedEffect(games) {
        println("mise à jour ...")
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    colors = topAppBarColors(containerColor = Color.Magenta, titleContentColor = Color.Black),
                    title = { SearchBar(query = searchQuery, onQueryChange = { searchQuery = it }) },
                    actions = {
                        FilterButton(viewModel)
                    }
                )

                LazyRow {
                    items(allGenres) { genre ->
                        FilterChip(
                            selected = genre in selectedGenres.value,
                            onClick = {
                                selectedGenres.value = selectedGenres.value.toMutableSet().apply {
                                    if (contains(genre)) remove(genre) else add(genre)
                                }
                            },
                            label = { Text(genre, fontSize = 14.sp) },
                            modifier = Modifier.padding(horizontal = 1.dp, vertical = 0.5.dp)
                        )
                    }
                    items(allPlatforms) { platform ->
                        FilterChip(
                            selected = platform in selectedPlatforms.value,
                            onClick = {
                                selectedPlatforms.value = selectedPlatforms.value.toMutableSet().apply {
                                    if (contains(platform)) remove(platform) else add(platform)
                                }
                            },
                            label = { Text(platform, fontSize = 14.sp) },
                            modifier = Modifier.padding(horizontal = 1.dp, vertical = 0.5.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            LazyColumn {
                if (filteredGames.isEmpty()) {
                    item {
                        if (isLoadingNewGame) {
                            Image(
                                bitmap = ImageBitmap.imageResource(R.raw.loading),
                                contentDescription = "Chargement...",
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Image(
                                bitmap = ImageBitmap.imageResource(R.raw.nomatch),
                                contentDescription = "No match found",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        coroutineScope.launch {
                                            viewModel.searchNewGame(searchQuery)
                                        }
                                    }
                            )
                        }
                    }
                } else {
                    items(filteredGames) { game ->
                        GameCellule(game.id) { id ->
                            navController.navigate(GameDetails(id))
                        }
                    }
                }
            }
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailsScreen(id : Long,navController: NavController) {
    val context = LocalContext.current
    val game = IGDB.findGameById(IGDB.games, id)
    val coverUrl = game?.let { IGDB.findCoverById(IGDB.covers, it.cover!!)?.url }
    val genres = game?.genres?.mapNotNull { IGDB.findGenreById(IGDB.genres, it)?.name } ?: emptyList()
    val platforms = IGDB.findPlatformsById(id)

    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = Color.Magenta,
                    titleContentColor = Color.Black,
                ),
                title = { game?.name?.let { Text(it) } },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                game?.name?.let {
                    Text(
                        text = it,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(textDecoration = TextDecoration.Underline),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                var model by rememberSaveable { mutableIntStateOf(R.raw.no_favori) }
                if (game?.let { IGDB.favoriteGame(it) } == true) {
                    model = R.raw.favori
                } else {
                    model = R.raw.no_favori
                }

                AsyncImage(
                    model = model,
                    contentDescription = "Icône favori",
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            if (game?.let { IGDB.favoriteGame(it) } == true) {
                                model = R.raw.no_favori
                                sendNotification(context, "${game.name}", "${game.name} a été supprimé des favoris.")
                                IGDB.list_favoris.remove(game)
                                IGDB.saveFavorites(context, IGDB.list_favoris)
                            } else {
                                model = R.raw.favori
                                game?.let {
                                    sendNotification(context, "${game.name}", "${game.name} a été ajouté aux favoris.")
                                    IGDB.list_favoris.add(it)
                                    IGDB.saveFavorites(context, IGDB.list_favoris)
                                }
                            }
                        }
                )
            }

            AsyncImage(
                model = "https:$coverUrl",
                contentDescription = "Image du jeu",
                modifier = Modifier
                    .size(250.dp)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Text(
                text = genres.joinToString(", "),
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(8.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                platforms?.let {
                    items(it) { logo ->
                        AsyncImage(
                            model = "https:${logo.url}",
                            contentDescription = "Logo plateforme",
                            modifier = Modifier
                                .size(80.dp)
                                .padding(8.dp)
                        )
                    }
                }
            }

            game?.summary?.let {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = it.ifEmpty { "Aucune description disponible." },
                        fontSize = 18.sp,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Justify
                    )
                }
            }
        }
    }

}






