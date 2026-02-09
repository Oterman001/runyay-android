package com.oterman.rundemo.data.mock

/**
 * Daily motivational sentences for runners
 * Corresponds to iOS DailySentenceUtils
 */
object DailySentences {
    val sentences = listOf(
        "不是在跑路，是在突破昨天的自己。",
        "今天不拼，明天拿什么和自己较劲？",
        "脚下越狠，目标越近。",
        "越累越要坚持，那是实力正在重塑。",
        "你现在的每一步，未来都会感谢你。",
        "强者不是天生，是一步一步跑出来的。",
        "最难的一步，永远是迈出家门的那一步。",
        "世界很吵，但在路上，你只属于你自己。",
        "感受心跳的节奏，那是生命最蓬勃的鼓点。",
        "每一步，都在重新定义你的边界。",
        "汗水是脂肪的眼泪，也是意志的勋章。",
        "不怕慢，就怕站。",
        "跑步不会让你变年轻，但会让你永远年轻。",
        "没有不能跑的天气，只有不想跑的借口。",
        "当你想放弃时，想想当初为什么开始。",
        "跑步是最公平的运动，付出多少就收获多少。",
        "你的对手只有一个，就是昨天的自己。",
        "跑步的意义不在于终点，而在于沿途的风景。",
        "每一次呼吸都在告诉你：你还活着，还在战斗。",
        "跑下去，天自然会亮。"
    )

    /**
     * Get a random daily sentence
     */
    fun getRandomSentence(): String {
        return sentences.random()
    }
}
