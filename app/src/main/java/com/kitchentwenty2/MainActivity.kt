package com.kitchentwenty2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kitchentwenty2.ui.navigation.AppNavigation
import com.kitchentwenty2.ui.theme.KitchenTwenty2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KitchenTwenty2Theme {
                AppNavigation()
            }
        }
    }
}
