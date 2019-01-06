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
import ru.byprogminer.SchemeRenderer.util.LineNode
import ru.byprogminer.SchemeRenderer.util.Position
import ru.byprogminer.SchemeRenderer.util.nearestEntry
import java.awt.Graphics
import java.awt.Image
import java.awt.image.BufferedImage
import java.util.*
import kotlin.math.roundToInt

class Renderer {

    var hGap = 3
    var vGap = 1

    var nodeRenderer: NodeRenderer = GOSTNodeRenderer()

    var width: Int
        get() = size.x
        private set(value) {
            size.x = value
        }

    var height: Int
        get() = size.y
        private set(value) {
            size.y = value
        }

    val renderedNodes: Set<RenderedNode>
        get() = _renderedNodes.toSet()

    private val columns = mutableListOf<MutableList<Node>>()

    private lateinit var columnWidths: List<Int>
    private lateinit var size: Dimension

    private val nodes = mutableMapOf<Node, RenderedNode>()

    private lateinit var _renderedNodes: Set<RenderedNode>

    fun render(nodes: Set<Node>): Renderer {
        renderNodes(nodes)

        val ret = mutableSetOf<RenderedNode>()
        for ((_, renderedNode) in this.nodes) {
            ret.add(renderedNode)
        }

        _renderedNodes = ret.toSet()
        return this
    }

    fun renderOn(canvas: Image, zoom: Double? = null): Renderer {
        if (!this::_renderedNodes.isInitialized) {
            throw RuntimeException("Nothing to render")
        }

        @Suppress("NAME_SHADOWING")
        val zoom = zoom ?: minOf(
            canvas.getWidth(null).toDouble() / width,
            canvas.getHeight(null).toDouble() / height
        )

        val graphics = canvas.graphics
        renderLines(graphics)

        for (node in _renderedNodes) {
            graphics.setClip(
                (node.position.x * zoom).roundToInt(),
                (node.position.y * zoom).roundToInt(),
                (node.size.x * zoom).roundToInt(),
                (node.size.y * zoom).roundToInt()
            )

            nodeRenderer.renderNode(node, graphics, zoom)
        }

        return this
    }

    fun renderWith(zoom: Double): Image {
        if (!this::_renderedNodes.isInitialized) {
            throw RuntimeException("Nothing to render")
        }

        val canvas = BufferedImage(
            (width * zoom).roundToInt(),
            (height * zoom).roundToInt(),
            BufferedImage.TYPE_INT_ARGB
        )

        renderOn(canvas, zoom)

        return canvas
    }

    private fun getVerticalCenterOfNode(node: RenderedNode) =
        (node.position.y.toDouble() + (node.size.y - 1).toDouble() / 2)

    private fun renderLines(graphics: Graphics) {
        val outputs = mutableMapOf<Node, MutableSet<Node>>()
        val depths = mutableMapOf<Node, Int>()

        for (depth in columns.indices) {
            for (node in columns[depth]) {
                depths[node] = depth
            }
        }

        for (node in _renderedNodes) {
            for (input in node.node.inputs) {
                if (input !is Node) {
                    continue
                }

                val nodeOutputs = outputs[input] ?: mutableSetOf()

                nodeOutputs.add(node.node)
                outputs[input] = nodeOutputs
            }
        }

        // The list of maps of numbers of empty areas on each gap
        //
        // Area  #   Gap
        //
        //  0      |     |
        // --------+-----+
        //     ####|     |####
        //  1  # 0#|     |####
        //     ####|     |####
        // --------+-----+####
        //  2      |     |####
        //
        val areasCenters = mutableListOf<List<Double>>()
        val emptyAreas = mutableListOf<TreeMap<Double, Int>>()
        for (depth in columns.indices) {
            val grid = Array(height) { false }

            for (node in columns[depth]) {
                val renderedNode = nodes[node]!!

                for (y in renderedNode.position.y until renderedNode.position.y + renderedNode.size.y) {
                    grid[y] = true
                }
            }

            var currentAreaStart = 0
            val currentAreasCenters = mutableListOf<Double>()
            val currentEmptyAreas = TreeMap<Double, Int>()
            for (y in grid.indices) {
                if (currentAreasCenters.size % 2 == (if (grid[y]) { 0 } else { 1 })) {
                    currentAreasCenters.add((currentAreaStart.toDouble() + y - 1) / 2)
                    currentAreaStart = y
                }

                if (currentAreasCenters.size % 2 == 0) {
                    currentEmptyAreas[y.toDouble()] = currentAreasCenters.size
                }
            }

            currentAreasCenters.add((currentAreaStart.toDouble() + grid.lastIndex) / 2)

            emptyAreas.add(currentEmptyAreas)
            areasCenters.add(currentAreasCenters)
        }

        // The list of lines, that is a pair of start gap and line node
        val lines = mutableListOf<Pair<Int, LineNode<Int>>>()
        val linesByStart = mutableMapOf<Node, Int>()
        for ((node, nodeOutputs) in outputs) {
            if (depths[node] == 0) {
                // As a precaution
                continue
            }

            val startDepth = depths[node]!!
            val lineNodes = mutableListOf<LineNode<Int>>()
            val lineNode = LineNode(columns[startDepth].indexOf(node) * 2 + 1)
            val areas = Array(columns.size - 1) { Array(areasCenters[it].size) { mutableSetOf<Int>() } }
            for (output in nodeOutputs) {
                val finishGap = depths[output]!! + 1
                val finishArea = columns[depths[output]!!].indexOf(output) * 2 + 1
                val finishY = areasCenters[finishGap - 1][finishArea]

                var prevNode = lineNode
                var currentGap = startDepth
                var y = getVerticalCenterOfNode(nodes[node]!!)
                while (true) {
                    if (currentGap == finishGap) {
                        prevNode.continues.add(LineNode(finishArea))
                        break
                    }

                    --currentGap
                    val currentArea = emptyAreas[currentGap].nearestEntry((y + finishY) / 2)!!
                    y = areasCenters[currentGap][currentArea]

                    val currentNode = LineNode(currentArea)
                    if (prevNode == lineNode) {
                        lineNodes.add(currentNode)
                    }

                    prevNode.continues.add(currentNode)
                    prevNode = currentNode

                    areas[currentGap][currentArea].add(lineNodes.lastIndex)
                }
            }

            val connectedLines = mutableListOf<Int>()
            for (gap in 1 until areas.size) {
                for (area in areas[gap].indices) {
                    val lineNodesInArea = areas[gap][area]

                    if (lineNodesInArea.size < 2) {
                        continue
                    }

                    val iterator = lineNodesInArea.iterator()
                    val first = iterator.next()

                    val firstLineNode = lineNodes[first].findValue(area)!!
                    while (iterator.hasNext()) {
                        val current = iterator.next()

                        if (current in connectedLines) {
                            continue
                        }

                        val currentLineNode = lineNodes[current]
                        lineNode.continues.remove(currentLineNode)

                        firstLineNode.continues.addAll(currentLineNode.findValue(area)!!.continues)
                        connectedLines.add(current)
                    }
                }
            }

            lines.add(Pair(startDepth, lineNode))
            linesByStart[node] = lines.lastIndex
        }

        /*
        // Counters of horizontal lines in areas
        val areasCounters = Array<Array<out Int>>(columns.size) { Array(areasCenters[it].size) { 0 } }

        // Line segments on each gap
        val gapsSegments = Array<List<IntRange>>(columns.size) { listOf() }
        for ((startGap, start, paths) in lines) {
            for (ends in paths) {
                var gap = startGap

                var prevY = areasCenters[gap][start]
                for (end in ends) {
                    --gap

                    val y = areasCenters[gap][end]

                    top = minOf(top, y)
                    bottom = maxOf(bottom, y)
                }
            }
        }
        */
    }

    private fun renderNodes(nodes: Set<Node>) {
        for (node in nodes) {
            fillColumns(node, 0)
        }

        horizontalAlign()
        cleanRepeats()

        calculateSize()
        fillNodes()

        verticalAlignRight()
        verticalAlignLeft()

        recalculateHeight()
        for (i in columns.indices) {
            resolveCollisions(i)
        }

        recalculateHeight()
        verticalAlignRight()
    }

    private fun fillColumns(node: Node, depth: Int) {
        if (columns.size <= depth) {
            columns.add(mutableListOf(node))
        } else {
            columns[depth].add(node)
        }

        for (input in node.inputs) {
            if (input is Node) {
                fillColumns(input, depth + 1)
            }
        }
    }

    private fun getNodeInputs(node: Node) =
        node.inputs.filterIsInstance<Node>()

    private fun horizontalAlign() {
        if (columns.isEmpty()) {
            return
        }

        val minDepths = mutableMapOf<Node, Int>()

        for (node in columns.last()) {
            minDepths[node] = columns.lastIndex
        }

        for (i in columns.lastIndex - 1 downTo 0) {
            val col = columns[i]

            val newCol = mutableListOf<Node>()
            for (node in col) {
                val depth: Int

                if (getNodeInputs(node).isEmpty()) {
                    columns.last().add(node)
                    depth = columns.lastIndex
                } else {
                    val minInputDepth = minDepths[
                            getNodeInputs(node).minBy {
                                minDepths[it]!!
                            }
                    ]!!
                    depth = minInputDepth - 1

                    if (depth == i) {
                        newCol.add(node)
                    } else {
                        columns[depth].add(node)
                    }
                }

                minDepths[node] = minOf(minDepths[node] ?: depth, depth)
            }

            columns[i] = newCol
        }
    }

    private fun cleanRepeats() {
        for (depth in columns.indices) {
            val col = columns[depth]

            repeat@while (true) {
                for (i in col.indices) {
                    for (j in (i + 1)..col.lastIndex) {
                        if (col[i] === col[j]) {
                            col.removeAt(j)
                            continue@repeat
                        }
                    }
                }

                break
            }
        }
    }

    private fun calculateSize() {
        var maxHeight = 0

        val widths = mutableListOf<Int>()
        for (col in columns) {
            var maxWidth = 0
            var height = 0

            for (node in col) {
                val size = nodeRenderer.getNodeSize(node)

                maxWidth = maxOf(maxWidth, size.x)
                height += size.y + vGap
            }

            widths.add(maxWidth)
            maxHeight = maxOf(maxHeight, height)
        }

        var haveVariablesAtStart = false
        for (node in columns.first()) {
            if (node.name != null) {
                haveVariablesAtStart = true
                break
            }
        }

        var haveVariablesAtEnd = false
        for (node in columns.last()) {
            for (input in node.inputs) {
                if (input is Variable) {
                    haveVariablesAtEnd = true
                    break
                }
            }
        }

        columnWidths = widths.toList()
        size = Dimension(
            widths.sum() + hGap * (widths.size - 1) +
                    if (haveVariablesAtStart) { nodeRenderer.variableWidth } else { 0 } +
                    if (haveVariablesAtEnd) { nodeRenderer.variableWidth } else { 0 },
            maxHeight + vGap
        )
    }

    private fun fillNodes() {
        var hOffset = width

        for (node in columns.first()) {
            if (node.name != null) {
                hOffset -= nodeRenderer.variableWidth
                break
            }
        }

        columns.forEachIndexed { depth, col ->
            hOffset -= columnWidths[depth]

            var vOffset = vGap
            for (node in col) {
                val size = nodeRenderer.getNodeSize(node)

                nodes[node] = RenderedNode(node, size, Position(hOffset, vOffset))
                vOffset += size.y + vGap
            }

            hOffset -= hGap
        }
    }

    private fun calculateAverageHeight(nodes: Set<Node>): Int {
        var sum = .0

        for (node in nodes) {
            sum += getVerticalCenterOfNode(this.nodes[node]!!)
        }

        return (sum / nodes.size).roundToInt()
    }

    private fun resolveCollisions(depth: Int) {
        val col = columns[depth]

        repeat@while(true) {
            val grid = Array<Node?>(height) { null }
            val collision = Array<Node?>(height) { null }

            for (node in col) {
                val renderedNode = nodes[node]!!

                for (y in renderedNode.position.y until renderedNode.position.y + renderedNode.size.y + vGap) {
                    try {
                        if (grid[y] != null) {
                            collision[y] = node
                        } else {
                            grid[y] = node
                        }
                    } catch (e: IndexOutOfBoundsException) {}
                }
            }

            for (i in collision.indices) {
                val nodeB = collision[i] ?: continue
                val nodeA = grid[i]!!

                val renderedNodeA = nodes[nodeA]!!
                val renderedNodeB = nodes[nodeB]!!

                val top = renderedNodeA.position.y
                val bottom = renderedNodeB.position.y + renderedNodeB.size.y - 1
                var center = (top * nodeA.inputs.size + bottom * nodeB.inputs.size).toDouble() / (nodeA.inputs.size + nodeB.inputs.size)
                val height = renderedNodeA.size.y.toDouble() + vGap + renderedNodeB.size.y

                if (center.isNaN()) {
                    center = (top + bottom).toDouble() / 2
                }

                renderedNodeA.position.y = (center - height / 2).roundToInt()
                renderedNodeB.position.y = (center + height / 2 + 1 - renderedNodeB.size.y).roundToInt()

                if (renderedNodeA.position.y < 0) {
                    val offset = -renderedNodeA.position.y

                    renderedNodeA.position.y = 0
                    renderedNodeB.position.y += offset
                }

                continue@repeat
            }

            break
        }
    }

    private fun verticalAlignRight() {
        for (depth in columns.lastIndex - 1 downTo 0) {
            val col = columns[depth]

            for (node in col) {
                val renderedNode = nodes[node]!!

                renderedNode.position.y = maxOf(0, calculateAverageHeight(getNodeInputs(node).toSet()) - renderedNode.size.y / 2)
            }

            resolveCollisions(depth)
        }
    }

    private fun verticalAlignLeft() {
        for (depth in 0 until columns.lastIndex) {
            val col = columns[depth]

            for (node in col) {
                val renderedNode = nodes[node]!!

                val center = renderedNode.position.y + renderedNode.size.y / 2
                val inputsCenter = calculateAverageHeight(getNodeInputs(node).toSet())

                var offset = center - inputsCenter
                if (offset != 0) {
                    for (input in node.inputs) {
                        if (input is Node) {
                            val inputRenderedNode = nodes[input]!!
                            inputRenderedNode.position.y += offset

                            if (inputRenderedNode.position.y < 0) {
                                offset -= inputRenderedNode.position.y

                                inputRenderedNode.position.y = 0
                            }
                        }
                    }
                }
            }
        }
    }

    private fun recalculateHeight() {
        var height = 0

        for (col in columns) {
            val maxNode = nodes[col.maxBy { nodes[it]!!.position.y }]!!

            height = maxOf(height, maxNode.position.y + maxNode.size.y)
        }

        this.height = height + vGap
    }
}