package org.legendaryjourney.lc0analyzer

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    @Volatile private var activeProcess: Process? = null
    @Volatile private var stopRequested = false
    private lateinit var board: ChessBoardView
    private lateinit var fenInput: EditText
    private lateinit var status: TextView
    private lateinit var output: TextView
    private lateinit var analyzeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        showPosition(ChessPosition.initial())
        status.text = "Chạm một ô để đặt quân, hoặc nạp FEN."
    }

    private fun buildScreen() {
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(20))
        }
        scroll.addView(content)
        setContentView(scroll)

        content.addView(TextView(this).apply {
            text = "Lc0 — Phân tích cờ"
            textSize = 24f
        })
        content.addView(TextView(this).apply {
            text = "Chạm ô để đổi quân: trống → quân trắng → quân đen."
            textSize = 14f
            setPadding(0, dp(4), 0, dp(8))
        })

        board = ChessBoardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(340),
            )
            onPositionChanged = { fenInput.setText(it.toFen()) }
        }
        content.addView(board)

        fenInput = EditText(this).apply {
            hint = "Dán chuỗi FEN (4–6 trường)"
            setSingleLine(false)
            minLines = 2
            textSize = 14f
        }
        content.addView(fenInput)
        content.addView(horizontalButtons(
            makeButton("Nạp FEN") { loadFen() },
            makeButton("Vị trí ban đầu") { showPosition(ChessPosition.initial()) },
        ))
        content.addView(makeButton("Làm trống bàn cờ") {
            showPosition(ChessPosition(CharArray(64) { '.' }))
        })
        content.addView(horizontalButtons(
            makeButton("Phân tích") { analyze() }.also { analyzeButton = it },
            makeButton("Dừng") { stopAnalysis() },
        ))

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, dp(12), 0, dp(4))
        }
        output = TextView(this).apply {
            textSize = 14f
            setTextIsSelectable(true)
            setPadding(0, dp(4), 0, 0)
        }
        content.addView(status)
        content.addView(output)
    }

    private fun horizontalButtons(vararg buttons: Button): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            buttons.forEach { button ->
                addView(button, LinearLayout.LayoutParams(0, dp(48), 1f))
            }
        }

    private fun makeButton(label: String, action: () -> Unit) =
        Button(this).apply {
            text = label
            setOnClickListener { action() }
        }

    private fun loadFen() {
        try {
            showPosition(ChessPosition.fromFen(fenInput.text.toString()))
            status.text = "Đã nạp thế cờ từ FEN."
        } catch (error: IllegalArgumentException) {
            status.text = error.message ?: "FEN không hợp lệ."
        }
    }

    private fun showPosition(position: ChessPosition) {
        board.setPosition(position)
        fenInput.setText(position.toFen())
        output.text = ""
    }

    private fun analyze() {
        if (activeProcess != null) {
            status.text = "Lc0 đang phân tích. Hãy dừng lượt hiện tại trước."
            return
        }
        val position = try {
            ChessPosition.fromFen(fenInput.text.toString())
        } catch (error: IllegalArgumentException) {
            status.text = error.message ?: "FEN không hợp lệ."
            return
        }
        if (position.squares.count { it == 'K' } != 1 ||
            position.squares.count { it == 'k' } != 1
        ) {
            status.text = "Thế cờ cần có đúng một vua trắng và một vua đen."
            return
        }

        showPosition(position)
        stopRequested = false
        analyzeButton.isEnabled = false
        output.text = ""
        status.text = "Đang tìm engine Lc0…"
        executor.execute {
            try {
                val engine = Lc0Engine.find(this)
                    ?: throw IllegalStateException(
                        "Không tìm thấy engine tương thích. Hãy cài APK Lc0 v0.32.1 trước."
                    )
                mainHandler.post { status.text = "Đang khởi chạy ${engine.name}…" }
                runAnalysis(engine.path, position)
            } catch (error: Exception) {
                if (stopRequested) {
                    mainHandler.post { status.text = "Đã dừng phân tích." }
                } else {
                    mainHandler.post {
                        status.text = "Không thể phân tích: ${error.message ?: error.javaClass.simpleName}"
                    }
                }
                mainHandler.post { analyzeButton.isEnabled = true }
            }
        }
    }

    private fun runAnalysis(path: String, position: ChessPosition) {
        val process = ProcessBuilder(path).redirectErrorStream(true).start()
        activeProcess = process
        val writer = process.outputStream.bufferedWriter()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        try {
            writer.write("uci\n")
            writer.flush()
            if (!readUntil(reader, "uciok")) throw IllegalStateException("Engine không phản hồi uciok.")

            writer.write("isready\n")
            writer.flush()
            if (!readUntil(reader, "readyok")) throw IllegalStateException("Engine không phản hồi readyok.")

            writer.write("position fen ${position.toFen()}\ngo depth 12\n")
            writer.flush()
            var latestInfo = ""
            var bestMove = ""
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val current = line.orEmpty()
                if (current.startsWith("info ")) latestInfo = current
                if (current.startsWith("bestmove ")) {
                    bestMove = current
                    break
                }
                if (Thread.currentThread().isInterrupted) break
            }
            if (bestMove.isNotEmpty()) {
                val result = listOf(latestInfo, bestMove).filter { it.isNotBlank() }.joinToString("\n")
                mainHandler.post {
                    output.text = result
                    status.text = "Phân tích hoàn tất."
                }
            } else if (activeProcess === process && !stopRequested) {
                throw IllegalStateException("Engine đã dừng trước khi trả về nước tốt nhất.")
            } else if (stopRequested) {
                mainHandler.post { status.text = "Đã dừng phân tích." }
            }
        } finally {
            runCatching {
                writer.write("quit\n")
                writer.flush()
            }
            process.destroy()
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly()
            reader.close()
            writer.close()
            if (activeProcess === process) activeProcess = null
            mainHandler.post { analyzeButton.isEnabled = true }
        }
    }

    private fun readUntil(reader: BufferedReader, expected: String): Boolean {
        while (true) {
            val line = reader.readLine() ?: return false
            if (line.startsWith("info ")) mainHandler.post { output.text = line }
            if (line.trim() == expected) return true
        }
    }

    private fun stopAnalysis() {
        val process = activeProcess
        if (process == null) {
            status.text = "Không có lượt phân tích đang chạy."
        } else {
            stopRequested = true
            status.text = "Đang dừng phân tích…"
            process.destroy()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        activeProcess?.destroy()
        executor.shutdownNow()
        super.onDestroy()
    }
}
