import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

internal class GrammarTest {

    @Test fun testParse1() = testParsing("P","P")

@Test fun testParse2() = testParsing("P&Q", "(P & Q)")
@Test fun testParse3() = testParsing(" P | Q ", "(P | Q)")
@Test fun testParse4() = testParsing("a & b | c", "((a & b) | c)")
@Test fun testParse5() = testParsing("a | b & c", "(a | (b & c))")
@Test fun testParse6() = testParsing("(a|b )&c", "((a | b) & c)")
@Test fun testParse7() = testParsing("a -> b ", "(a -> b)")
@Test fun testParse8() = testParsing("0&0", "(0 & 0)")
@Test fun testParse9() = testParsing("-P", "-P")
@Test fun testParse10() = testParsing("-P&Q", "(-P & Q)" )

    private fun testParsing(inp: String, outp: String) {
        println("Case $inp")
        val result = formulaGrammar.parseToEnd(inp)
        assertEquals(outp, result.toASCII())
    }

@Test fun testError1() = testError("P Q", "Unexpected trailing text: 'Q'")
@Test fun testError2() = testError("P &", "Unexpected end of string")
@Test fun testError3() = testError("P &  ", "Unexpected end of string")
@Test fun testError4() = testError("(P&Q", "Missing closing parenthesis")
@Test fun testError5() = testError("P|Q)", "Unexpected trailing text: ')'")
@Test fun testError6() = testError("P-> & Q", "Unexpected string starting at '& Q'.")

    fun testError(inp: String, msg:String) {
        println("Case $inp")
        try {
            val result = formulaGrammar.parseToEnd(inp)

            fail("Unexpeced success: $inp becomes $result")
        } catch (e: RuntimeException) {
            assertEquals(msg, e.message)
        }
    }

    val proofCases: List<String> =
        listOf(
            "P -> Q -> P",
            "P -> -P -> 0",
            "P&(Q|R)->(P->-R)->(Q|E)",
            "C&D|E->E|D"
        )

    @Test
    fun testProof() {
        proofCases.forEach {
            println(it)
            val formula = formulaGrammar.parseToEnd(it)
            val pt = findProof(formula)
            if (pt == null) {
                FAIL("No proof found")
            }
            pt.printTree()
            pt.verify()
        }
    }
}