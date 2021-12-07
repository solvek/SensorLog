package com.skygolf.detector

import timber.log.Timber
import kotlin.math.max
import kotlin.math.sqrt

/*
Allows to detect a single tap on device
0 < avrFactor < 1

 */
class TapDetector internal constructor(
    private val avrFactor: Float,
    private val minDuration: Long,
    private val maxDuration: Long,
    private val minAccel: Float,
    private val maxAccel: Float,
    private val maxCos: Float) {

    private var listener: TapListener? = null

    private var avrDx: Float? = null
    private var avrDy: Float? = null
    private var avrDz: Float? = null

    private val isInTap get() = fluctuations.isNotEmpty()

    private val fluctuations = ArrayList<Move>()

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
            fluctuations.add(currentMove)
        } else if (isInTap) {
            endTap()
        }
    }

    private fun endTap() {
        try {
            val startTime = fluctuations.last().time
            val endTime = fluctuations.first().time
            val duration = startTime - endTime

            if (duration > 0) {
                Timber.tag("!TapDetector").d("Tap candidate, duration $duration)")
            }
            if (duration < minDuration || duration > maxDuration) return

            val avrCos = calcAvrCos()

            Timber.tag("!TapDetector").d("Average cos $avrCos")

            if (avrCos > maxCos) return

            listener?.onTap(startTime, endTime)
        }
        finally {
            fluctuations.clear()
        }
    }

    private fun calcAvrCos(): Float {
        var avrCos = 0f
        for(idx in 1 until fluctuations.size){
            var c = 10f
            for(prev in max(0, idx-3) until idx){
                val v1 = fluctuations[prev]
                val v2 = fluctuations[idx]

                val scalar = v1.vx * v2.vx + v1.vy * v2.vy + v1.vz * v2.vz
                val c1 = scalar / (v1.len*v2.len)

                if (c1 < c) c = c1
            }

            avrCos += c
        }

        return avrCos/(fluctuations.size-1)
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