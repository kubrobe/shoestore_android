package uz.shushop.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import uz.shushop.app.ui.AppRoot
import uz.shushop.app.ui.ShopViewModel
import uz.shushop.app.ui.theme.ShushopTheme

class MainActivity : ComponentActivity() {
    private val vm: ShopViewModel by viewModels {
        ShopViewModel.factory((application as ShushopApp).container.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShushopTheme {
                AppRoot(vm)
            }
        }
    }
}
