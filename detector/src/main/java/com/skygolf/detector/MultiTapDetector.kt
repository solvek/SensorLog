package com.skygolf.detector

import java.util.*
import kotlin.concurrent.timer

class MultiTapDetector internal constructor(private val maxDelay: Long) {
    private var listener: MultiTapListener? = null

    private var timer: Timer? = null

    private var taps = 0

    fun setListener(listener: MultiTapListener){
        this.listener = listener
    }

    fun registerTap(){
        timer?.cancel()
        timer = timer(initialDelay = maxDelay, period = 10000000000L){
            timer?.cancel()
            timer = null
            listener?.onMultiTap(taps)
            taps = 0
        }
        taps ++
    }
}