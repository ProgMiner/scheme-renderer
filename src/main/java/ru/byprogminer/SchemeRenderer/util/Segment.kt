package ru.byprogminer.SchemeRenderer.util

class Segment(private val range: IntRange):
    ClosedRange<Int> by range,
    Iterable<Int> by range
