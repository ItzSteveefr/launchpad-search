package com.devrinth.launchpad.search.plugins

import android.content.Context
import androidx.appcompat.content.res.AppCompatResources
import com.devrinth.launchpad.R
import com.devrinth.launchpad.adapters.ResultAdapter
import com.devrinth.launchpad.search.SearchPlugin
import java.util.regex.Pattern

class UnitConversionPlugin(mContext: Context) : SearchPlugin(mContext) {

    override var ID = "units"

    private data class Conversion(
        val value: Double,
        val fromUnit: String,
        val toUnit: String
    )

    private fun parseConversion(input: String): Conversion? {
        val pattern = Pattern.compile("""(\d+(\.\d+)?)\s*([a-zA-Z]+)\s*(to|in)\s*([a-zA-Z]+)""")
        val matcher = pattern.matcher(input)
        return if (matcher.find()) {
            Conversion(
                matcher.group(1).toDouble(),
                matcher.group(3).lowercase(),
                matcher.group(5).lowercase()
            )
        } else {
            null
        }
    }

    private fun convert(conversion: Conversion, conversionMap: Map<String, Double>): String {
        val fromFactor = conversionMap[conversion.fromUnit]
        val toFactor = conversionMap[conversion.toUnit]
        if (fromFactor == null || toFactor == null) {
            throw IllegalArgumentException("Unknown unit")
        }
        val valueInSIUnit = conversion.value * fromFactor
        val convertedValue = valueInSIUnit / toFactor
        val formatterString =
            if (convertedValue != 0.0) R.string.plugin_unit_conversion_result_full else R.string.plugin_unit_conversion_result
        return mContext.getString(formatterString, convertedValue, conversion.toUnit)
    }

    private fun convertTemperature(conversion: Conversion): String {
        val kelvinValue = when (conversion.fromUnit) {
            "c", "celsius" -> conversion.value + 273.15
            "f", "fahrenheit" -> (conversion.value + 459.67) * 5.0 / 9.0
            "k", "kelvin" -> conversion.value
            else -> throw IllegalArgumentException("Unknown unit")
        }
        val convertedValue = when (conversion.toUnit) {
            "c", "celsius" -> kelvinValue - 273.15
            "f", "fahrenheit" -> kelvinValue * 9.0 / 5.0 - 459.67
            "k", "kelvin" -> kelvinValue
            else -> throw IllegalArgumentException("Unknown unit")
        }
        return mContext.getString(
            R.string.plugin_unit_conversion_result,
            convertedValue,
            conversion.toUnit
        )
    }

    private fun getConversionMap(type: String): Map<String, Double> {
        return when (type) {
            "length" -> mapOf(
                "mm" to 0.001, "millimeter" to 0.001, "millimeters" to 0.001,
                "cm" to 0.01, "centimeter" to 0.01, "centimeters" to 0.01,
                "m" to 1.0, "meter" to 1.0, "meters" to 1.0,
                "km" to 1000.0, "kilometer" to 1000.0, "kilometers" to 1000.0,
                "in" to 0.0254, "inch" to 0.0254, "inches" to 0.0254,
                "ft" to 0.3048, "foot" to 0.3048, "feet" to 0.3048,
                "yd" to 0.9144, "yard" to 0.9144, "yards" to 0.9144,
                "mi" to 1609.34, "mile" to 1609.34, "miles" to 1609.34
            )
            "mass" -> mapOf(
                "mg" to 0.000001, "milligram" to 0.000001, "milligrams" to 0.000001,
                "g" to 0.001, "gram" to 0.001, "grams" to 0.001,
                "kg" to 1.0, "kilogram" to 1.0, "kilograms" to 1.0,
                "lb" to 0.453592, "pound" to 0.453592, "pounds" to 0.453592,
                "oz" to 0.0283495, "ounce" to 0.0283495, "ounces" to 0.0283495,
                "ton" to 1000.0, "tons" to 1000.0
            )
            else -> emptyMap()
        }
    }

    private fun processConversion(
        conversion: Conversion,
        type: String,
        iconRes: Int
    ): ResultAdapter {
        val conversionMap = getConversionMap(type)
        val resultText = if (type == "temperature") {
            convertTemperature(conversion)
        } else {
            convert(conversion, conversionMap)
        }
        return ResultAdapter(
            resultText,
            mContext.getString(
                R.string.plugin_unit_conversion_result_units,
                conversion.fromUnit,
                conversion.toUnit
            ),
            AppCompatResources.getDrawable(mContext, iconRes),
            null,
            null
        )
    }


    override fun pluginProcess(query: String) {
        super.pluginProcess(query)
        try {
            val input = query.trim().lowercase()
            val conversion = parseConversion(input) ?: return
            val result = when {
                getConversionMap("length").containsKey(conversion.fromUnit) ->
                    processConversion(conversion, "length", R.drawable.baseline_ruler_24)
                getConversionMap("mass").containsKey(conversion.fromUnit) ->
                    processConversion(conversion, "mass", R.drawable.baseline_scale_24)
                "celsius".startsWith(conversion.fromUnit) || "fahrenheit".startsWith(conversion.fromUnit) || "kelvin".startsWith(
                    conversion.fromUnit
                ) ->
                    processConversion(
                        conversion,
                        "temperature",
                        R.drawable.baseline_thermostat_24
                    )
                else -> null
            }
            if (result != null) {
                pluginResult(arrayListOf(result), query)
            }
        } catch (e: Exception) {
            // Ignore exceptions
        }
    }
}