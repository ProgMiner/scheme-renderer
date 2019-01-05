/* MIT License

Copyright (c) 2019 Eridan Domoratskiy

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE. */

package ru.byprogminer.SchemeRenderer

import ru.byprogminer.SchemeRenderer.util.Dimension
import java.awt.*
import kotlin.math.roundToInt

class GOSTNodeRenderer: NodeRenderer {

    companion object {

        val BG_COLOR = Color.WHITE!!
        val FG_COLOR = Color.BLACK!!

        val SIGNS = mapOf(
            Node.BUF::class.java to "1",
            Node.AND::class.java to "&",
            Node.OR::class.java to "1",
            Node.XOR::class.java to "=1"
        )
    }

    var font: Font = Font.decode("Courier New")

    override val variableWidth = 2

    override fun getNodeSize(node: Node) =
        Dimension(2, maxOf(3, 1 + (node.inputs.size / 2) * 2))

    override fun renderNode(node: RenderedNode, graphics: Graphics, zoom: Double) {
        val oldColor = graphics.color
        val oldClip = graphics.clip
        val oldFont = graphics.font

        val oldRenderingHint = if (graphics is Graphics2D) {
            graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
        } else { null }

        if (graphics is Graphics2D) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }

        drawFilledRect(graphics,
            (node.position.x * zoom).roundToInt(),
            (node.position.y * zoom).roundToInt(),
            (node.size.x * zoom).roundToInt(),
            (node.size.y * zoom).roundToInt()
        )

        var sign: String? = SIGNS[node.node::class.java]
        if (sign == null) {
            for ((nodeType, nodeSign) in SIGNS) {
                if (nodeType.isInstance(node.node)) {
                    sign = nodeSign
                    break
                }
            }
        }

        if (sign != null) {
            graphics.font = font.deriveFont(Font.BOLD, (zoom * 3 / 4).toFloat())

            val fontMetrics = graphics.fontMetrics
            graphics.drawString(sign,
                (node.position.x * zoom + 1.85 * zoom - fontMetrics.stringWidth(sign)).roundToInt(),
                (node.position.y * zoom + fontMetrics.height).roundToInt()
            )
        }

        if (node.node.invertedOutput) {
            graphics.setClip(
                (node.position.x * zoom).roundToInt(),
                (node.position.y * zoom).roundToInt(),
                (node.size.x * zoom + (zoom + 1) / 2 - 1).roundToInt(),
                (node.size.y * zoom - 1).roundToInt()
            )

            drawFilledCircle(graphics,
                ((node.position.x + node.size.x) * zoom - 1).roundToInt(),
                (node.position.y * zoom + (node.size.y * zoom + 1) / 2 - 1).roundToInt(),
                zoom / 4
            )
        }

        graphics.font = oldFont
        graphics.clip = oldClip
        graphics.color = oldColor

        if (graphics is Graphics2D) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldRenderingHint)
        }
    }

    private fun drawFilledRect(graphics: Graphics, x: Int, y: Int, width: Int, height: Int) {
        graphics.color = BG_COLOR
        graphics.fillRect(x, y, width - 1, height - 1)

        graphics.color = FG_COLOR
        graphics.drawRect(x, y, width - 1, height - 1)
    }

    private fun drawFilledCircle(graphics: Graphics, x: Int, y: Int, radius: Double) {
        val diameter = (radius * 2).roundToInt()

        graphics.color = BG_COLOR
        graphics.fillOval((x - radius).roundToInt(), (y - radius).roundToInt(), diameter, diameter)

        graphics.color = FG_COLOR
        graphics.drawOval((x - radius).roundToInt(), (y - radius).roundToInt(), diameter, diameter)
    }
}