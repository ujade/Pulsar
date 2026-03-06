package com.ds.pulsar

import android.app.Activity
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beepiz.bluetooth.gattcoroutines.ConnectionClosedException
import com.ds.pulsar.ui.theme.PulsarTheme
import com.ds.pulsar.widget.HrWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.util.concurrent.TimeoutException
import kotlin.math.abs


private lateinit
var navController_: NavHostController
val navController by ::navController_

lateinit
var activity : Activity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                            }
                        ) { MainScreen() }
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
                            }
                        ) { DeviceDiscoverScreen() }
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
        val topButtonPadding = 6.dp
        ElevatedButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(topButtonPadding),
            onClick = { navController.navigate(Screens.devList) }
        ){
            Icon(
                Icons.AutoMirrored.Filled.BluetoothSearching,
                contentDescription = "List of available BLE devices",
            )
        }
        var askAgain by remember{
            mutableStateOf(gotToAskAboutDisablingBt)
        }
        fun quit(disableBt: Boolean ){
            if (disableBt)
                btAdapter.disable()
            activity.finishAndRemoveTask()
        }
        var showExitDialog by remember{ mutableStateOf(false) }
        if (showExitDialog) {
            Dialog(onDismissRequest = {showExitDialog = false}) {
                Card {
                    val mdf = Modifier.padding(8.dp)
                    Text(
                        modifier = mdf,
                        text = "Turn off bluetooth?",
                        style=MaterialTheme.typography.headlineLarge
                    )
                    Text(
                        modifier = mdf,
                        text = """
                            Since I have turned on bluetooth, I can turn it off for ya.
                            But I will only ever do it, when I was the one, who have turned it on.""".trimIndent(),
                    )
                    Row(
                        modifier = mdf,
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Checkbox(
                            checked = !askAgain,
                            onCheckedChange = {
                                askAgain = !it
                            }
                        )
                        Text("Don't ask again, always apply current choice")
                    }
                    Row(
                        modifier = mdf.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ){
                        ElevatedButton(
                            onClick = {
                                if (!askAgain) {
                                    disableBt = true
                                    gotToAskAboutDisablingBt = askAgain
                                }
                                quit(true)
                            }
                        ){
                            Text("Turn it off")
                        }
                        ElevatedButton(
                            onClick = {
                                if (!askAgain) {
                                    disableBt = false
                                    gotToAskAboutDisablingBt = askAgain
                                }
                                quit(false)
                            }
                        ){
                            Text("Leave it on")
                        }
                    }
                }
            }
        }
        ElevatedButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(topButtonPadding),
            onClick = {
                if (mayDisableBt){
                    if (gotToAskAboutDisablingBt) {
                        showExitDialog = true
                    }
                    else{
                        quit(disableBt)
                    }
                }
                else
                    quit(false)
            }
        ){
            Icon(
                Icons.Default.Close,
                contentDescription = "Close the app",
            )
        }
        //region Digits
        val landscape = maxWidth > maxHeight
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
            biggerTextSize = spHeight * 0.45f
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
                .fillMaxSize()
                .padding(top =
                    if (landscape)
                        0.dp
                    else
                        topButtonPadding + ButtonDefaults.MinHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
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
                                HrWidget.sendHrUpdate(activity, bpm = pulse.value.toInt())
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
            var iconNdx by rememberSaveable{ mutableStateOf(0) }
            var startTime by rememberSaveable{ mutableStateOf( currentSeconds() ) }
            var accumulatedTime by rememberSaveable{ mutableStateOf( 0L ) }
            LaunchedEffect(null ) {
                while (true) {
                    delay(950)
                    if ( iconNdx != 0 )
                        continue
                    val td = accumulatedTime +
                            currentSeconds() - startTime
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
            Row{
                val mdf = Modifier
                    .padding(horizontal = 32.dp, vertical = 6.dp)
                ElevatedButton(
                    modifier = mdf,
                    onClick = {
                            accumulatedTime = 0
                            startTime = currentSeconds()
                            iconNdx = 0
                    }
                ){
                    Icon(
                        Icons.Default.RestartAlt,
                        contentDescription = "Reset")
                }
                val icons by remember{
                    mutableStateOf( arrayOf(Icons.Default.Pause, Icons.Default.PlayArrow ) )
                }
                ElevatedButton(
                    modifier = mdf,
                    onClick = {
                        if (iconNdx == 0)
                            accumulatedTime += currentSeconds() - startTime
                        else
                            startTime = currentSeconds()
                        iconNdx = ++iconNdx % icons.size
                    }
                ){
                    Icon(
                        icons[iconNdx],
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