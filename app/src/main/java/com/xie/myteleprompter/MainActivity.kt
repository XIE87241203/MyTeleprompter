package com.xie.myteleprompter

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xie.myteleprompter.ui.theme.MyTeleprompterTheme
import kotlinx.coroutines.delay

// TODO: 实现可触控的进度条

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
    //标题位置列表
    val partTitleInfoList by viewModel.partTitleInfoList.observeAsState(ArrayList())

    var needScrollToPx by remember { mutableIntStateOf(-1) }

    val focusManager = LocalFocusManager.current

    //滚动帧率
    val frameRate = 60

    //每帧延时
    val frameDelay: Long = 1000L / frameRate
    //每帧位移
    val frameOffset = LocalDensity.current.run { speed.dp.toPx() / frameRate }

    //滚动状态
    val scrollState = rememberScrollState()

    LaunchedEffect(isPlay, needScrollToPx, frameOffset, frameDelay) {
        if (isPlay) {
            while (isPlay) {
                //1秒60次
                delay(frameDelay)
                scrollState.scrollBy(frameOffset)
            }
        }

        if (needScrollToPx != -1) {
            scrollState.scrollTo(needScrollToPx)
            needScrollToPx = -1
        }
    }

    if (scrollState.value == scrollState.maxValue || scrollState.isScrollInProgress) {
        viewModel.onPlayChange(false)
    }

    Column {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            BasicTextField(
                value = text,
                onValueChange = {
                    viewModel.setText(it)
                },
                decorationBox = {
                    if (text.isNullOrEmpty()) {
                        Text(
                            text = stringResource(id = R.string.input_hint),
                            fontSize = textSize.sp,
                            lineHeight = textSize.sp,
                            color = colorResource(id = R.color.color_6A6A6A)
                        )
                    }
                    it()
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .background(colorResource(id = R.color.black)),
                onTextLayout = {
                    //获取标题的位置
                    for (title in partTitleInfoList) {
                        val boundsRect = it.getBoundingBox(title.startIndex)
                        if (title.localY != boundsRect.topLeft.y) {
                            title.localY = boundsRect.topLeft.y
                        }
                    }
                },
                cursorBrush = SolidColor(colorResource(id = R.color.white)),
                textStyle = TextStyle(
                    fontSize = textSize.sp,
                    lineHeight = textSize.sp,
                    color = colorResource(id = R.color.white)
                )
            )

            ProcessBar(partTitleInfoList) {
                needScrollToPx = it
            }
        }


        FunctionBtnGroup(isPlay, textSize, speed, onTextSizeChange = {
            viewModel.textSize.value = it
        }, onSpeedChange = {
            viewModel.speed.value = it
        }, onStartClick = {
            focusManager.clearFocus()
            viewModel.onPlayChange(!isPlay)
        }, onResetClick = {
            needScrollToPx = 0
        })
    }


}

@Composable
fun ProcessBar(
    partTitleInfoList: MutableList<PartTitleIndexInfo>,
    onTitleClickListener: (Int) -> Unit
) {
    if (partTitleInfoList.isNotEmpty()) {
        val state = rememberLazyListState()
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(0.dp, 10.dp),
            modifier = Modifier
                .background(colorResource(id = R.color.color_6A6A6A))
                .fillMaxHeight(),
            state = state
        ) {
            items(partTitleInfoList.size) {
                key(partTitleInfoList) {
                    //整个list作为索引，list更新时重组此块数据
                    val titleInfo = partTitleInfoList[it]
                    Text(
                        text = titleInfo.text,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(50.dp)
                            .background(colorResource(id = R.color.black))
                            .pointerInput(Unit) {
                                //触控或者点击跳转到对应段落
                                detectTapGestures(
                                    onPress = {
                                        onTitleClickListener(titleInfo.localY.toInt())
                                    },
                                    onTap = {
                                        onTitleClickListener(titleInfo.localY.toInt())
                                    }
                                )
                            },
                        color = colorResource(id = R.color.white),
                        fontSize = 18.sp
                    )
                }
            }
        }
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
        mutableStateOf(true)
    }

    //播放时自动折叠
    if(startScrollFlag && !isGroupFold) isGroupFold = true

    Column(
        modifier = Modifier
            .background(colorResource(id = R.color.color_3A3A3A))
            .padding(10.dp, 3.dp)

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
                    text = stringResource(id = if (startScrollFlag) R.string.stop else R.string.start),
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
                        text = stringResource(id = R.string.reset),
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
                        text = stringResource(id = if (isGroupFold) R.string.unfold else R.string.fold),
                        fontSize = 20.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        if (!startScrollFlag && !isGroupFold) {

            //字号设置
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(id = R.string.text_size_) + textSize.toInt(),
                    fontSize = 20.sp,
                    color = Color.White
                )

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
                Text(
                    stringResource(id = R.string.speed_) + speed.toInt(),
                    fontSize = 20.sp,
                    color = Color.White
                )

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
