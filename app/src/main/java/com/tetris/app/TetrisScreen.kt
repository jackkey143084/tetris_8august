package com.tetris.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowLeft
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgDark = Color(0xFF0F0F1B)
private val Accent = Color(0xFF00E5FF)
private val TextLight = Color.White
private val BtnBg = Color(0xFF2A2A40)
private val PanelBg = Color(0xFF1B1B2E)

@Composable
fun TetrisScreen(
    engine: TetrisEngine,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onDown: () -> Unit,
    onRotate: () -> Unit,
    onDrop: () -> Unit,
    onPauseToggle: () -> Unit,
    onRestart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Hud(engine)

        Spacer(Modifier.height(6.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            TetrisBoard(
                engine = engine,
                onTap = onRotate,
                onDrag = { dir -> if (dir > 0) onRight() else onLeft() },
            )
            if (engine.isGameOver) {
                OverlayText(
                    "Game Over\nScore: ${engine.score}\nTap Restart",
                    Modifier.padding(16.dp),
                )
            } else if (engine.isPaused) {
                OverlayText("Paused", Modifier)
            }
        }

        Spacer(Modifier.height(6.dp))
        NextAndPauseRow(engine, onPauseToggle, onRestart)
        Spacer(Modifier.height(8.dp))
        ControlButtons(onLeft, onRight, onDown, onRotate, onDrop)
    }
}

@Composable
private fun Hud(engine: TetrisEngine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatLabel("Score", "${engine.score}", TextLight, Modifier.weight(1f))
        StatLabel("Level", "${engine.level}", Accent, Modifier.weight(1f),
            textAlign = TextAlign.Center)
        StatLabel("Lines", "${engine.lines}", TextLight, Modifier.weight(1f),
            textAlign = TextAlign.End)
    }
}

@Composable
private fun StatLabel(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    Column(modifier = modifier) {
        Text(text = label, color = color.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            textAlign = textAlign, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun OverlayText(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = TextLight,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NextAndPauseRow(
    engine: TetrisEngine,
    onPauseToggle: () -> Unit,
    onRestart: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Next", color = TextLight, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            NextPiecePreview(
                type = engine.nextType(),
                modifier = Modifier
                    .size(72.dp)
                    .background(PanelBg, RoundedCornerShape(8.dp)),
            )
        }
        Button(
            onClick = { if (engine.isGameOver) onRestart() else onPauseToggle() },
            colors = ButtonDefaults.buttonColors(containerColor = BtnBg),
        ) {
            Icon(
                imageVector = if (engine.isGameOver) Icons.Filled.Refresh
                    else if (engine.isPaused) Icons.Filled.PlayArrow
                    else Icons.Filled.Pause,
                contentDescription = if (engine.isGameOver) "Restart"
                    else if (engine.isPaused) "Resume" else "Pause",
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (engine.isGameOver) "Restart"
                    else if (engine.isPaused) "Resume" else "Pause",
            )
        }
    }
}

@Composable
private fun ControlButtons(
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onDown: () -> Unit,
    onRotate: () -> Unit,
    onDrop: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ControlButton("◀", Icons.Filled.ArrowLeft, Modifier.weight(1f), onLeft)
            ControlButton("▼", Icons.Filled.ArrowDownward, Modifier.weight(1f), onDown)
            ControlButton("▶", Icons.Filled.ArrowRight, Modifier.weight(1f), onRight)
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ControlButton("Rotate", Icons.Filled.RotateRight, Modifier.weight(1f), onRotate)
            ControlButton("Drop", Icons.Filled.VerticalAlignBottom, Modifier.weight(1f), onDrop)
        }
    }
}

@Composable
private fun ControlButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BtnBg),
    ) {
        Icon(icon, contentDescription = text, tint = TextLight)
        Spacer(Modifier.width(6.dp))
        Text(text, color = TextLight, fontSize = 16.sp)
    }
}
