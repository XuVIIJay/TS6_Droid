package dev.tsdroid.ui.component

private val SPACER_REGEX = Regex(
    """^\[([clr*]?)[sS][pP][aA][cC][eE][rR](\d*)\](.*)"""
)

enum class SpacerType { LEFT, CENTER, RIGHT, REPEAT }

data class SpacerInfo(
    val type: SpacerType,
    val displayText: String,
)

fun parseSpacer(name: String): SpacerInfo? {
    val match = SPACER_REGEX.find(name) ?: run {
        dev.tsdroid.AppLogger.d("SpacerParser", "NO MATCH: '$name'")
        return null
    }
    val prefix = match.groupValues[1].lowercase()
    val type = when (prefix) {
        "c" -> SpacerType.CENTER
        "l" -> SpacerType.LEFT
        "r" -> SpacerType.RIGHT
        "*" -> SpacerType.REPEAT
        "" -> SpacerType.LEFT
        else -> {
            dev.tsdroid.AppLogger.d("SpacerParser", "UNKNOWN PREFIX '$prefix' in: '$name'")
            return null
        }
    }
    val text = match.groupValues[3]
    dev.tsdroid.AppLogger.d("SpacerParser", "MATCH: '$name' → $type text='$text'")
    return SpacerInfo(type, text)
}
