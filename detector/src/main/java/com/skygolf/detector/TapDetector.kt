package com.skygolf.detector

import kotlin.math.max
import kotlin.math.sqrt

/*
Allows to detect a single tap on device
0 < avrFactor < 1

 */
class TapDetector(
    private val avrFactor: Float = 0.6f,
    private val minDuration: Long = 65000000,
    private val maxDuration: Long = 130000000,
    private val minAccel: Float = 1f,
    private val maxAccel: Float = 5f,
    private val maxCos: Float = -0.4f) {

    private lateinit var listener: TapListener

    private var avrDx: Float? = null
    private var avrDy: Float? = null
    private var avrDz: Float? = null

    private val isInTap get() = recentMoves.isNotEmpty()

    private val recentMoves = ArrayList<Move>()

    private lateinit var currentMove: Move

    fun setListener(listener: TapListener) {
        this.listener = listener
    }

    fun registerAccel(time: Long, dx: Float, dy: Float, dz: Float) {
        avrDx = updateAvr(avrDx, dx)
        avrDy = updateAvr(avrDy, dy)
        avrDz = updateAvr(avrDz, dz)

        currentMove = Move(time, dx, dy, dz)

        if (currentMove.len > minAccel && currentMove.len < maxAccel) {
            recentMoves.add(currentMove)
        } else if (isInTap) {
            endTap()
        }
    }

    private fun endTap() {
        try {
            val startTime = recentMoves.last().time
            val endTime = recentMoves.first().time
            val duration = startTime - endTime
            if (duration < minDuration || duration > maxDuration) return

            val avrCos = calcAvrCos()

            if (avrCos > maxCos) return

            if (!this::listener.isInitialized) return

            listener.onTap(startTime, endTime)
        }
        finally {
            recentMoves.clear()
        }
    }

    private fun calcAvrCos(): Float {
        var avrCos = 0f
        for(idx in 1 until recentMoves.size){
            var c = 10f
            for(prev in max(0, idx-3) until idx){
                val v1 = recentMoves[prev]
                val v2 = recentMoves[idx]

                val scalar = v1.vx * v2.vx + v1.vy * v2.vy + v1.vz * v2.vz
                val c1 = scalar / (v1.len*v2.len)

                if (c1 < c) c = c1
            }

            avrCos += c
        }

        return avrCos/(recentMoves.size-1)
    }

    private fun updateAvr(oldValue: Float?, curValue: Float) =
        if (oldValue == null) curValue else avrFactor*oldValue + (1-avrFactor)*curValue

    private inner class Move(
        val time: Long,
        dx: Float,
        dy: Float,
        dz: Float){

        val vx = dx - avrDx!!
        val vy = dy - avrDy!!
        val vz = dz - avrDz!!
        val len = sqrt(vx * vx + vy * vy + vz * vz)
    }
}