package meetnote3.transcript

import kotlin.test.Test
import kotlin.test.assertEquals

class LrcParserKtTest {
    @Test
    fun `parseLrcContent should parse LRC content correctly ignoring header and duplicate lines`() {
        val lrcContent = """
        [by:whisper.cpp]
        [00:10.00] hello
        [00:11.00] hello
        [00:12.00] world
        """.trimIndent()

        val expected = listOf(
            LrcLine("00:10.00", "hello"),
            LrcLine("00:12.00", "world"),
        )

        val result = parseLrcContent(lrcContent)

        assertEquals(expected, result)
    }
}
