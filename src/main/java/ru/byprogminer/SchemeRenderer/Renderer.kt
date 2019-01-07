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

import ru.byprogminer.SchemeRenderer.util.*
import java.awt.Graphics
import java.awt.Image
import java.awt.image.BufferedImage
import java.util.*
import kotlin.math.roundToInt

class Renderer {

    var hGap = 3
    var vGap = 1

    var linesWidth = 1

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
        renderLines(graphics, zoom)

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

    private fun renderLines(graphics: Graphics, zoom: Double) {
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
        val emptyAreas = mutableListOf<TreeMap<Double, Int>>()

        // The list of lists of areas, that is a triple of center, top and bottom (inclusive)
        val areas = mutableListOf<List<Triple<Double, Double, Double>>>()
        for (depth in columns.indices) {
            val grid = Array(height) { false }

            for (node in columns[depth]) {
                val renderedNode = nodes[node]!!

                for (y in renderedNode.position.y until renderedNode.position.y + renderedNode.size.y) {
                    grid[y] = true
                }
            }

            var currentAreaStart = 0
            val currentAreas = mutableListOf<Triple<Double, Double, Double>>()
            val currentEmptyAreas = TreeMap<Double, Int>()
            for (y in grid.indices) {
                if (currentAreas.size % 2 == (if (grid[y]) { 0 } else { 1 })) {
                    currentAreas.add(Triple((currentAreaStart.toDouble() + y - 1) / 2, currentAreaStart.toDouble(), (y - 1).toDouble()))
                    currentAreaStart = y
                }

                if (currentAreas.size % 2 == 0) {
                    currentEmptyAreas[y.toDouble()] = currentAreas.size
                }
            }

            currentAreas.add(Triple((currentAreaStart.toDouble() + grid.lastIndex) / 2, currentAreaStart.toDouble(), grid.lastIndex.toDouble()))

            emptyAreas.add(currentEmptyAreas)
            areas.add(currentAreas)
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
            val areasNodes = Array(columns.size - 1) { Array(areas[it].size) { mutableSetOf<Int>() } }
            for (output in nodeOutputs) {
                val finishGap = depths[output]!! + 1
                val finishArea = columns[depths[output]!!].indexOf(output) * 2 + 1
                val finishY = areas[finishGap - 1][finishArea].first

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
                    y = areas[currentGap][currentArea].first

                    val currentNode = LineNode(currentArea)
                    if (prevNode == lineNode) {
                        lineNodes.add(currentNode)
                    }

                    prevNode.continues.add(currentNode)
                    prevNode = currentNode

                    areasNodes[currentGap][currentArea].add(lineNodes.lastIndex)
                }
            }

            val connectedLines = mutableListOf<Int>()
            for (gap in 1 until areasNodes.size) {
                for (areaNodes in areasNodes[gap].indices) {
                    val lineNodesInArea = areasNodes[gap][areaNodes]

                    if (lineNodesInArea.size < 2) {
                        continue
                    }

                    val iterator = lineNodesInArea.iterator()
                    val first = iterator.next()

                    val firstLineNode = lineNodes[first].findValue(areaNodes)!!
                    while (iterator.hasNext()) {
                        val current = iterator.next()

                        if (current in connectedLines) {
                            continue
                        }

                        val currentLineNode = lineNodes[current]
                        lineNode.continues.remove(currentLineNode)

                        firstLineNode.continues.addAll(currentLineNode.findValue(areaNodes)!!.continues)
                        connectedLines.add(current)
                    }
                }
            }

            lines.add(Pair(startDepth, lineNode))
            linesByStart[node] = lines.lastIndex
        }

        // First not-busy input numbers of nodes: from first half and second half of node
        val busyInputs = mutableMapOf<Node, Pair<Int, Int>>()
        for (depth in 0 until columns.lastIndex) {
            for (node in columns[depth]) {
                busyInputs[node] = Pair(0, node.inputs.size / 2)
            }
        }

        val gapsSegments = Array(columns.size) { mutableSetOf<Segment>() }
        val gapsSegmentsAreas = Array(columns.size) { mutableMapOf<Segment, MutableSet<Int>>() }

        // Horizontal lines in areas, this is a triple of start and end segments and y coordinate of line
        val areasSegments = Array(columns.size) { Array(areas[it].size) { mutableSetOf<Triple<Segment, Segment, Double>>() } }
        val segmentsContinues = Array(columns.size) { mutableMapOf<Segment, Set<Segment>>() }
        for ((startGap, lineNode) in lines) {
            fun next (gap: Int, lineNode: LineNode<Int>, outerPreferred: Double): Pair<Double, Segment?> {
                if (lineNode.value % 2 == 1 && lineNode.continues.isEmpty()) {
                    val renderedNode = nodes[columns[gap][lineNode.value / 2]]!!
                    val areaCenter = areas[gap][lineNode.value].first

                    val number: Int
                    val (firstHalf, secondHalf) = busyInputs[renderedNode.node]!!
                    if (
                        (outerPreferred < areaCenter && firstHalf < renderedNode.node.inputs.size / 2) ||
                        (outerPreferred >= areaCenter && secondHalf >= renderedNode.node.inputs.size)
                    ) {
                        busyInputs[renderedNode.node] = Pair(firstHalf + 1, secondHalf)
                        number = firstHalf
                    } else {
                        busyInputs[renderedNode.node] = Pair(firstHalf, secondHalf + 1)
                        number = secondHalf
                    }

                    return Pair(nodeRenderer.getNodeInputY(renderedNode, number).toDouble(), null)
                }

                val (_, areaTop, areaBottom) = areas[gap][lineNode.value]
                val inAreaPreferred = when {
                            (outerPreferred < areaTop) ->
                                areaTop
                            (outerPreferred > areaBottom) ->
                                areaBottom
                            else -> outerPreferred
                        }

                var top = inAreaPreferred
                var bottom = inAreaPreferred

                var contY = .0
                val segmentContinues = mutableSetOf<Segment>()
                val continuesY = mutableMapOf<Segment, Double>()
                for (cont in lineNode.continues) {
                    val ret = next(gap - 1, cont, inAreaPreferred)
                    contY = ret.first

                    val contSegment = ret.second
                    if (contSegment != null) {
                        segmentContinues.add(contSegment)
                        continuesY[contSegment] = contY
                    }

                    top = minOf(top, contY)
                    bottom = maxOf(bottom, contY)
                }

                val segment: Segment
                segment = Segment(
                    if (lineNode.continues.size == 1 && lineNode.continues.first().value % 2 == 0) {
                        val intContY = contY.roundToInt()

                        intContY..intContY
                    } else { top.roundToInt()..bottom.roundToInt() }
                )
                gapsSegments[gap].add(segment)

                for (cont in lineNode.continues) {
                    val constSegmentAreas = gapsSegmentsAreas[gap][segment]

                    if (constSegmentAreas == null) {
                        gapsSegmentsAreas[gap][segment] = mutableSetOf(cont.value)
                    } else {
                        constSegmentAreas.add(cont.value)
                    }
                }

                if (segmentContinues.isNotEmpty()) {
                    segmentsContinues[gap][segment] = segmentContinues

                    for (cont in segmentContinues) {
                        areasSegments[gap][lineNode.value].add(Triple(segment, cont, continuesY[cont]!!))
                    }
                }

                return Pair(
                    if (lineNode.value % 2 == 1) { outerPreferred }
                    else if (lineNode.continues.size == 1 && lineNode.continues.first().value % 2 == 0) { contY }
                    else { inAreaPreferred },
                    segment
                )
            }

            next(startGap, lineNode, areas[startGap][lineNode.value].first)
        }

        // TODO Fix inputs coordinates

        val gapsLinesGrids = Array(columns.size) { mutableListOf<Array<Boolean>>() }
        val gapsLinesSegments = Array(columns.size) { mutableMapOf<Segment, Int>() }
        for (gap in gapsSegments.indices) {
            processSegments@for (segment in gapsSegments[gap]) {
                search@for (line in gapsLinesGrids[gap].indices) {
                    val grid = gapsLinesGrids[gap][line]

                    for (y in segment) {
                        if (grid[y]) {
                            continue@search
                        }
                    }

                    for (y in segment) {
                        grid[y] = true
                    }

                    gapsLinesSegments[gap][segment] = line
                    continue@processSegments
                }

                gapsLinesGrids[gap].add(Array(height) { it in segment })
                gapsLinesSegments[gap][segment] = gapsLinesGrids[gap].lastIndex
            }
        }

        val gapsSegmentsEnds = Array(columns.size - 1) { Array(areas[it].size) { mutableListOf<Segment>() } }
        for (gap in gapsSegments.indices) {
            for (segment in gapsSegments[gap]) {
                val segmentAreas = gapsSegmentsAreas[gap][segment] ?: continue

                for (area in segmentAreas) {
                    gapsSegmentsEnds[gap - 1][area].add(segment)
                }
            }
        }

        var hOffset = width
        for (node in columns.first()) {
            if (node.name != null) {
                hOffset -= nodeRenderer.variableWidth
                break
            }
        }

        val gapsX = Array(columns.size) { -linesWidth.toDouble() / 2 }
        for (gap in 1..gapsX.lastIndex) {
            hOffset -= columnWidths[gap - 1]

            gapsX[gap] += hOffset - hGap.toDouble() / 2

            hOffset -= hGap
        }

        val gapsLinesSpaces = Array(columns.size) { linesWidth * zoom / (gapsLinesGrids[it].size + 1) }
        // TODO Vertical by horizontal
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