package com.xie.myteleprompter

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * @Author XIE
 * @Date 2024/1/21
 * @Description
 */
class MainViewModel : ViewModel() {
    val text = MutableLiveData<String>()
    val speed = MutableLiveData<Float>()
    val textSize = MutableLiveData<Float>()
    val isPlayLD = MutableLiveData(false)

    private val partRegex = "#\\d+#".toRegex()

    val partTitleInfoList = MutableLiveData<MutableList<PartTitleIndexInfo>>()

    fun setText(text: String) {
        val partTitleMatch = partRegex.findAll(text)
        val partTitleList = ArrayList<PartTitleIndexInfo>()
        for (match in partTitleMatch) {
            partTitleList.add(PartTitleIndexInfo(match.value, match.range.first))
        }
        partTitleInfoList.value = partTitleList
        this.text.value = text
    }

    fun onPlayChange(isPlay: Boolean) {
        isPlayLD.value = isPlay
    }
}