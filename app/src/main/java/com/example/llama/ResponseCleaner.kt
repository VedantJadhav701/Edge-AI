package com.example.llama

import android.text.Html
import android.text.SpannableString
import android.text.Spanned

object ResponseCleaner {

    private val controlTokenRegex = Regex(
        "<\\|(?:im_start|im_end|endoftext|assistant|user|system)\\|?>"
    )

    private val ansiRegex = Regex("\\u001B\\[[0-9;]*[a-zA-Z]")

    private val singleCharRepetitionRegex = Regex("(.)\\1{15,}")

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

        // Filter out extreme runaway character repetition loops
        text = text.replace(singleCharRepetitionRegex) { match ->
            match.groupValues[1].repeat(3)
        }

        return text
            .replace(Regex("0{2,}$"), "")
            .replace(Regex("@{2,}"), "")
            .trimStart()
    }

    fun cleanLatexFormulas(raw: String): String {
        var text = raw
        if (text.isBlank()) return text

        // Replace \frac{a}{b} -> (a / b)
        text = text.replace(Regex("""\\frac\{([^}]+)\}\{([^}]+)\}""")) { match ->
            "(${match.groupValues[1]} / ${match.groupValues[2]})"
        }

        // Replace \sqrt{a} -> √(a)
        text = text.replace(Regex("""\\sqrt\{([^}]+)\}""")) { match ->
            "√(${match.groupValues[1]})"
        }

        // Replace \text{...}, \mathrm{...}, \mathbf{...}
        text = text.replace(Regex("""\\(text|mathrm|mathbf|mathsf)\{([^}]+)\}""")) { match ->
            match.groupValues[2]
        }

        // Replace common LaTeX symbols
        text = text
            .replace("\\times", "×")
            .replace("\\cdot", "·")
            .replace("\\ast", "*")
            .replace("\\approx", "≈")
            .replace("\\leq", "≤")
            .replace("\\ge", "≥")
            .replace("\\geq", "≥")
            .replace("\\le", "≤")
            .replace("\\neq", "≠")
            .replace("\\pm", "±")
            .replace("\\mp", "∓")
            .replace("\\infty", "∞")
            .replace("\\partial", "∂")
            .replace("\\nabla", "∇")
            .replace("\\sum", "∑")
            .replace("\\prod", "∏")
            .replace("\\int", "∫")
            // Greek letters
            .replace("\\alpha", "α")
            .replace("\\beta", "β")
            .replace("\\gamma", "γ")
            .replace("\\delta", "δ")
            .replace("\\epsilon", "ε")
            .replace("\\zeta", "ζ")
            .replace("\\eta", "η")
            .replace("\\theta", "θ")
            .replace("\\lambda", "λ")
            .replace("\\mu", "μ")
            .replace("\\nu", "ν")
            .replace("\\pi", "π")
            .replace("\\rho", "ρ")
            .replace("\\sigma", "σ")
            .replace("\\tau", "τ")
            .replace("\\phi", "φ")
            .replace("\\chi", "χ")
            .replace("\\psi", "ψ")
            .replace("\\omega", "ω")
            .replace("\\Delta", "Δ")
            .replace("\\Gamma", "Γ")
            .replace("\\Theta", "Θ")
            .replace("\\Lambda", "Λ")
            .replace("\\Sigma", "Σ")
            .replace("\\Phi", "Φ")
            .replace("\\Omega", "Ω")

        // Superscript mappings
        text = text
            .replace("^0", "⁰")
            .replace("^1", "¹")
            .replace("^2", "²")
            .replace("^3", "³")
            .replace("^4", "⁴")
            .replace("^5", "⁵")
            .replace("^6", "⁶")
            .replace("^7", "⁷")
            .replace("^8", "⁸")
            .replace("^9", "⁹")
            .replace("^+", "⁺")
            .replace("^-", "⁻")
            .replace("^n", "ⁿ")
            .replace("^x", "ˣ")
            .replace("^t", "ᵗ")

        // Subscript mappings
        text = text
            .replace("_0", "₀")
            .replace("_1", "₁")
            .replace("_2", "₂")
            .replace("_3", "₃")
            .replace("_4", "₄")
            .replace("_5", "₅")
            .replace("_6", "₆")
            .replace("_7", "₇")
            .replace("_8", "₈")
            .replace("_9", "₉")
            .replace("_x", "ₓ")
            .replace("_y", "ᵧ")
            .replace("_t", "ₜ")

        // Remove LaTeX delimiters $$ ... $$, \[ ... \], \( ... \)
        text = text
            .replace("$$", "")
            .replace("\\[", "")
            .replace("\\]", "")
            .replace("\\(", "")
            .replace("\\)", "")

        return text
    }

    fun formatMarkdown(text: String): Spanned {
        val cleaned = cleanLatexFormulas(cleanRawTokenStream(text))

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
