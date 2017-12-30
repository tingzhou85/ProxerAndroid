package me.proxer.app.ui.view.bbcode.table

import me.proxer.app.ui.view.bbcode.BBPrototype
import me.proxer.app.ui.view.bbcode.BBTree
import kotlin.text.RegexOption.IGNORE_CASE

/**
 * @author Ruben Gees
 */
object TablePrototype : BBPrototype {

    override val startRegex = Regex("\\s*table\\s*", IGNORE_CASE)
    override val endRegex = Regex("/\\s*table\\s*", IGNORE_CASE)

    override fun construct(code: String, parent: BBTree) = TableTree(parent)
}
