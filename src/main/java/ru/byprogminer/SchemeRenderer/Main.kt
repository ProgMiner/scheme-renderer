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

fun main() {
    val phi = Node(mutableSetOf(
        Variable("x1"),
        Variable("x2")
    ))

    val scheme = setOf(
        Node(mutableSetOf(
            Node(mutableSetOf()),
            Node(mutableSetOf(
                Node(mutableSetOf(
                    Variable("A")
                )),
                Node(mutableSetOf(
                    phi,
                    Node(mutableSetOf())
                )),
                Node(mutableSetOf()),
                phi
            )),
            Node(mutableSetOf(
                Node(mutableSetOf()),
                Node(mutableSetOf()),
                phi
            )),
            phi
        ))
    )

    val renderer = Renderer()
    val (size, nodes) = renderer.render(scheme)

    val canvas = Array(size.y) { Array<RenderedNode?> (size.x) { null } }
    for (node in nodes) {
        for (y in node.position.y until node.position.y + node.size.y) {
            for (x in node.position.x until node.position.x + node.size.x) {
                canvas[y][x] = node
            }
        }
    }

    for (y in canvas.indices) {
        for (x in canvas[y].indices) {
            if (canvas[y][x] != null) {
                print("XX")
            } else {
                print("  ")
            }
        }

        println()
    }
}
