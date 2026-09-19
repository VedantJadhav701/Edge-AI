package com.example.llama

import android.text.Html
import android.text.SpannableString
import android.text.Spanned

object ResponseCleaner {

    private val controlTokenRegex = Regex(
        "<\\|(?:im_start|im_end|endoftext|assistant|user|system)\\|?>"
    )

    private val ansiRegex = Regex("\\u001B\\[[0-9;]*[a-zA-Z]")

    fun cleanRawTokenStream(raw: String): String {
        var text = raw

        // Remove Qwen/ChatML control tokens
        text = text.replace(controlTokenRegex, "")

        // Remove ANSI escape sequences
        text = text.replace(ansiRegex, "")

        // Remove thinking blocks completely
        text = text.replace(
            Regex(
                "<think>.*?</think>",
                setOf(
                    RegexOption.DOT_MATCHES_ALL,
                    RegexOption.IGNORE_CASE
                )
            ),
            ""
        )

        // Remove incomplete thinking section during streaming
        val thinkStart = text.indexOf("<think>")
        if (thinkStart >= 0) {
            text = text.substring(0, thinkStart)
        }

        return text
            .replace(Regex("@{2,}"), "")
            .trimStart()
    }

    fun formatMarkdown(text: String): Spanned {
        val cleaned = cleanRawTokenStream(text)

        if (cleaned.isBlank()) {
            return SpannableString("")
        }

        val html = cleaned
            .replace(
                Regex("\\*\\*(.*?)\\*\\*"),
                "<b>$1</b>"
            )
            .replace(
                Regex("`([^`]+)`"),
                "<tt>$1</tt>"
            )
            .replace(
                Regex("^[-*]\\s+(.*)$", RegexOption.MULTILINE),
                "• $1<br/>"
            )
            .replace("\n", "<br/>")

        return Html.fromHtml(
            html,
            Html.FROM_HTML_MODE_LEGACY
        )
    }
}
