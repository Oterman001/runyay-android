package com.oterman.rundemo.presentation.feature.share.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oterman.rundemo.R

/**
 * 品牌区域：App图标 + 品牌文案 + 二维码
 * 显示在分享图底部
 */
@Composable
fun AppBrandingSection(
    brandText: String,
    showSeparator: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayText = brandText.ifBlank { getRandomBrandText() }
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Column(modifier = modifier.fillMaxWidth()) {
        // 可选分隔线
        if (showSeparator) {
            HorizontalDivider(thickness = 1.dp, color = dividerColor)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：图标 + 名称 + 描述
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 图标 + 应用名称
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.run_demo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "跑鸭·RunYay",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }
                // 品牌文案
                Text(
                    text = displayText,
                    fontSize = 11.sp,
                    color = onSurfaceVariant,
                    maxLines = 2,
                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧：二维码
            Image(
                painter = painterResource(id = R.drawable.ic_unifify_download_url),
                contentDescription = "QR Code",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(50.dp)
//                    .clip(RoundedCornerShape(6.dp))
//                    .border(1.dp, dividerColor, RoundedCornerShape(6.dp))
            )
        }
    }
}

internal val brandTexts = listOf(
    "每一步都算数",
    "跑出自己的节奏",
    "用脚步丈量世界",
    "坚持跑步，遇见更好的自己",
    "奔跑是最好的解压方式",
    "今天的汗水，明天的勋章",
    "跑步让生活更美好",
    "一步一脚印，一路向前",
    "用跑步记录生活",
    "每一次进步，都能看到。",
    "把坚持跑成习惯。",
    "记录不是目的，变强才是。一路向前。",
    "你的每一步，都认真对待。",
    "跑步让生活更清晰。",
    "跑得漂亮，也要晒得漂亮。",
    "保持热爱，持续发光。",
    "不怕慢，只怕停。持续突破。",
    "速度不是天生，练出来的。",
    "数据驱动跑步，量化你的跑力能力。",
    "记录的不是数据，是你的成长轨迹。",
    "今天的汗水，是明天的实力。",
    "每一次抵达终点，都比昨天更强一点。",
    "出门前不想跑，跑完之后真香。",
    "跑步，是把疲惫甩在身后，把力量留在心里。",
    "每一个清晨与黄昏，都值得被记录。",
    "用脚步把烦恼甩远一点。",
    "每一次出发，都让生活更清晰。",
    "跑步治愈了我。",
    "今天也是被自己说服去跑的一天。",
    "再累也要完成，再忙也要坚持。",
    "不是为了跑快，是为了跑得坚定。",
    "风在后退，我在前进。",
    "再累一点，就能再强一点。",
    "跑完了！快夸我，不然我明天不跑了。",
    "今天的步伐，是明天的底气。",
    "不为别人，只为更强的自己。"
)

internal val recommendBrandTexts = listOf(
    "跑友推荐！跑鸭记录每一步，数据清晰到让你上瘾。",
    "用了跑鸭才知道，跑步也可以这么有仪式感。",
    "每次跑完打开跑鸭看数据，是我最爱的奖励时刻。",
    "跑鸭的轨迹地图太好看了，每条路线都是一幅画。",
    "推荐给所有热爱跑步的你，跑鸭让跑步更值得记录。",
    "跑鸭，跑者自己做的跑步 App，懂你的每一步。",
    "跑完配速一拉，成就感直接拉满——跑鸭绝了。",
    "跑鸭让我开始认真对待每一次出门跑步。",
    "不知道怎么进步？先把数据交给跑鸭，它比你更清楚。",
    "跑友圈都在用跑鸭，你还没下载？",
    "跑鸭的分享图太好看，每次都想发朋友圈晒成绩。",
    "记录跑步这件事，交给跑鸭就够了，简单又专业。",
    "用跑鸭两个月，我的配速进步了 30 秒——真的！",
    "跑鸭不只是记录工具，它让我更爱跑步这件事。",
    "朋友问我为什么越跑越起劲，我说因为有跑鸭陪着。",
    "跑步数据这么好看，多亏了跑鸭。强烈安利！",
    "每公里都留痕迹，每次进步都看得见——这就是跑鸭。",
    "跑了这么多年，终于找到最懂跑者的 App：跑鸭。",
    "认真跑步的人，都值得一个认真记录的工具——跑鸭。",
    "跑鸭在手，每次出发都更有动力。跑友必备！",
    "跑鸭在手，天下我有。",
    "迈开腿，跑鸭陪你记。",
    "步步为营，鸭你同行。",
    "跑过的路，跑鸭都记得。",
    "配速说话，跑鸭来记。",
    "每一步，都算数。",
    "跑起来，跑鸭懂你。",
    "出发！跑鸭在线。",
    "鸭鸭不累，你也别停。",
    "记录在跑，进步在看。",
    "不跑白不跑，跑了不白跑。",
    "腿动数据动，鸭鸭帮你懂。",
    "跑多快不重要，跑起来最重要。",
    "今天不跑明天悔，明天一跑后天美。",
    "跑鸭记录，比你自己还了解你。",
    "一鸭在手，跑遍神州。",
    "慢一点没关系，跑鸭都记得你。",
    "你跑我记，天衣无缝。",
    "废话少说，开始跑吧！",
    "跑得越多，越上瘾——跑鸭助攻。",
)

internal fun getRandomBrandText(): String {
    return recommendBrandTexts.random()
}
