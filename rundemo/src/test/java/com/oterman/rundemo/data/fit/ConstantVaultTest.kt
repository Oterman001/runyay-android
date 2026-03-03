package com.oterman.rundemo.data.fit

import com.oterman.rundemo.data.fit.internal._CV
import org.junit.Assert.assertEquals
import org.junit.Test

class ConstantVaultTest {

    private val DELTA = 1e-10

    @Test fun a0()  = assertEquals(0.182258, _CV.a0(), DELTA)
    @Test fun a1()  = assertEquals(0.000104, _CV.a1(), DELTA)
    @Test fun a2()  = assertEquals(4.6, _CV.a2(), DELTA)
    @Test fun a3()  = assertEquals(0.80, _CV.a3(), DELTA)
    @Test fun a4()  = assertEquals(0.298956, _CV.a4(), DELTA)
    @Test fun a5()  = assertEquals(0.193261, _CV.a5(), DELTA)
    @Test fun a6()  = assertEquals(0.189439, _CV.a6(), DELTA)
    @Test fun a7()  = assertEquals(0.012778, _CV.a7(), DELTA)
    @Test fun a8()  = assertEquals(0.16667, _CV.a8(), DELTA)
    @Test fun a9()  = assertEquals(1.10, _CV.a9(), DELTA)
    @Test fun a10() = assertEquals(1.07, _CV.a10(), DELTA)
    @Test fun a11() = assertEquals(1.05, _CV.a11(), DELTA)
    @Test fun a12() = assertEquals(1.02, _CV.a12(), DELTA)
    @Test fun a13() = assertEquals(3.5, _CV.a13(), DELTA)
    @Test fun a14() = assertEquals(0.1, _CV.a14(), DELTA)
    @Test fun a15() = assertEquals(39.0, _CV.a15(), DELTA)
    @Test fun a16() = assertEquals(0.62, _CV.a16(), DELTA)
    @Test fun a17() = assertEquals(0.70, _CV.a17(), DELTA)
    @Test fun a18() = assertEquals(0.84, _CV.a18(), DELTA)
    @Test fun a19() = assertEquals(0.88, _CV.a19(), DELTA)
    @Test fun a20() = assertEquals(0.975, _CV.a20(), DELTA)
    @Test fun a21() = assertEquals(6.0, _CV.a21(), DELTA)
    @Test fun a22() = assertEquals(29.54, _CV.a22(), DELTA)
    @Test fun a23() = assertEquals(5.000663, _CV.a23(), DELTA)
    @Test fun a24() = assertEquals(0.007546, _CV.a24(), DELTA)
    @Test fun a25() = assertEquals(0.0075, _CV.a25(), DELTA)
    @Test fun a26() = assertEquals(0.19326, _CV.a26(), DELTA)
    @Test fun a27() = assertEquals(42195.0, _CV.a27(), DELTA)
    @Test fun a28() = assertEquals(2.1, _CV.a28(), DELTA)
    @Test fun a29() = assertEquals(0.59, _CV.a29(), DELTA)
    @Test fun a30() = assertEquals(0.74, _CV.a30(), DELTA)
    @Test fun a31() = assertEquals(0.84, _CV.a31(), DELTA)
    @Test fun a32() = assertEquals(0.88, _CV.a32(), DELTA)
    @Test fun a33() = assertEquals(0.95, _CV.a33(), DELTA)
    @Test fun a34() = assertEquals(1.67, _CV.a34(), DELTA)
    @Test fun a35() = assertEquals(1.92, _CV.a35(), DELTA)
    @Test fun a36() = assertEquals(0.60, _CV.a36(), DELTA)
    @Test fun a37() = assertEquals(0.70, _CV.a37(), DELTA)
    @Test fun a38() = assertEquals(0.80, _CV.a38(), DELTA)
    @Test fun a39() = assertEquals(0.90, _CV.a39(), DELTA)
}
