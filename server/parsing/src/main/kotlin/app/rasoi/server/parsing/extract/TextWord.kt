package app.rasoi.server.parsing.extract

/** A word on a page in PDF points; [y] is the top edge measured from the top of the page. */
data class TextWord(val text: String, val x: Float, val y: Float, val width: Float, val height: Float) {
    val right: Float get() = x + width
    val centerY: Float get() = y + height / 2
}
