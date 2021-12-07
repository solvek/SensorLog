package com.skygolf.detector

object MultiTapHelper {
    fun createDetector(
        listener: MultiTapListener,
        tapDetectorBuilder: TapDetectorBuilder = TapDetectorBuilder(),
        multiTapDetectorBuilder: MultiTapDetectorBuilder = MultiTapDetectorBuilder()
    ): TapDetector {

        val mtapDetector = multiTapDetectorBuilder.build()
        mtapDetector.setListener(listener)

        val tapDetector = tapDetectorBuilder.build()

        val tapListener: TapListener = object : TapListener() {
            override fun onTap(startTime: Long, endTime: Long) {
                mtapDetector.registerTap()
            }
        }

        tapDetector.setListener(tapListener)

        return tapDetector
    }
}