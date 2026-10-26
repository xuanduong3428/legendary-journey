package org.legendaryjourney.lc0analyzer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ChessPositionTest {
    @Test
    fun initialPositionRoundTripsToStandardFen() {
        val fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        assertEquals(fen, ChessPosition.fromFen(fen).toFen())
    }

    @Test
    fun fourFieldFenGetsDefaultMoveCounters() {
        val position = ChessPosition.fromFen("8/8/8/8/8/8/8/K6k b - -")

        assertEquals("8/8/8/8/8/8/8/K6k b - - 0 1", position.toFen())
    }

    @Test
    fun rejectsInvalidRankWidth() {
        assertThrows(IllegalArgumentException::class.java) {
            ChessPosition.fromFen("9/8/8/8/8/8/8/K6k w - - 0 1")
        }
    }

    @Test
    fun rejectsNonNumericHalfmoveClock() {
        assertThrows(IllegalArgumentException::class.java) {
            ChessPosition.fromFen("8/8/8/8/8/8/8/K6k w - - nope 1")
        }
    }
}
