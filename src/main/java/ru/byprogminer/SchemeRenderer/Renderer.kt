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
import ru.byprogminer.SchemeRenderer.util.Position
import kotlin.math.roundToInt

class Renderer {

    var hGap = 3
    var vGap = 1

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

    private val columns = mutableListOf<MutableList<Node>>()
    private val outputs = mutableMapOf<Node, MutableSet<Node>>()

    private lateinit var columnWidths: List<Int>
    private lateinit var size: Dimension

    private val nodes = mutableMapOf<Node, RenderedNode>()

    infix fun render(nodes: Set<Node>): Pair<Dimension, Set<RenderedNode>> {
        renderNodes(nodes)

        val ret = mutableSetOf<RenderedNode>()
        for ((_, renderedNode) in this.nodes) {
            ret.add(renderedNode)
        }

        return Pair(size.copy(), ret.toSet())
    }

    fun getSize(node: Node) =
        Dimension(2, maxOf(3, 1 + (node.inputs.size / 2) * 2))

    private fun renderNodes(nodes: Set<Node>) {
        for (node in nodes) {
            fillColumns(node, 0)
        }

        horizontalAlign()
        cleanRepeats()

        calculateSize()
        fillNodes()

        verticalAlign()
    }

    private fun fillColumns(node: Node, depth: Int) {
        if (columns.size <= depth) {
            columns.add(mutableListOf(node))
        } else {
            columns[depth].add(node)
        }

        for (input in node.inputs) {
            fillColumns(input, depth + 1)

            val inputOutputs = outputs[input]
            inputOutputs?.add(node)

            outputs[input] = inputOutputs ?: mutableSetOf(node)
        }
    }

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

                if (node.inputs.isEmpty()) {
                    columns.last().add(node)
                    depth = columns.lastIndex
                } else {
                    val minInputDepth = minDepths[node.inputs.minBy {
                        minDepths[it]!!
                    }]!!
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
                val size = getSize(node)

                maxWidth = maxOf(maxWidth, size.x)
                height += size.y + vGap
            }

            widths.add(maxWidth)
            maxHeight = maxOf(maxHeight, height)
        }

        columnWidths = widths.toList()
        size = Dimension(
            widths.sum() + hGap * (widths.size - 1),
            maxHeight - vGap
        )
    }

    private fun fillNodes() {
        var hOffset = width

        columns.forEachIndexed { depth, col ->
            hOffset -= columnWidths[depth]

            var vOffset = 0
            for (node in col) {
                val size = getSize(node)

                nodes[node] = RenderedNode(node, size, Position(hOffset, vOffset))
                vOffset += size.y + vGap
            }

            hOffset -= hGap
        }
    }

    private fun verticalAlign() {
        for (depth in columns.lastIndex - 1 downTo 0) {
            val col = columns[depth]

            for (node in col) {
                val top = nodes[node.inputs.minBy { nodes[it]!!.position.y }]!!.position.y
                val bottomNode = nodes[node.inputs.maxBy { nodes[it]!!.position.y }]!!
                val bottom = bottomNode.position.y + bottomNode.size.y - 1
                val center = (top + bottom) / 2

                val renderedNode = nodes[node]!!
                renderedNode.position.y = center - renderedNode.size.y / 2
            }

            repeat@while(true) {
                val grid = Array<Node?>(height) { null }
                val collision = Array<Node?>(height) { null }

                for (node in col) {
                    val renderedNode = nodes[node]!!

                    for (y in renderedNode.position.y until renderedNode.position.y + renderedNode.size.y + vGap) {
                        if (grid[y] != null) {
                            collision[y] = node
                        } else {
                            grid[y] = node
                        }
                    }
                }

                for (i in collision.indices) {
                    val nodeB = collision[i] ?: continue
                    val nodeA = grid[i]!!

                    val renderedNodeA = nodes[nodeA]!!
                    val renderedNodeB = nodes[nodeB]!!

                    val top = renderedNodeA.position.y
                    val bottom = renderedNodeB.position.y + renderedNodeB.size.y - 1
                    val center = (top * nodeA.inputs.size + bottom * nodeB.inputs.size).toDouble() / (nodeA.inputs.size + nodeB.inputs.size)
                    val height = renderedNodeA.size.y.toDouble() + vGap + renderedNodeB.size.y

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
    }
}