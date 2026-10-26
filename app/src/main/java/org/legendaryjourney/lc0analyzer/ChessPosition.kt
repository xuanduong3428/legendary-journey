package org.legendaryjourney.lc0analyzer

data class ChessPosition(
    val squares: CharArray,
    val activeColor: Char = 'w',
    val castlingRights: String = "-",
    val enPassant: String = "-",
    val halfmoveClock: Int = 0,
    val fullmoveNumber: Int = 1,
) {
    fun toFen(): String {
        val placement = (0 until 8).joinToString("/") { rank ->
            buildString {
                var emptyCount = 0
                for (file in 0 until 8) {
                    val piece = squares[rank * 8 + file]
                    if (piece == '.') {
                        emptyCount++
                    } else {
                        if (emptyCount > 0) {
                            append(emptyCount)
                            emptyCount = 0
                        }
                        append(piece)
                    }
                }
                if (emptyCount > 0) append(emptyCount)
            }
        }
        return "$placement $activeColor $castlingRights $enPassant $halfmoveClock $fullmoveNumber"
    }

    companion object {
        private val validPieces = "KQRBNPkqrbnp"

        fun initial(): ChessPosition = fromFen(
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        )

        fun fromFen(fen: String): ChessPosition {
            val fields = fen.trim().split(Regex("\\s+"))
            require(fields.size in 4..6) { "FEN cần có từ 4 đến 6 trường." }

            val ranks = fields[0].split("/")
            require(ranks.size == 8) { "Bàn cờ trong FEN phải có đúng 8 hàng." }
            val squares = CharArray(64) { '.' }
            ranks.forEachIndexed { rank, text ->
                var file = 0
                for (symbol in text) {
                    if (symbol in '1'..'8') {
                        file += symbol.digitToInt()
                    } else {
                        require(symbol in validPieces) { "FEN chứa quân cờ không hợp lệ." }
                        require(file < 8) { "Một hàng trong FEN có quá 8 ô." }
                        squares[rank * 8 + file] = symbol
                        file++
                    }
                    require(file <= 8) { "Một hàng trong FEN có quá 8 ô." }
                }
                require(file == 8) { "Mỗi hàng trong FEN phải biểu diễn đủ 8 ô." }
            }

            val activeColor = fields[1].singleOrNull()
                ?: throw IllegalArgumentException("Lượt đi trong FEN phải là w hoặc b.")
            require(activeColor == 'w' || activeColor == 'b') {
                "Lượt đi trong FEN phải là w hoặc b."
            }

            val castling = fields[2]
            require(castling == "-" ||
                (castling.all { it in "KQkq" } && castling.toSet().size == castling.length)
            ) { "Quyền nhập thành trong FEN không hợp lệ." }

            val enPassant = fields[3]
            require(enPassant == "-" || enPassant.matches(Regex("[a-h][36]"))) {
                "Ô bắt tốt qua đường trong FEN không hợp lệ."
            }

            val halfmove = if (fields.size >= 5) {
                fields[4].toIntOrNull()
                    ?: throw IllegalArgumentException("Bộ đếm nửa nước trong FEN không hợp lệ.")
            } else {
                0
            }
            val fullmove = if (fields.size >= 6) {
                fields[5].toIntOrNull()
                    ?: throw IllegalArgumentException("Số thứ tự nước đi trong FEN không hợp lệ.")
            } else {
                1
            }
            require(halfmove >= 0) { "Bộ đếm nửa nước không thể âm." }
            require(fullmove >= 1) { "Số thứ tự nước đi phải ít nhất là 1." }

            return ChessPosition(squares, activeColor, castling, enPassant, halfmove, fullmove)
        }
    }
}
