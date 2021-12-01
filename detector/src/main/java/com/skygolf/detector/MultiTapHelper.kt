package com.skygolf.detector

object MultiTapHelper {
    fun createDetector(listener: MultiTapListener): TapDetector {
        val mtapDetector = MultiTapDetector()
        mtapDetector.setListener(listener)

        val tapDetector = TapDetector()

        val tapListener: TapListener = object : TapListener() {
            override fun onTap(startTime: Long, endTime: Long) {
                mtapDetector.registerTap()
            }
        }

        tapDetector.setListener(tapListener)

        return tapDetector
    }
}