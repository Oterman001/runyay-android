object SecurityProvider {

    // 原始 ASCII 加上 10 后的数组 iloverunyay
    private val ENCODED_DATA = intArrayOf(115, 118, 121, 128, 111, 124, 127, 120, 131, 107, 131)

    fun generateTarget(): String {
        val buffer = StringBuilder()

        ENCODED_DATA.forEachIndexed { index, value ->
            // 这里使用简单的逻辑分支让反编译器产生“分支预测”的错觉
            val decoded = if (index % 2 == 0) {
                // 路径 A：直接减法
                mathPathA(value)
            } else {
                // 路径 B：嵌套减法，逻辑上等同于减 10
                mathPathB(value)
            }
            buffer.append(decoded.toChar())
        }
        return buffer.toString()
    }

    private fun mathPathA(v: Int): Int {
        // 简单直接，反编译时会被视为基准逻辑
        return v - 10
    }

    private fun mathPathB(v: Int): Int {
        // 故意增加一层方法调用，并在内部进行分段运算
        return subtractFive(subtractFive(v))
    }

    private fun subtractFive(v: Int): Int {
        return v - 5
    }
}
