package com.ayush

import android.graphics.RectF
import android.util.Log
import com.robinhood.spark.SparkAdapter

class covidSparkAdapter(private val dailyData: List<CovidData>): SparkAdapter() {
    var metric=Metric.POSITIVE
    var daysAgo=TimeScale.MAX
    override fun getCount()=dailyData.size

    override fun getItem(index: Int)=dailyData[index]

    override fun getY(index: Int): Float {
        val chosenDay=dailyData[index]
        return when(metric){
            Metric.DEATH->chosenDay.deathIncrease.toFloat()
            Metric.POSITIVE->chosenDay.positiveIncrease.toFloat()
            Metric.NEGATIVE->chosenDay.negativeIncrease.toFloat()
        }
    }

    override fun getDataBounds(): RectF {
        val bounds= super.getDataBounds()
        Log.i("tag","bounds have been changed")
        if(daysAgo!=TimeScale.MAX){
          bounds.left=count-daysAgo.numDays.toFloat()
        }
        return bounds

    }
}