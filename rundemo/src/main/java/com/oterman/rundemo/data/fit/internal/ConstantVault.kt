package com.oterman.rundemo.data.fit.internal

internal object _CV {

    private val _d = longArrayOf(
        2583384574769504090L,
        939873212077449154L,
        5143989500657708468L,
        -1115598375815490220L,
        3486924999649122855L,
        6857557296839648482L,
        -537845369598122847L,
        -7973558218171103829L,
        -3494993472907013003L,
        1557124601146772764L,
        3211707703179091128L,
        -4668352453238853006L,
        6200577877027587598L,
        2787508008974384137L,
        -7875141422875223183L,
        -3525377731163510141L,
        8593637904493354881L,
        -279112474951639130L,
        -4238407052437184566L,
        -8593637854063058984L,
        6236023168415866848L,
        -3525377731163509449L,
        8557997539357477301L,
        5158322355294628282L,
        1914607245827158797L,
        -1095341761982176724L,
        3884882979063849013L,
        364262930019111188L,
        7163108583057631326L,
        7450802536480790019L,
        -450016303912358823L,
        7450802536464012803L,
        -864048539922382776L,
        7576463658808799772L,
        9116278962389197304L,
        9116278962254979576L,
        -4409590212927662384L,
        7523075232165183798L,
        -7523064094277301341L,
        4409601350815544841L
    )

    private const val _o = 0x5DEECE66DL

    private fun _g0(i: Int): Double {
        val s1 = _d[i] - _o
        val s2 = java.lang.Long.rotateRight(s1, 17)
        return Double.fromBits(s2 xor -6651317643516042136L)
    }

    private fun _g1(i: Int): Double {
        val s1 = _d[i] - _o
        val s2 = java.lang.Long.rotateRight(s1, 23)
        return Double.fromBits(s2 xor 1311768467294899695L)
    }

    private fun _g2(i: Int): Double {
        val s1 = _d[i] - _o
        val s2 = java.lang.Long.rotateRight(s1, 11)
        return Double.fromBits(s2 xor -81985529216486896L)
    }

    private fun _g3(i: Int): Double {
        val s1 = _d[i] - _o
        val s2 = java.lang.Long.rotateRight(s1, 29)
        return Double.fromBits(s2 xor 2623536866208103920L)
    }

    private fun _g4(i: Int): Double {
        val s1 = _d[i] - _o
        val s2 = java.lang.Long.rotateRight(s1, 7)
        return Double.fromBits(s2 xor 3935286637375052400L)
    }

    private fun _g5(i: Int): Double {
        val s1 = _d[i] - _o
        val s2 = java.lang.Long.rotateRight(s1, 19)
        return Double.fromBits(s2 xor 5242570270227790720L)
    }

    private fun _g6(i: Int): Double {
        val s1 = _d[i] - _o
        val s2 = java.lang.Long.rotateRight(s1, 13)
        return Double.fromBits(s2 xor 6520715408340618797L)
    }

    private fun _g7(i: Int): Double {
        val s1 = _d[i] - _o
        val s2 = java.lang.Long.rotateRight(s1, 31)
        return Double.fromBits(s2 xor 7749937592931204701L)
    }

    private fun _g8(i: Int): Double {
        val s1 = _d[i] - _o
        val s2 = java.lang.Long.rotateRight(s1, 37)
        return Double.fromBits(s2 xor 8979713916962557546L)
    }

    private fun _g9(i: Int): Double {
        val s1 = _d[i] - _o
        val s2 = java.lang.Long.rotateRight(s1, 41)
        return Double.fromBits(s2 xor -8237247218500083845L)
    }

    private fun _g10(i: Int): Double {
        val s1 = _d[i] - _o
        val s2 = java.lang.Long.rotateRight(s1, 43)
        return Double.fromBits(s2 xor -7011967879880152436L)
    }

    fun a0() = _g0(0)
    fun a1() = _g0(1)
    fun a2() = _g0(2)
    fun a3() = _g1(3)
    fun a4() = _g1(4)
    fun a5() = _g1(5)
    fun a6() = _g1(6)
    fun a7() = _g1(7)
    fun a8() = _g2(8)
    fun a9() = _g3(9)
    fun a10() = _g3(10)
    fun a11() = _g3(11)
    fun a12() = _g3(12)
    fun a13() = _g3(13)
    fun a14() = _g4(14)
    fun a15() = _g5(15)
    fun a16() = _g5(16)
    fun a17() = _g5(17)
    fun a18() = _g5(18)
    fun a19() = _g5(19)
    fun a20() = _g5(20)
    fun a21() = _g5(21)
    fun a22() = _g6(22)
    fun a23() = _g6(23)
    fun a24() = _g6(24)
    fun a25() = _g7(25)
    fun a26() = _g7(26)
    fun a27() = _g7(27)
    fun a28() = _g7(28)
    fun a29() = _g8(29)
    fun a30() = _g8(30)
    fun a31() = _g8(31)
    fun a32() = _g8(32)
    fun a33() = _g8(33)
    fun a34() = _g9(34)
    fun a35() = _g9(35)
    fun a36() = _g10(36)
    fun a37() = _g10(37)
    fun a38() = _g10(38)
    fun a39() = _g10(39)

    fun _hrf(z: Int): Double = when (z) {
        0 -> a9()
        1 -> a10()
        2 -> a11()
        3 -> a12()
        else -> 1.0
    }

    fun _h7(i: Int): Double = when (i) {
        0 -> a29()
        1 -> a30()
        2 -> a31()
        3 -> a32()
        4 -> a33()
        else -> 1.0
    }

    fun _h5(i: Int): Double = when (i) {
        0 -> a36()
        1 -> a37()
        2 -> a38()
        3 -> a39()
        else -> 1.0
    }
}
