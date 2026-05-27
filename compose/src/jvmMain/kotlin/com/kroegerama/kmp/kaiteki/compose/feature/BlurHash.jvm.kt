package com.kroegerama.kmp.kaiteki.compose.feature

import androidx.compose.foundation.layout.ExperimentalGridApi
import androidx.compose.foundation.layout.Grid
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kroegerama.kmp.kaiteki.ExperimentalKaitekiApi

@OptIn(ExperimentalGridApi::class, ExperimentalKaitekiApi::class)
internal fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "BlurHash Test"
    ) {
        val hashes = listOf(
            "1x1" to "00F5?x",
            "2x1" to "1GF5?xYk",
            "1x2" to "9CF5?x_{",
            "9x1" to "8GF5?xYk^6#M9vF~W@j=#*",
            "1x9" to "=HF5?x@-@[};jMj]beXmxH",
            "9x9" to "|HF5?xYk^6#M9wKSW@j=#*@-5b,1J5O[V=R:s;w[@[or[k6.O[TLtJnNnO};FxngOZE3NgNHsps,jMFxS#OtcXnzRjxZxHj]OYNeR:JCs9xunhwIbeIpNaxHNGr;v}aeo0Xmt6XS\$et6#*\$ft6nhxHnNV@w{nOenwfNHo0",
        )
        Grid(
            config = {
                column(1.fr)
                column(1.fr)
                column(1.fr)
                gap(4.dp)
            }
        ) {
            hashes.fastForEach { (title, hash) ->
                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f)
                        .blurHash(hash, .8f)
                        .padding(8.dp)
                )
            }
        }
    }
}
