package com.skygolf.detector

class MultiTapDetectorBuilder {
    private var maxDelay: Long = 500

    fun maxDelay(maxDelay: Long): MultiTapDetectorBuilder {
        this.maxDelay = maxDelay
        return this
    }

    fun build() = MultiTapDetector(maxDelay)
}