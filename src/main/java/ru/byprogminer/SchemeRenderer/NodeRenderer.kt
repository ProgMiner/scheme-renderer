package ru.byprogminer.SchemeRenderer

import ru.byprogminer.SchemeRenderer.util.Dimension
import java.awt.Graphics

interface NodeRenderer {

    val variableWidth: Int

    fun getNodeSize(node: Node): Dimension
    fun renderNode(node: RenderedNode, graphics: Graphics, zoom: Double)
}