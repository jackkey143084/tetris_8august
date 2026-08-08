package com.tetris.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

private val BoardBg = Color(0xFF1B1B2E)
private val GridColor = Color(0x22FFFFFF)
private val BorderColor = Color(0x66FFFFFF)

/**
 * Renders the play field: locked cells, the falling piece, and a ghost
 * drop-preview. Supports tap-to-rotate and horizontal drag to move.
 */
@Composable
fun TetrisBoard(
    engine: TetrisEngine,
    onTap: () -> Unit,
    onDrag: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(TetrisEngine.COLS.toFloat() / TetrisEngine.ROWS)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(BoardBg)
                .pointerInput(Unit) {
                    var lastX = 0f
                    detectDragGestures(
                        onDragStart = { lastX = it.x },
                        onDragEnd = { lastX = 0f },
                    ) { change, _ ->
                        val dx = change.position.x - lastX
                        if (abs(dx) >= size.width / (TetrisEngine.COLS * 2)) {
                            onDrag(if (dx > 0) 1 else -1)
                            lastX = change.position.x
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { onTap() }
                }
        ) {
            drawBoard(engine)
        }
    }
}

private fun DrawScope.drawBoard(engine: TetrisEngine) {
    val cols = TetrisEngine.COLS
    val rows = TetrisEngine.ROWS
    val cell = size.minDimension / maxOf(cols, rows)
    val fieldW = cell * cols
    val fieldH = cell * rows
    val ox = (size.width - fieldW) / 2f
    val oy = (size.height - fieldH) / 2f

    // Locked cells
    val board = engine.boardSnapshot()
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val v = board[r][c]
            if (v != 0) {
                drawCell(ox + c * cell, oy + r * cell, cell, Color(TetrisEngine.COLORS[v - 1]))
            }
        }
    }

    // Ghost piece
    if (!engine.isGameOver && !engine.isPaused) {
        var ghostRow = engine.currentRow()
        while (!wouldCollide(engine, ghostRow + 1)) ghostRow++
        if (ghostRow > engine.currentRow()) {
            drawPiece(engine, ghostRow, engine.currentCol(), engine.currentType(),
                engine.currentRotation(), cell, ox, oy, ghost = true)
        }
    }

    // Current falling piece
    if (!engine.isGameOver) {
        drawPiece(engine, engine.currentRow(), engine.currentCol(), engine.currentType(),
            engine.currentRotation(), cell, ox, oy, ghost = false)
    }

    // Grid lines
    for (c in 0..cols) {
        val x = ox + c * cell
        drawLine(GridColor, Offset(x, oy), Offset(x, oy + fieldH), 1f)
    }
    for (r in 0..rows) {
        val y = oy + r * cell
        drawLine(GridColor, Offset(ox, y), Offset(ox + fieldW, y), 1f)
    }

    // Field border
    drawRect(BorderColor, topLeft = Offset(ox, oy), size = Size(fieldW, fieldH),
        style = Stroke(width = 1f))
}

private fun DrawScope.wouldCollide(engine: TetrisEngine, row: Int): Boolean {
    val shape = TetrisEngine.SHAPES[engine.currentType()][engine.currentRotation()]
    val board = engine.boardSnapshot()
    for (r in 0 until 4) {
        for (c in 0 until 4) {
            if (!shape[r][c]) continue
            val br = row + r
            val bc = engine.currentCol() + c
            if (bc < 0 || bc >= TetrisEngine.COLS || br >= TetrisEngine.ROWS) return true
            if (br >= 0 && board[br][bc] != 0) return true
        }
    }
    return false
}

private fun DrawScope.drawPiece(
    engine: TetrisEngine, row: Int, col: Int, type: Int, rotation: Int,
    cell: Float, ox: Float, oy: Float, ghost: Boolean,
) {
    val shape = TetrisEngine.SHAPES[type][rotation]
    val color = Color(TetrisEngine.COLORS[type])
    for (r in 0 until 4) {
        for (c in 0 until 4) {
            if (shape[r][c]) {
                val x = ox + (col + c) * cell
                val y = oy + (row + r) * cell
                if (ghost) {
                    drawRect(
                        color = color.copy(alpha = 0.25f),
                        topLeft = Offset(x + 2, y + 2),
                        size = Size(cell - 4, cell - 4),
                        style = Stroke(width = 3f),
                    )
                } else {
                    drawCell(x, y, cell, color)
                }
            }
        }
    }
}

private fun DrawScope.drawCell(x: Float, y: Float, cell: Float, color: Color) {
    drawRect(color = color, topLeft = Offset(x + 1, y + 1),
        size = Size(cell - 2, cell - 2))
    // Subtle highlight bevel
    drawRect(color = Color.White.copy(alpha = 0.25f),
        topLeft = Offset(x + 2, y + 2), size = Size(cell - 4, 3f))
    drawRect(color = Color.Black.copy(alpha = 0.2f),
        topLeft = Offset(x + 2, y + cell - 4), size = Size(cell - 4, 2f))
}

/** Small preview showing the upcoming tetromino. */
@Composable
fun NextPiecePreview(type: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.background(BoardBg)) {
        if (type < 0) return@Canvas
        val shape = TetrisEngine.SHAPES[type][0]
        val color = Color(TetrisEngine.COLORS[type])

        // Compute bounding box to center the piece.
        var minR = 4; var maxR = -1; var minC = 4; var maxC = -1
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (shape[r][c]) {
                    if (r < minR) minR = r
                    if (r > maxR) maxR = r
                    if (c < minC) minC = c
                    if (c > maxC) maxC = c
                }
            }
        }
        val pw = (maxC - minC + 1)
        val ph = (maxR - minR + 1)
        val cell = minOf(size.width, size.height) / 4f
        val ox = (size.width - pw * cell) / 2f - minC * cell
        val oy = (size.height - ph * cell) / 2f - minR * cell

        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (shape[r][c]) {
                    drawRect(color = color,
                        topLeft = Offset(ox + c * cell + 1, oy + r * cell + 1),
                        size = Size(cell - 2, cell - 2))
                }
            }
        }
    }
}

private fun DrawScope.drawLine(color: Color, a: Offset, b: Offset, width: Float) {
    drawLine(color, a, b, strokeWidth = width)
}
