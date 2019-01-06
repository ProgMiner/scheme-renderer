package ru.byprogminer.SchemeRenderer.util

data class LineNode<V>(
    val value: V,
    val continues: MutableSet<LineNode<V>> = mutableSetOf()
) {

    /**
     * Finds value in node and continues
     *
     * @return Node with value or null
     */
    fun findValue(value: V): LineNode<V>? {
        if (this.value == value) {
            return this
        }

        for (cont in continues) {
            val ret = cont.findValue(value)

            if (ret != null) {
                return ret
            }
        }

        return null
    }
}