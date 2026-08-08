package com.tetris.app

import kotlin.math.max

/**
 * Pure Tetris game logic. No Android dependencies, so it is easy to reason
 * about and unit test independently of rendering.
 */
class TetrisEngine {

    interface Listener {
        fun onStateChanged()
        fun onGameOver()
    }

    companion object {
        const val COLS = 10
        const val ROWS = 20

        /** Tetromino shapes: SHAPES[type][rotation][row][col]. */
        val SHAPES: Array<Array<Array<BooleanArray>>> = buildShapes()

        /** Distinct color (ARGB Int) per tetromino type. */
        val COLORS = intArrayOf(
            0xFF00E5FF.toInt(), // I - cyan
            0xFFFFD600.toInt(), // O - yellow
            0xFFC04CFF.toInt(), // T - purple
            0xFF00E676.toInt(), // S - green
            0xFFFF1744.toInt(), // Z - red
            0xFF2979FF.toInt(), // J - blue
            0xFFFF9100.toInt(), // L - orange
        )

        private fun buildShapes(): Array<Array<Array<BooleanArray>>> {
            val defs = arrayOf(
                // I
                arrayOf("....", "XXXX", "....", "...."),
                arrayOf("..X.", "..X.", "..X.", "..X."),
                arrayOf("....", "....", "XXXX", "...."),
                arrayOf(".X..", ".X..", ".X..", ".X.."),
                // O (same for all rotations)
                arrayOf(".XX.", ".XX.", "....", "...."),
                arrayOf(".XX.", ".XX.", "....", "...."),
                arrayOf(".XX.", ".XX.", "....", "...."),
                arrayOf(".XX.", ".XX.", "....", "...."),
                // T
                arrayOf(".X..", "XXX.", "....", "...."),
                arrayOf(".X..", ".XX.", ".X..", "...."),
                arrayOf("....", "XXX.", ".X..", "...."),
                arrayOf(".X..", "X...", "XX..", "...."),
                // S
                arrayOf(".XX.", "XX..", "....", "...."),
                arrayOf(".X..", ".XX.", "..X.", "...."),
                arrayOf("....", ".XX.", "XX..", "...."),
                arrayOf("X...", "XX..", ".X..", "...."),
                // Z
                arrayOf("XX..", ".XX.", "....", "...."),
                arrayOf("..X.", ".XX.", ".X..", "...."),
                arrayOf("....", "XX..", ".XX.", "...."),
                arrayOf(".X..", "XX..", "X...", "...."),
                // J
                arrayOf("X...", "XXX.", "....", "...."),
                arrayOf(".XX.", ".X..", ".X..", "...."),
                arrayOf("....", "XXX.", "..X.", "...."),
                arrayOf(".X..", ".X..", "XX..", "...."),
                // L
                arrayOf("..X.", "XXX.", "....", "...."),
                arrayOf(".X..", ".X..", ".XX.", "...."),
                arrayOf("....", "XXX.", "X...", "...."),
                arrayOf("XX..", ".X..", ".X..", "...."),
            )
            return Array(7) { t ->
                Array(4) { rot ->
                    val rows = defs[t * 4 + rot]
                    Array(4) { r ->
                        BooleanArray(4) { c -> rows[r][c] == 'X' }
                    }
                }
            }
        }
    }

    // board[r][c] == 0 -> empty; otherwise (type + 1) so the renderer can
    // look up the locked color.
    private val board = Array(ROWS) { IntArray(COLS) }

    private var currentType = 0
    private var currentRotation = 0
    private var currentRow = 0
    private var currentCol = 0
    private var nextType = 0

    var score = 0
        private set
    var lines = 0
        private set
    var level = 1
        private set
    var isGameOver = false
        private set
    var isPaused = false
        private set

    var listener: Listener? = null

    init {
        reset()
    }

    fun reset() {
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) board[r][c] = 0
        }
        score = 0
        lines = 0
        level = 1
        isGameOver = false
        isPaused = false
        nextType = randomType()
        spawn()
        notifyChanged()
    }

    private fun spawn() {
        currentType = nextType
        nextType = randomType()
        currentRotation = 0
        currentRow = 0
        // Piece enters centered at the top of the field.
        currentCol = (COLS - 4) / 2
        if (collides(currentRow, currentCol, currentRotation)) {
            isGameOver = true
            listener?.onGameOver()
        }
    }

    private fun randomType(): Int = (Math.random() * 7).toInt()

    private fun collides(row: Int, col: Int, rotation: Int): Boolean {
        val shape = SHAPES[currentType][rotation]
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (!shape[r][c]) continue
                val br = row + r
                val bc = col + c
                if (bc < 0 || bc >= COLS || br >= ROWS) return true
                if (br >= 0 && board[br][bc] != 0) return true
            }
        }
        return false
    }

    private fun lockPiece() {
        val shape = SHAPES[currentType][currentRotation]
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (shape[r][c]) {
                    val br = currentRow + r
                    val bc = currentCol + c
                    if (br in 0 until ROWS && bc in 0 until COLS) {
                        board[br][bc] = currentType + 1
                    }
                }
            }
        }
        clearLines()
        if (!isGameOver) spawn()
    }

    private fun clearLines() {
        var cleared = 0
        var r = ROWS - 1
        while (r >= 0) {
            var full = true
            for (c in 0 until COLS) {
                if (board[r][c] == 0) {
                    full = false
                    break
                }
            }
            if (full) {
                cleared++
                for (rr in r downTo 1) {
                    System.arraycopy(board[rr - 1], 0, board[rr], 0, COLS)
                }
                for (c in 0 until COLS) board[0][c] = 0
                // re-check the same row index after the shift
            } else {
                r--
            }
        }
        if (cleared > 0) {
            lines += cleared
            // Classic NES-style scoring.
            val pts = intArrayOf(0, 40, 100, 300, 1200)
            score += pts[cleared] * level
            level = 1 + lines / 10
        }
    }

    fun moveLeft(): Boolean {
        if (isGameOver || isPaused) return false
        if (!collides(currentRow, currentCol - 1, currentRotation)) {
            currentCol--
            notifyChanged()
            return true
        }
        return false
    }

    fun moveRight(): Boolean {
        if (isGameOver || isPaused) return false
        if (!collides(currentRow, currentCol + 1, currentRotation)) {
            currentCol++
            notifyChanged()
            return true
        }
        return false
    }

    fun rotate(): Boolean {
        if (isGameOver || isPaused) return false
        val newRot = (currentRotation + 1) % 4
        // Simple wall-kick: try original col, then +/-1, +/-2.
        for (kick in intArrayOf(0, -1, 1, -2, 2)) {
            if (!collides(currentRow, currentCol + kick, newRot)) {
                currentCol += kick
                currentRotation = newRot
                notifyChanged()
                return true
            }
        }
        return false
    }

    /** Advance one tick (gravity). Returns true if the piece moved, false if locked. */
    fun tick(): Boolean {
        if (isGameOver || isPaused) return false
        if (!collides(currentRow + 1, currentCol, currentRotation)) {
            currentRow++
            notifyChanged()
            return true
        }
        lockPiece()
        notifyChanged()
        return false
    }

    fun softDrop() {
        if (isGameOver || isPaused) return
        if (tick()) score += 1
        notifyChanged()
    }

    fun hardDrop() {
        if (isGameOver || isPaused) return
        var dropped = 0
        while (!collides(currentRow + 1, currentCol, currentRotation)) {
            currentRow++
            dropped++
        }
        score += dropped * 2
        lockPiece()
        notifyChanged()
    }

    fun togglePause() {
        if (isGameOver) return
        isPaused = !isPaused
        notifyChanged()
    }

    fun setPaused(p: Boolean) {
        isPaused = p
        notifyChanged()
    }

    private fun notifyChanged() {
        listener?.onStateChanged()
    }

    // ---- Accessors for the renderer ----

    fun boardSnapshot(): Array<IntArray> = board

    fun currentType() = currentType
    fun currentRotation() = currentRotation
    fun currentRow() = currentRow
    fun currentCol() = currentCol
    fun nextType() = nextType

    /** Fall interval in milliseconds for the current level. */
    fun getInterval(): Long = max(80, 800 - (level - 1) * 70).toLong()
}
