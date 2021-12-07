package com.skygolf.detector

class TapDetectorBuilder {
    private var avrFactor: Float = 0.6f
    private var minDuration: Long = 7000000
    private var maxDuration: Long = 150000000
    private var minAccel: Float = 1f
    private var maxAccel: Float = 5f
    private var maxCos: Float = -0.1f

    fun avrFactor(v: Float): TapDetectorBuilder {
        avrFactor = v
        return this
    }

    fun duration(minDuration: Long, maxDuration: Long): TapDetectorBuilder {
        this.minDuration = minDuration
        this.maxDuration = maxDuration
        return this
    }

    fun accelerationLimit(minAccel: Float, maxAccel: Float): TapDetectorBuilder {
        this.minAccel = minAccel
        this.maxAccel = maxAccel
        return this
    }

    fun build() = TapDetector(avrFactor, minDuration, maxDuration, minAccel, maxAccel, maxCos)
}