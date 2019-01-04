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

abstract class Node(
    val inputs: MutableSet<NodeInput> = mutableSetOf(),
    val invertedOutput: Boolean = false,
    val name: String? = null
): NodeInput {

    open class BUF(
        input: NodeInput,
        invertedOutput: Boolean = false,
        name: String? = null
    ): Node(mutableSetOf(input), invertedOutput, name) {

        constructor(input: NodeInput):
                this(input, false, null)

        constructor(input: NodeInput, invertedOutput: Boolean = false):
                this(input, invertedOutput, null)

        constructor(input: NodeInput, name: String? = null):
                this(input, false, name)
    }

    open class INV(
        input: NodeInput,
        name: String? = null
    ): BUF(input, true, name)

    open class AND(
        inputs: MutableSet<NodeInput> = mutableSetOf(),
        invertedOutput: Boolean = false,
        name: String? = null
    ): Node(inputs, invertedOutput, name) {

        constructor(inputs: MutableSet<NodeInput>):
                this(inputs, false, null)

        constructor(inputs: MutableSet<NodeInput>, invertedOutput: Boolean = false):
                this(inputs, invertedOutput, null)

        constructor(inputs: MutableSet<NodeInput>, name: String? = null):
                this(inputs, false, name)
    }

    open class NAND(
        inputs: MutableSet<NodeInput> = mutableSetOf(),
        name: String? = null
    ): AND(inputs, true, name)

    open class OR(
        inputs: MutableSet<NodeInput> = mutableSetOf(),
        invertedOutput: Boolean = false,
        name: String? = null
    ): Node(inputs, invertedOutput, name) {

        constructor(inputs: MutableSet<NodeInput>):
                this(inputs, false, null)

        constructor(inputs: MutableSet<NodeInput>, invertedOutput: Boolean = false):
                this(inputs, invertedOutput, null)

        constructor(inputs: MutableSet<NodeInput>, name: String? = null):
                this(inputs, false, name)
    }

    open class NOR(
        inputs: MutableSet<NodeInput> = mutableSetOf(),
        name: String? = null
    ): OR(inputs, true, name)

    open class XOR(
        inputs: MutableSet<NodeInput> = mutableSetOf(),
        invertedOutput: Boolean = false,
        name: String? = null
    ): Node(inputs, invertedOutput, name) {

        constructor(inputs: MutableSet<NodeInput>):
                this(inputs, false, null)

        constructor(inputs: MutableSet<NodeInput>, invertedOutput: Boolean = false):
                this(inputs, invertedOutput, null)

        constructor(inputs: MutableSet<NodeInput>, name: String? = null):
                this(inputs, false, name)
    }

    open class XNOR(
        inputs: MutableSet<NodeInput> = mutableSetOf(),
        name: String? = null
    ): XOR(inputs, true, name)
}
