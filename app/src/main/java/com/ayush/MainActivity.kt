package com.ayush

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.ticker.TickerUtils
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
private const val BASE_URL = "https://api.covidtracking.com/v1/"

class MainActivity : AppCompatActivity() {
    lateinit private var currentlyshowndata: List<CovidData>
    lateinit var adapter: covidSparkAdapter
    private lateinit var perstateDailyData: Map<String, List<CovidData>>
    private lateinit var nationalDailyData: List<CovidData>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val covidService = retrofit.create(CovidService::class.java)
        covidService.getNationalData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                Log.i("TAG", "the respose is $response")
                val nationalData = response.body()
                if (nationalData == null) {
                    Log.e("TAG", "some error occured")
                    return
                }

                nationalDailyData = nationalData.reversed()
                setUpEventListeners()
                updateDisplaywithData(nationalDailyData)


            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e("TAG", "On Failure$t")
            }

        })
        covidService.getStatesData().enqueue(object : Callback<List<CovidData>> {
            override fun onResponse(
                call: Call<List<CovidData>>,
                response: Response<List<CovidData>>
            ) {
                val stateData = response.body()
                if (stateData == null) {
                    Log.e("TAG","some error occurred")
                    return
                }
                perstateDailyData = stateData.reversed().groupBy { it.state }
                updateSpinnerData(perstateDailyData.keys)

            }

            override fun onFailure(call: Call<List<CovidData>>, t: Throwable) {
                Log.e("TAG", "On Failure$t")
            }
        })

    }

    private fun updateSpinnerData(StateName: Set<String>) {
        val stateSet=StateName.toMutableList()
        stateSet.sort()
        stateSet.add(0,"All State")
        spinner.attachDataSource(stateSet)
        spinner.setOnSpinnerItemSelectedListener { parent, view, position, id ->
            val selectedState=parent.getItemAtPosition(position) as String
            val selectData=perstateDailyData[selectedState]?: nationalDailyData
            updateDisplaywithData(selectData)

        }

    }

    private fun setUpEventListeners() {
        tickerView.setCharacterLists(TickerUtils.provideNumberList())
        sparkview.isScrubEnabled = true
        sparkview.setScrubListener { itemData ->
            if (itemData is CovidData)
                updateDateData(itemData)
        }
        btnDateSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when (checkedId) {
                R.id.radioButtonWeek -> TimeScale.WEEK
                R.id.radioButtonMonth -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }
        btnMetricGroup.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId){
                R.id.radioButtonNegative->updateMetric(Metric.NEGATIVE)
                R.id.radioButtonPositive->updateMetric(Metric.POSITIVE)
                R.id.radioButtonDeaths->updateMetric(Metric.DEATH)
            }
        }
    }

    private fun updateMetric(metric: Metric) {
       adapter.metric=metric
        adapter.notifyDataSetChanged()
        var colorRes=when(adapter.metric)
        {
            Metric.POSITIVE->R.color.Positive
            Metric.NEGATIVE->R.color.Negative
            Metric.DEATH->R.color.Deaths
        }
        sparkview.lineColor=ContextCompat.getColor(this,colorRes)
        updateDateData(currentlyshowndata.last())
    }

    private fun updateDisplaywithData(DailyData: List<CovidData>) {
        currentlyshowndata=DailyData
        adapter = covidSparkAdapter(DailyData)
        sparkview.adapter = adapter
        radioButtonMax.isChecked = true
        radioButtonPositive.isChecked = true

    }

    private fun updateDateData(covidData: CovidData) {
        var matricChosen = when (adapter.metric) {
            Metric.DEATH -> covidData.deathIncrease
            Metric.POSITIVE -> covidData.positiveIncrease
            Metric.NEGATIVE -> covidData.negativeIncrease
        }
        tickerView.text = NumberFormat.getInstance().format(matricChosen)
        tvDate.text = SimpleDateFormat("MMM dd, yyyy").format(covidData.dateChecked)
    }
}
