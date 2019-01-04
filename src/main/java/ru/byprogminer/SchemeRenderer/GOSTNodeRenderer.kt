package ru.byprogminer.SchemeRenderer

import ru.byprogminer.SchemeRenderer.util.Dimension
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import kotlin.math.roundToInt

class GOSTNodeRenderer: NodeRenderer {

    companion object {

        val BG_COLOR = Color.WHITE!!
        val FG_COLOR = Color.BLACK!!
    }

    override val variableWidth = 2

    override fun getNodeSize(node: Node) =
        Dimension(2, maxOf(3, 1 + (node.inputs.size / 2) * 2))

    override fun renderNode(node: RenderedNode, graphics: Graphics, zoom: Double) {
        val oldColor = graphics.color
        val oldClip = graphics.clip

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