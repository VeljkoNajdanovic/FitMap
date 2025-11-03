package veljko.najdanovic19273.fitmap.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import veljko.najdanovic19273.fitmap.ui.auth.LoginScreen
import veljko.najdanovic19273.fitmap.ui.auth.RegisterScreen
import veljko.najdanovic19273.fitmap.ui.profile.EditProfileScreen
import veljko.najdanovic19273.fitmap.ui.profile.ProfileScreen
import veljko.najdanovic19273.fitmap.ui.map.AddObjectScreen
import veljko.najdanovic19273.fitmap.ui.map.MapScreen
import veljko.najdanovic19273.fitmap.ui.map.ObjectDetailsScreen
import veljko.najdanovic19273.fitmap.ui.leaderboard.LeaderboardScreen
import veljko.najdanovic19273.fitmap.ui.theme.*
import veljko.najdanovic19273.fitmap.viewmodel.AuthViewModel
import veljko.najdanovic19273.fitmap.viewmodel.LeaderboardViewModel
import veljko.najdanovic19273.fitmap.viewmodel.MapViewModel
import veljko.najdanovic19273.fitmap.viewmodel.ProfileViewModel

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val authViewModel: AuthViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val mapViewModel: MapViewModel = viewModel()
    val leaderboardViewModel: LeaderboardViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screen.LOGIN) {
        composable(Screen.LOGIN) {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }
        composable(Screen.REGISTER) {
            RegisterScreen(navController = navController, authViewModel = authViewModel)
        }
        composable(Screen.HOME) {
            HomeScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        composable(Screen.MAP) {
            MapScreen(
                navController = navController,
                mapViewModel = mapViewModel
            )
        }
        composable(Screen.PROFILE) {
            ProfileScreen(
                navController = navController,
                authViewModel = authViewModel,
                profileViewModel = profileViewModel
            )
        }
        composable(Screen.EDIT_PROFILE) {
            EditProfileScreen(
                navController = navController,
                profileViewModel = profileViewModel
            )
        }
        composable(
            route = "${Screen.ADD_OBJECT}?lat={lat}&lng={lng}&gymId={gymId}",
            arguments = listOf(
                navArgument("lat") { type = NavType.FloatType },
                navArgument("lng") { type = NavType.FloatType },
                navArgument("gymId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
            val lng = backStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 0.0
            val gymId = backStackEntry.arguments?.getString("gymId")
            AddObjectScreen(
                navController = navController,
                mapViewModel = mapViewModel,
                latitude = lat,
                longitude = lng,
                parentGymId = gymId
            )
        }
        composable(
            route = Screen.OBJECT_DETAILS,
            arguments = listOf(
                navArgument("objectId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val objectId = backStackEntry.arguments?.getString("objectId") ?: ""
            ObjectDetailsScreen(
                navController = navController,
                mapViewModel = mapViewModel,
                objectId = objectId
            )
        }
        composable(Screen.LEADERBOARD) {
            LeaderboardScreen(
                navController = navController,
                leaderboardViewModel = leaderboardViewModel
            )
        }
    }
}

@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by authViewModel.state.collectAsState()

    LaunchedEffect(state.user) {
        if (state.user == null) {
            navController.navigate(Screen.LOGIN) {
                popUpTo(Screen.HOME) { inclusive = true }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GradientDarkStart,
                        GradientDarkMid,
                        DarkBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profesionalan logo bez emoji
            Surface(
                modifier = Modifier.size(100.dp),
                shape = RoundedCornerShape(20.dp),
                color = DarkCard,
                tonalElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FM",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Dobrodošli u FitMap!",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = state.user?.email?.substringBefore("@") ?: "Korisniče",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = GoldPrimary
            )

            Text(
                text = state.user?.email ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Kartica za mapu - profesionalan dizajn
            Card(
                onClick = { navController.navigate(Screen.MAP) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = GoldPrimary.copy(alpha = 0.2f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Mapa",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pronađi teretane u blizini",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kartica za profil
            Card(
                onClick = { navController.navigate(Screen.PROFILE) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = SteelBlue.copy(alpha = 0.3f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = GoldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Moj Profil",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Pogledaj svoje podatke",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kartica za rang listu
            Card(
                onClick = { navController.navigate(Screen.LEADERBOARD) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Rang Lista",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Vidi najbolje korisnike",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Dugme za logout - elegantno
            OutlinedButton(
                onClick = {
                    authViewModel.logout()
                    navController.navigate(Screen.LOGIN) {
                        popUpTo(Screen.HOME) { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GoldPrimary
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(GoldPrimary, GoldLight)
                    )
                )
            ) {
                Icon(Icons.Default.ExitToApp, "Odjavi se", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Odjavi se",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
