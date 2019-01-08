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
import java.lang.Exception
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO
import javax.swing.*

fun main() {
    val scheme = run {
        val z1 = Node.AND(setOf(
            Variable("x1"),
            Variable("x4")
        ), Variable("z1"))

        val notZ1 = Node.OR(setOf(
            Variable("x1", true),
            Variable("x4", true)
        ), Variable("z1", true))

        val z2 = Node.AND(setOf(
            Variable("x1", true),
            Variable("x2")
        ), Variable("z2"))

        val z3 = Node.AND(setOf(
            Variable("x5"),
            Variable("x6", true)
        ), Variable("z3")
        )

        val z4 = Node.AND(setOf(
            Variable("x3", true),
            Variable("x5", true)
        ), Variable("z4"))

        val z5 = Node.AND(setOf(
            Variable("x2"),
            Variable("x5")
        ), Variable("z5"))

        val z6 = Node.AND(setOf(
            Variable("x5", true),
            Variable("x6", true)
        ), Variable("z6"))

        val z7 = Node.AND(setOf(
            Variable("x2", true),
            Variable("x5", true)
        ), Variable("z7"))

        val z8 = Node.AND(setOf(
            Variable("x3"),
            Variable("x6")
        ), Variable("z8"))

        val z9 = Node.AND(setOf(
            Variable("x3", true),
            Variable("x6", true)
        ), Variable("z9"))

        val w1 = Node.OR(setOf(
            notZ1,
            Variable("x3", true)
        ), Variable("w1"))

        val w2 = Node.OR(setOf(
            z4,
            z6,
            z7
        ), Variable("w2"))

        val y1 = Node.OR(setOf(
            Node.AND(setOf(
                Variable("x1"),
                Variable("x4", true),
                Node.OR(setOf(
                    z5,
                    Node.AND(setOf(
                        z8,
                        Node.OR(setOf(
                            Variable("x2"),
                            Variable("x5")
                        ))
                    ))
                ))
            )),
            Node.AND(setOf(
                z1,
                Node.OR(setOf(
                    z7,
                    Node.AND(setOf(
                        z9,
                        Node.OR(setOf(
                            Variable("x2", true),
                            Variable("x5", true)
                        ))
                    ))
                ))
            )),
            Node.AND(setOf(
                z2,
                Variable("x4"),
                Node.OR(setOf(
                    z3,
                    Node.AND(setOf(
                        Variable("x5", true),
                        z8
                    ))
                ))
            ))
        ), Variable("y1"))

        val y2 = Node.OR(setOf(
            Node.AND(setOf(
                Variable("x1", true),
                Variable("x4"),
                Node.OR(setOf(
                    w2,
                    Node.AND(setOf(
                        Variable("x2", true),
                        z3
                    ))
                ))
            )),
            Node.AND(setOf(
                Variable("x1"),
                Variable("x4", true),
                Node.OR(setOf(
                    w2,
                    Node.AND(setOf(
                        Variable("x2", true),
                        Node.OR(setOf(
                            Variable("x3", true),
                            Variable("x6", true)
                        ))
                    ))
                ))
            )),
            Node.AND(setOf(
                z2,
                Variable("x4", true),
                Node.OR(setOf(
                    Variable("x5"),
                    z8
                ))
            )),
            Node.AND(setOf(
                Variable("x5"),
                Variable("x6"),
                Node.OR(setOf(
                    z1,
                    Node.AND(setOf(
                        Variable("x1", true),
                        Variable("x3"),
                        Variable("x4", true)
                    ))
                ))
            ))
        ), Variable("y2")
        )

        val y3 = Node.OR(setOf(
            Node.AND(setOf(
                Node.OR(setOf(
                    Node.AND(setOf(
                        Variable("x2", true),
                        z3
                    )),
                    Node.AND(setOf(
                        Variable("x2"),
                        z6
                    ))
                )),
                w1
            )),
            Node.AND(setOf(
                z5,
                Node.OR(setOf(
                    z8,
                    Node.AND(setOf(
                        Variable("x4"),
                        Variable("x6")
                    )),
                    Node.AND(setOf(
                        Variable("x3"),
                        z1
                    ))
                ))
            )),
            Node.AND(setOf(
                Variable("x2", true),
                Variable("x3", true),
                Variable("x4", true),
                Variable("x5")
            )),
            Node.AND(setOf(
                z4,
                Node.OR(setOf(
                    Node.AND(setOf(
                        Variable("x2"),
                        Variable("x4", true)
                    ))
                ))
            )),
            Node.AND(setOf(
                z7, z8
            ))
        ), Variable("y3"))

        val y4 = Node.OR(setOf(
            Node.AND(setOf(
                Variable("x3", true),
                Variable("x6"),
                Node.OR(setOf(
                    Variable("x4", true),
                    z7,
                    Node.AND(setOf(
                        Variable("x1", true),
                        Variable("x5", true)
                    ))
                ))
            )),
            Node.AND(setOf(
                Variable("x3"),
                Variable("x6", true),
                Node.OR(setOf(
                    z7,
                    notZ1
                ))
            )),
            Node.AND(setOf(
                z8,
                Node.OR(setOf(
                    Node.AND(setOf(
                        Variable("x4"),
                        Variable("x5")
                    )),
                    Node.AND(setOf(
                        Variable("x2"),
                        z1
                    ))
                ))
            )),
            Node.AND(setOf(
                z1,
                z5,
                z9
            ))
        ), Variable("y4"))

        val y5 = Node.OR(setOf(
            Node.AND(setOf(
                z1,
                Variable("x2"),
                Node.OR(setOf(
                    Node.AND(setOf(
                        Variable("x5", true),
                        Variable("x6")
                    )),
                    z3,
                    Node.AND(setOf(
                        Variable("x3"),
                        Variable("x5", true)
                    ))
                ))
            )),
            Node.AND(setOf(
                z1,
                z3,
                Variable("x3")
            ))
        ), Variable("y5"))

        setOf(y1, y2, y3, y4, y5)
    }

    val renderer = Renderer()
    renderer.linesWidth = 5
    renderer.hGap = 8

    renderer.render(scheme)

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

            val saveButton = JButton("Save to file")
            saveButton.addActionListener {
                CompletableFuture.runAsync {
                    val fileChooser = JFileChooser("Choose path")

                    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        val file = fileChooser.selectedFile

                        if (
                            !file.exists() ||
                            JOptionPane.showConfirmDialog(this, "Do you want to rewrite file ${file.name}?", "", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION
                        ) {
                            ImageIO.write(renderer.renderWith(50.0) as BufferedImage, "PNG", file)
                        }
                    }
                }
            }

            contentPane.add(saveButton)

            pack()
            isVisible = true
        }
    }
}
