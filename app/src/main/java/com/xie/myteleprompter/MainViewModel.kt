package com.xie.myteleprompter

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * @Author XIE
 * @Date 2024/1/21
 * @Description
 */
class MainViewModel :ViewModel() {
    val text = MutableLiveData<String>()
    val speed = MutableLiveData<Float>()
    val textSize = MutableLiveData<Float>()
    val isPlayLD = MutableLiveData(false)

    fun onPlayChange(isPlay:Boolean){
        isPlayLD.value = isPlay
    }
}