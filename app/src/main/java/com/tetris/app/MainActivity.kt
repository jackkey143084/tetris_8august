package com.tetris.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val engine = TetrisEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // A versioned flag the Compose layer observes so it re-draws
            // whenever the engine notifies a state change.
            var tick by remember { mutableStateOf(0) }
            engine.listener = object : TetrisEngine.Listener {
                override fun onStateChanged() { tick++ }
                override fun onGameOver() { tick++ }
            }

            // Gravity game loop. Re-evaluates delay each cycle so speed
            // tracks the current level.
            LaunchedEffect(tick) {
                while (!engine.isGameOver && !engine.isPaused) {
                    delay(engine.interval)
                    engine.tick()
                }
            }

            TetrisScreen(
                engine = engine,
                onLeft = { engine.moveLeft() },
                onRight = { engine.moveRight() },
                onDown = { engine.softDrop() },
                onRotate = { engine.rotate() },
                onDrop = { engine.hardDrop() },
                onPauseToggle = { engine.togglePause() },
                onRestart = { engine.reset() },
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (!engine.isGameOver) engine.setPaused(true)
    }
}

// Convenience accessor to keep the game-loop call site readable.
private val TetrisEngine.interval: Long
    get() = getInterval()
