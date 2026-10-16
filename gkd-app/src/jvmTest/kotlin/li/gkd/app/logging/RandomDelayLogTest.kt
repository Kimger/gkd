package li.gkd.app.logging

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class RandomDelayLogTest {
    @Test
    fun readsOldAndNewEntriesAcrossDaysAndIgnoresIncompleteRecords() {
        val root = File(System.getProperty("gkd.projectRoot", "."), ".local/tests/random-delay-logs")
        root.mkdirs()
        val directory = Files.createTempDirectory(root.toPath(), "reader-").toFile()
        try {
            directory.resolve("gkd-20261015.log").writeText(
                """
                23:59:59.001 A11yRuleEngine, worker, queryAction
                [0]: Random action delay scheduled
                [1]: subsId=-2, groupKey=5, ruleKey=2, ruleIndex=1
                [2]: randomActionDelayMs=1227

                """.trimIndent()
            )
            val current = directory.resolve("gkd-20261016.log")
            current.writeText(
                """
                00:00:01.002 A11yRuleEngine, worker, queryAction
                [0]: Random action delay scheduled
                [1]: appId=com.tencent.mm, subsId=-2, groupKey=5, ruleKey=2, ruleIndex=1
                [2]: randomActionDelayMs=539

                00:00:02.003 OtherLogger, worker, other
                unrelated text randomActionDelayMs=999

                00:00:03.004 A11yRuleEngine, worker, queryAction
                [0]: Random action delay scheduled
                [1]: appId=com.tencent.mm
                """.trimIndent()
            )
            val logs = readRandomDelayLogs(directory)
            assertEquals(listOf(539L, 1227L), logs.map { it.delayMillis })
            assertEquals(listOf("com.tencent.mm", ""), logs.map { it.appId })
            assertEquals(2001L, logs[0].time - logs[1].time)

            current.appendText("\n[2]: randomActionDelayMs=1582\n\n")
            assertEquals(listOf(1582L, 539L, 1227L), readRandomDelayLogs(directory).map { it.delayMillis })
        } finally {
            directory.deleteRecursively()
        }
    }
}
