package com.ds.pulsar

import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ElevatedButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.beepiz.bluetooth.gattcoroutines.ConnectionClosedException
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ds.pulsar.ui.theme.PulsarTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.concurrent.TimeoutException
import kotlin.math.abs


private lateinit
var navController_: NavHostController
val navController by ::navController_

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContent {
            PulsarTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    navController_ = rememberNavController()
                    NavHost(navController = navController, startDestination = Screens.main) {
                        val durationMs = 700
                        val animSpec = tween<IntOffset>(durationMs, easing = EaseInOutQuad)
                        composable(
                            Screens.main,
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = animSpec
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = animSpec
                                )
                            }) { MainScreen() }
                        composable(
                            Screens.devList,
                            enterTransition = {
                                slideIntoContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Right,
                                    animationSpec = animSpec
                                )
                            },
                            exitTransition = {
                                slideOutOfContainer(
                                    AnimatedContentTransitionScope.SlideDirection.Left,
                                    animationSpec = animSpec
                                )
                            }) { DeviceDiscoverScreen() }
                    }
                }
            }
        }
    }
}

private object Screens{
    const val main = "main"
    const val devList = "device list"
}

@Composable
fun MainScreen() {
    WhenBtIsReady {
        val requiresSearch = remember {
            heartRateStream.requiresSearchForDevice
        }
        if (requiresSearch)
            navController.navigate(Screens.devList)
        else
            MainUI()
    }
}

@Composable
private fun MainUI() {
    BoxWithConstraints {
        ElevatedButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp),
            onClick = { navController.navigate(Screens.devList) }
        ){
            Icon(
                Icons.Default.Contactless,
                contentDescription = "List of available BLE devices",
            )
        }
        //region Digits
        val dm = LocalContext.current.resources.displayMetrics
        val pxPerSp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            100f,
            dm
        ) / 100f
        val heightInPx =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                maxHeight.value,
                dm
            )
        var spHeight = heightInPx / pxPerSp
        var biggerTextSize = 0f
        var smallerTextSize = 0f
        val recalcTextSize = {
            biggerTextSize = spHeight * 0.35f
            smallerTextSize = biggerTextSize * 0.5f
        }
        recalcTextSize()
        val bounds = Rect()
        val paint = Paint()
        val text = "0000"
        paint.textSize = biggerTextSize * pxPerSp
        paint.getTextBounds(text, 0, text.length, bounds)
        val textWidth = abs(bounds.right - bounds.left)
        val surfaceWidthInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            maxWidth.value,
            dm
        )
        if (surfaceWidthInPx < textWidth) {
            spHeight *= surfaceWidthInPx / textWidth
            recalcTextSize()
        }
        Column(
            Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val pulse = remember{ mutableStateOf("---") }
            val time = remember{ mutableStateOf("---") }
            var battery by remember{ mutableStateOf("") }
            LaunchedEffect(null ) {
                while (true) {
                    try {
                        coroutineScope {
                            println("collecting...")
                            pulse.value = "---"
                            heartRateStream.heartRateFlow().collect {
                                pulse.value = it.pulse.toString()
                                if (it.batteryLevel != null)
                                    battery = it.batteryLevel.toString() + "%"
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException && e !is ConnectionClosedException) {
                            throw e
                        }
                        if (e !is TimeoutException) {
                            println("Error collecting data from device: $e")
                            e.printStackTrace()
                        }
                    }
                    println("retrying to connect")
                    delay(200)
                }
            }
            val currentSeconds = {
                System.currentTimeMillis() / 1000
            }
            val icons = arrayOf(Icons.Default.Pause, Icons.Default.PlayArrow )
            val iconNdx = rememberSaveable{ mutableStateOf(0) }
            val startTime = rememberSaveable{ mutableStateOf( currentSeconds() ) }
            val accumulatedTime = rememberSaveable{ mutableStateOf( 0L ) }
            LaunchedEffect(null ) {
                while (true) {
                    delay(950)
                    if ( iconNdx.value != 0 )
                        continue
                    val td = accumulatedTime.value +
                            currentSeconds() - startTime.value
                    val h = td / 3600
                    val m = (td % 3600) / 60
                    val s = td % 60
                    if (h == 0L)
                        time.value = "%02d:%02d".format(m ,s)
                    else
                        time.value = "%d:%02d:%02d".format(h, m ,s)
                }
            }
            Text(text = pulse.value, fontSize = biggerTextSize.sp)
            Text(text = time.value, fontSize = smallerTextSize.sp)
            Row(){
                val mdf = Modifier
                    .padding(horizontal = 32.dp, vertical = 6.dp)
                ElevatedButton(
                    modifier = mdf,
                    onClick = {
                            accumulatedTime.value = 0
                            startTime.value = currentSeconds()
                            iconNdx.value = 0
                }){
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Reset")
                }
                var icon = icons[iconNdx.value]
                ElevatedButton(
                    modifier = mdf,
                    onClick = {
                    if (iconNdx.value == 0)
                        accumulatedTime.value += currentSeconds() - startTime.value
                    else
                        startTime.value = currentSeconds()
                    iconNdx.value = ++iconNdx.value % icons.size
                    icon = icons[iconNdx.value]
                }){
                    Icon(
                        icon,
                        contentDescription = "Pause")
                }
            }
            AnimatedVisibility(
                visible = battery.isNotEmpty(),
                modifier = Modifier.padding( 6.dp ),
                enter = fadeIn(animationSpec = tween(2000))
            ){
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.BatteryStd,
                        contentDescription = "BLE device battery charge level",
                        modifier = Modifier
                            .size(16.spToDp)
                    )
                    Text(text = battery, fontSize = 16.sp)
                }
            }
        }
        // endregion
    }
}

@Preview(
    showBackground = true,
    heightDp = 600,
    widthDp = 300)
@Composable
fun DefaultPreview() {
    PulsarTheme {
        MainUI()
    }
}