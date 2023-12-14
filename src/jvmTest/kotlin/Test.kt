import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

internal class GrammarTest {

    val testcases: List<Pair<String, String>> =
        listOf("P" to "P", "P&Q" to "(P & Q)", " P | Q " to "(P | Q)",
            "a & b | c" to "((a & b) | c)",
            "a | b & c" to "(a | (b & c))",
            "(a|b )&c" to "((a | b) & c)",
            "a -> b " to "(a -> b)",
            "0&0" to "(0 & 0)",
            "-P" to "-P",
            "-P&Q" to "(-P & Q)" )

    @Test
    fun testParsing() {
        testcases.forEach { (inp, outp) ->
            println("Case $inp")
            val result = formulaGrammar.parseToEnd(inp)
            assertEquals(outp, result.toASCII())
        }
    }

    val errorcases: List<Pair<String, String>> =
        listOf(
            "P Q" to "Unexpected trailing text: 'Q'",
            "P &" to "Unexpected end of string",
            "P &  " to "Unexpected end of string",
            "(P&Q" to "Missing closing parenthesis",
            "P|Q)" to "Unexpected trailing text: ')'",
            "P-> & Q" to "Unexpected string starting at '& Q'.",
            )

    @Test
    fun testErrors() {
        errorcases.forEach { (inp, msg) ->
            println("Case $inp")
            try {
                val result = formulaGrammar.parseToEnd(inp)

                fail("Unexpeced success: $inp becomes $result")
            } catch (e: RuntimeException) {
                assertEquals(msg, e.message)
            }
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
                fail("No proof found")
            }
            pt.printTree()
            pt.verify()
        }
    }
}