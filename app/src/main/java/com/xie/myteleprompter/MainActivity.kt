package com.xie.myteleprompter

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xie.myteleprompter.ui.theme.MyTeleprompterTheme
import kotlinx.coroutines.delay

// TODO: 识别“#%d”来分段

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyTeleprompterTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TeleprompterView(viewModel)
                }
            }
        }

        viewModel.isPlayLD.observe(this) {
            if (it) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        //蓝牙按钮播放
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val isPlay = viewModel.isPlayLD.value ?: false
            viewModel.onPlayChange(!isPlay)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

}

@Preview(showBackground = true)
@Composable
fun ContentView() {
    MyTeleprompterTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            TeleprompterView()
        }
    }
}

@Composable
fun TeleprompterView(viewModel: MainViewModel = MainViewModel()) {
    val text by viewModel.text.observeAsState("")

    //速度67dp 1秒
    val speed by viewModel.speed.observeAsState(67f)

    val textSize by viewModel.textSize.observeAsState(46f)

    //开始滚动标志
    val isPlay by viewModel.isPlayLD.observeAsState(false)

    //复位标志
    var isReset by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    //滚动帧率
    val frameRate = 60

    //每帧延时
    val frameDelay: Long = 1000L / frameRate
    //每帧位移
    val frameOffset = LocalDensity.current.run { speed.dp.toPx() / frameRate }

    //滚动状态
    val scrollState = rememberScrollState()

    LaunchedEffect(isPlay, isReset, frameOffset, frameDelay) {
        if (isPlay) {
            while (isPlay) {
                //1秒60次
                delay(frameDelay)
                Log.i(
                    "testMsg",
                    "TeleprompterView: offset=" + frameOffset + " scrollState.value=" + scrollState.value
                )
                scrollState.scrollBy(frameOffset)
            }
        }

        if (isReset) {
            scrollState.scrollTo(0)
            isReset = false
        }
    }

    if (scrollState.value == scrollState.maxValue || scrollState.isScrollInProgress) {
        viewModel.onPlayChange(false)
    }

    Column {
        TextField(
            value = text,
            onValueChange = {
                viewModel.text.value = it
            },
            placeholder = {
                Text(text = "请输入文字", fontSize = textSize.sp, lineHeight = textSize.sp)
            },
            modifier = Modifier
                .verticalScroll(scrollState)
                .weight(1f)
                .fillMaxWidth(),
            textStyle = TextStyle(fontSize = textSize.sp, lineHeight = textSize.sp)
        )

        FunctionBtnGroup(isPlay, textSize, speed, onTextSizeChange = {
            viewModel.textSize.value = it
        }, onSpeedChange = {
            viewModel.speed.value = it
        }, onStartClick = {
            focusManager.clearFocus()
            viewModel.onPlayChange(!isPlay)
        }, onResetClick = {
            isReset = true
        })
    }


}

@Composable
fun FunctionBtnGroup(
    startScrollFlag: Boolean,
    textSize: Float,
    speed: Float,
    onTextSizeChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onStartClick: () -> Unit,
    onResetClick: () -> Unit
) {
    val maxSpeed = 130f
    val minSpeed = 5f
    val maxTextSize = 80f
    val minTextSize = 12f

    var isGroupFold by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .background(if (startScrollFlag) Color.Unspecified else colorResource(id = R.color.color_B3000000))
            .padding(10.dp)

    ) {
        //开始复位按钮
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            //开始按钮
            Button(
                onClick = onStartClick,
                modifier = Modifier.onKeyEvent {
                    return@onKeyEvent false
                }
            ) {
                Text(
                    text = if (startScrollFlag) "停止" else "开始",
                    fontSize = 20.sp,
                    lineHeight = 20.sp
                )
            }
            if (!startScrollFlag) {
                //复位按钮
                Button(
                    onClick = onResetClick,
                ) {
                    Text(
                        text = "复位",
                        fontSize = 20.sp,
                        lineHeight = 20.sp
                    )
                }

                //折叠按钮
                Button(
                    onClick = {
                        isGroupFold = !isGroupFold
                    },
                ) {
                    Text(
                        text = if (isGroupFold) "展开" else "折叠",
                        fontSize = 20.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        if (!startScrollFlag && !isGroupFold) {
            Spacer(modifier = Modifier.height(10.dp))

            //字号设置
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("字号：${textSize.toInt()}", fontSize = 20.sp, color = Color.White)

                Slider(
                    value = textSize,
                    onValueChange = onTextSizeChange,
                    valueRange = minTextSize..maxTextSize,
                    steps = (maxTextSize - minTextSize).toInt()
                )
            }

            //速度设置
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("速度：${speed.toInt()}", fontSize = 20.sp, color = Color.White)

                Slider(
                    value = speed,
                    onValueChange = onSpeedChange,
                    valueRange = minSpeed..maxSpeed,
                    steps = (maxSpeed - minSpeed).toInt()
                )
            }
        }
    }
}
