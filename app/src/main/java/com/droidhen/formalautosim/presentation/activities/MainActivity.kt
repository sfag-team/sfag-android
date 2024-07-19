package com.droidhen.formalautosim.presentation.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.droidhen.formalautosim.presentation.navigation.Destinations
import com.droidhen.formalautosim.presentation.navigation.screens.CommunityScreen
import com.droidhen.formalautosim.presentation.navigation.screens.GraphEditorScreen
import com.droidhen.formalautosim.presentation.navigation.screens.HistoryScreen
import com.droidhen.formalautosim.presentation.navigation.screens.MainScreen
import com.droidhen.formalautosim.presentation.navigation.screens.UserProfileScreen
import com.droidhen.formalautosim.presentation.views.BottomBar

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDefaultSettings()
        setContent {
            rememberNavController().apply {
                Column(modifier = Modifier.fillMaxWidth()) {
                    NavHost(navController = this@apply, startDestination = Destinations.MAIN.route, modifier = Modifier.weight(9f)){
                        composable(route = Destinations.MAIN.route){
                            MainScreen()
                        }
                        composable(route = Destinations.HISTORY.route){
                            HistoryScreen()
                        }
                        composable(route = Destinations.COMMUNITY.route){
                            CommunityScreen()
                        }
                        composable(route = Destinations.USER_PROFILE.route){
                            UserProfileScreen()
                        }
                        composable(route = Destinations.GRAPH_EDITOR.route){
                            GraphEditorScreen()
                        }
                    }
                    BottomBar(this@apply)
                }
            }
        }
    }
}