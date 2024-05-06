package com.ds.pulsar

import androidx.compose.runtime.Composable

@Composable
fun WhenBtIsReady(whenBtIsReady: @Composable ()->Unit){
    EnsurePermissionsGranted {
        EnsureBtIsOn {
            whenBtIsReady()
        }
    }
}