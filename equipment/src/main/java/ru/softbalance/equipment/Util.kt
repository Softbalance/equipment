package ru.softbalance.equipment

import android.view.View
import android.view.ViewConfiguration
import rx.Subscription
import java.math.BigDecimal

internal val ONE_HUNDRED = BigDecimal(100)

internal val LINE_SEPARATOR = getSystemProperty("line.separator")

fun getSystemProperty(property: String): String {
    try {
        return System.getProperty(property)
    } catch (ex: SecurityException) {
        // we are not allowed to look at this property
        System.err.println("Caught a SecurityException reading the system property '" + property
                + "'; the SystemUtils property value will default to null.")
        return "\n"
    }

}

internal fun String.toHttpUrl(port: Int): String {
    var url: String
    if (this.startsWith("http://")) {
        url = this
    } else {
        url = "http://" + this
    }

    if (!this.endsWith(":$port")) {
        url += ":$port"
    }

    return url
}

internal fun Subscription?.isActive(): Boolean {
    return this != null && !this.isUnsubscribed
}

internal fun Subscription?.isNonActive(): Boolean {
    return this == null || this.isUnsubscribed
}

internal class SingleClickListener(val click: (v: View) -> Unit) : View.OnClickListener {

    companion object {
        private val DOUBLE_CLICK_TIMEOUT = ViewConfiguration.getDoubleTapTimeout()
    }

    private var lastClick: Long = 0

    override fun onClick(v: View) {
        if (getLastClickTimeout() > DOUBLE_CLICK_TIMEOUT) {
            lastClick = System.currentTimeMillis()
            click(v)
        }
    }

    private fun getLastClickTimeout(): Long {
        return System.currentTimeMillis() - lastClick
    }
}

/**
 * Click listener setter that prevents double click on the view it´s set
 */
internal fun View.singleClick(l: (android.view.View?) -> Unit) {
    setOnClickListener(SingleClickListener(l))
}

internal var View.visible: Boolean
    get() {
        return this.visibility == View.VISIBLE
    }
    set(value) {
        if (value) {
            this.visibility = View.VISIBLE
        } else {
            this.visibility = View.GONE
        }
    }

/**
 *
 * Wraps a single line of text, identifying words by `' '`.
 *
 *
 * Leading spaces on a new line are stripped.
 * Trailing spaces are not stripped.
 *
 * <table border="1" summary="Wrap Results">
 * <tr>
 * <th>input</th>
 * <th>wrapLenght</th>
 * <th>newLineString</th>
 * <th>wrapLongWords</th>
 * <th>result</th>
</tr> *
 * <tr>
 * <td>null</td>
 * <td>*</td>
 * <td>*</td>
 * <td>true/false</td>
 * <td>null</td>
</tr> *
 * <tr>
 * <td>""</td>
 * <td>*</td>
 * <td>*</td>
 * <td>true/false</td>
 * <td>""</td>
</tr> *
 * <tr>
 * <td>"Here is one line of text that is going to be wrapped after 20 columns."</td>
 * <td>20</td>
 * <td>"\n"</td>
 * <td>true/false</td>
 * <td>"Here is one line of\ntext that is going\nto be wrapped after\n20 columns."</td>
</tr> *
 * <tr>
 * <td>"Here is one line of text that is going to be wrapped after 20 columns."</td>
 * <td>20</td>
 * <td>"&lt;br /&gt;"</td>
 * <td>true/false</td>
 * <td>"Here is one line of&lt;br /&gt;text that is going&lt;br /&gt;to be wrapped after&lt;br /&gt;20 columns."</td>
</tr> *
 * <tr>
 * <td>"Here is one line of text that is going to be wrapped after 20 columns."</td>
 * <td>20</td>
 * <td>null</td>
 * <td>true/false</td>
 * <td>"Here is one line of" + systemNewLine + "text that is going" + systemNewLine + "to be wrapped after" + systemNewLine + "20 columns."</td>
</tr> *
 * <tr>
 * <td>"Click here to jump to the commons website - http://commons.apache.org"</td>
 * <td>20</td>
 * <td>"\n"</td>
 * <td>false</td>
 * <td>"Click here to jump\nto the commons\nwebsite -\nhttp://commons.apache.org"</td>
</tr> *
 * <tr>
 * <td>"Click here to jump to the commons website - http://commons.apache.org"</td>
 * <td>20</td>
 * <td>"\n"</td>
 * <td>true</td>
 * <td>"Click here to jump\nto the commons\nwebsite -\nhttp://commons.apach\ne.org"</td>
</tr> *
</table> *
 *
 * @param str  the String to be word wrapped, may be null
 * @param wrapLength  the column to wrap the words at, less than 1 is treated as 1
 * @param newLineStr  the string to insert for a new line,
 * `null` uses the system property line separator
 * @param wrapLongWords  true if long words (such as URLs) should be wrapped
 * @return a line with newlines inserted, `null` if null input
 */
internal fun wrap(str: String?, paramWrapLength: Int, paramNewLineStr: String?, wrapLongWords: Boolean): String {
    var wrapLength = paramWrapLength
    var newLineStr = paramNewLineStr
    if (str == null) {
        return ""
    }
    if (newLineStr == null) {
        newLineStr = LINE_SEPARATOR
    }
    if (wrapLength < 1) {
        wrapLength = 1
    }
    val inputLineLength = str.length
    var offset = 0
    val wrappedLine = StringBuilder(inputLineLength + 32)

    while (inputLineLength - offset > wrapLength) {
        if (str[offset] == ' ') {
            offset++
            continue
        }
        var spaceToWrapAt = str.lastIndexOf(' ', wrapLength + offset)

        if (spaceToWrapAt >= offset) {
            // normal case
            wrappedLine.append(str.substring(offset, spaceToWrapAt))
            wrappedLine.append(newLineStr)
            offset = spaceToWrapAt + 1

        } else {
            // really long word or URL
            if (wrapLongWords) {
                // wrap really long word one line at a time
                wrappedLine.append(str.substring(offset, wrapLength + offset))
                wrappedLine.append(newLineStr)
                offset += wrapLength
            } else {
                // do not wrap really long word, just extend beyond limit
                spaceToWrapAt = str.indexOf(' ', wrapLength + offset)
                offset = if (spaceToWrapAt >= 0) {
                    wrappedLine.append(str.substring(offset, spaceToWrapAt))
                    wrappedLine.append(newLineStr)
                    spaceToWrapAt + 1
                } else {
                    wrappedLine.append(str.substring(offset))
                    inputLineLength
                }
            }
        }
    }

    // Whatever is left in line is short enough to just pass through
    wrappedLine.append(str.substring(offset))

    return wrappedLine.toString()
}
