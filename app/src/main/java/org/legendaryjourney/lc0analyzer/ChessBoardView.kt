package org.legendaryjourney.lc0analyzer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

class ChessBoardView(context: Context) : View(context) {
    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF0D9B5.toInt() }
    private val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB58863.toInt() }
    private val piecePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF202020.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("serif", Typeface.NORMAL)
    }
    private var position = ChessPosition.initial()
    var onPositionChanged: ((ChessPosition) -> Unit)? = null

    fun setPosition(newPosition: ChessPosition) {
        position = newPosition
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)
        val desiredHeight = MeasureSpec.getSize(heightMeasureSpec)
        val side = min(desiredWidth, desiredHeight)
        setMeasuredDimension(desiredWidth, side)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val side = min(width, height)
        val leftOffset = (width - side) / 2f
        val topOffset = (height - side) / 2f
        val cell = side / 8f
        piecePaint.textSize = cell * 0.76f
        for (rank in 0 until 8) {
            for (file in 0 until 8) {
                val left = leftOffset + file * cell
                val top = topOffset + rank * cell
                canvas.drawRect(
                    left,
                    top,
                    left + cell,
                    top + cell,
                    if ((rank + file) % 2 == 0) lightPaint else darkPaint,
                )
                val piece = position.squares[rank * 8 + file]
                if (piece != '.') {
                    val symbol = pieceSymbols[piece] ?: continue
                    val baseline = top + cell / 2f - (piecePaint.ascent() + piecePaint.descent()) / 2f
                    canvas.drawText(symbol, left + cell / 2f, baseline, piecePaint)
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val side = min(width, height).toFloat()
        if (side <= 0f) return true
        val leftOffset = (width - side) / 2f
        val topOffset = (height - side) / 2f
        if (event.x !in leftOffset..(leftOffset + side) ||
            event.y !in topOffset..(topOffset + side)
        ) return true
        val cell = side / 8f
        val file = ((event.x - leftOffset) / cell).toInt().coerceIn(0, 7)
        val rank = ((event.y - topOffset) / cell).toInt().coerceIn(0, 7)
        val squares = position.squares.copyOf()
        val index = rank * 8 + file
        val cycle = ".KQRBNPkqrbnp"
        val next = (cycle.indexOf(squares[index]) + 1).coerceAtLeast(0) % cycle.length
        squares[index] = cycle[next]
        position = position.copy(
            squares = squares,
            castlingRights = "-",
            enPassant = "-",
            halfmoveClock = 0,
        )
        invalidate()
        onPositionChanged?.invoke(position)
        performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private companion object {
        val pieceSymbols = mapOf(
            'K' to "♔", 'Q' to "♕", 'R' to "♖", 'B' to "♗", 'N' to "♘", 'P' to "♙",
            'k' to "♚", 'q' to "♛", 'r' to "♜", 'b' to "♝", 'n' to "♞", 'p' to "♟",
        )
    }
}
