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

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel

fun main() {
    val phi = Node.OR(mutableSetOf(
        Variable("x1"),
        Variable("x2")
    ), invertedOutput = true)

    val scheme = setOf(
        Node.AND(mutableSetOf(
            Node.XOR(),
            Node.XNOR(mutableSetOf(
                Node.BUF(Variable("A")),
                Node.NOR(mutableSetOf(
                    phi,
                    Node.NAND(mutableSetOf())
                )),
                Node.AND(mutableSetOf()),
                phi
            )),
            Node.XOR(mutableSetOf(
                Node.XOR(mutableSetOf()),
                Node.AND(mutableSetOf()),
                phi
            )),
            phi
        ), false, "f")
    )

    val renderer = Renderer()
        .render(scheme)

    val rendered = renderer.renderWith(50.0)

    object: JFrame() {
        init {
            defaultCloseOperation = EXIT_ON_CLOSE

            contentPane = object: JPanel() {
                override fun paintComponent(graphics: Graphics) {
                    super.paintComponent(graphics)

                    val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
                    renderer.renderOn(img)

                    graphics.drawImage(img, 0, 0, width, height, null)
                }
            }

            contentPane.background = Color.decode("#086405")
            contentPane.preferredSize = Dimension(rendered.getWidth(null), rendered.getHeight(null))

            pack()
            isVisible = true
        }
    }
}
