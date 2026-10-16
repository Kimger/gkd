package li.gkd.app.logging

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

data class RandomDelayLog(
    val id: String,
    val time: Long,
    val appId: String,
    val delayMillis: Long,
    val details: String,
)

/** Reads the existing diagnostic files, including entries written before appId was recorded. */
fun readRandomDelayLogs(directory: File): List<RandomDelayLog> {
    if (!directory.exists()) return emptyList()
    val files = directory.listFiles() ?: throw IOException("Cannot read log directory: $directory")
    val header = Regex("^(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) A11yRuleEngine,.*")
    val delayPattern = Regex("(?m)^\\[2]: randomActionDelayMs=(\\d+)$")
    val appPattern = Regex("(?:^|[, ])appId=([^,\\s]+)")
    val format = SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS", Locale.ROOT).apply { isLenient = false }
    val result = mutableListOf<RandomDelayLog>()
    for (file in files.filter { it.name.matches(Regex("gkd-\\d{8}\\.log")) }) {
        val date = file.name.substring(4, 12)
        var startLine = 0
        var timeText: String? = null
        val block = StringBuilder()
        fun finish() {
            val text = block.toString().trimEnd()
            val clock = timeText ?: return
            if (!text.contains("[0]: Random action delay scheduled")) return
            val delay = delayPattern.find(text)?.groupValues?.get(1)?.toLongOrNull() ?: return
            result += RandomDelayLog(
                id = "${file.name}:$startLine",
                time = format.parse("$date $clock").time,
                appId = appPattern.find(text)?.groupValues?.get(1).orEmpty(),
                delayMillis = delay,
                details = "$date $text",
            )
        }
        file.useLines { lines ->
            lines.forEachIndexed { index, line ->
                if (line.isEmpty()) {
                    finish()
                    timeText = null
                    block.clear()
                } else {
                    if (block.isEmpty()) {
                        timeText = header.matchEntire(line)?.groupValues?.get(1)
                        startLine = index
                    }
                    block.appendLine(line)
                }
            }
        }
        finish()
    }
    return result.sortedWith(compareByDescending<RandomDelayLog> { it.time }.thenByDescending { it.id })
}
