import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LoadSaveTest {

    @Test fun testImportExport1() = testImportExport("P->P", "impI|ax 0|")
    @Test fun testImportExport2() = testImportExport("P&Q->Q&P", "impI|andI|andE2|ax 0|andE1|ax 0|")
    @Test fun testImportExport3() = testImportExport("P|Q->Q|P", "impI|orE|ax 0|impI|orI2|ax 1|impI|orI1|ax 1|")

    @Test fun testImportExport4() = testImportExport("A&A->A", "impI|open|", true)
    @Test fun testImportExport5() = testImportExport("P|Q->Q|P", "impI|orE|ax 0|impI|open|open|", true)

    @Test fun testImportExportGap() = testImportExport("A&A->A", "impI|gap|ax 0|")

    @Test fun testImportExportAuto1() = testImportExportAuto("P&Q->Q&P")
    @Test fun testImportExportAuto2() = testImportExportAuto("((A -> B) -> A) -> A")

    private fun testImportExport(formulaStr: String, proofStr: String, open:Boolean=false) {
        val formula = formulaGrammar.parseToEnd(formulaStr)
        val pt = ProofTree.import(formula, proofStr)
        pt.printTree()
        if(!open) {
            assertTrue(pt.isClosed)
        }
        val exported = pt.export()
        assertEquals(proofStr, exported)
    }

    private fun testImportExportAuto(formulaStr: String) {
        val formula = formulaGrammar.parseToEnd(formulaStr)
        val pt = findProof(formula)
        assertNotNull(pt)
        pt.printTree()
        val exported = pt.export()
        println(exported)
        val imported = ProofTree.import(formula, exported)
        assertEquals(pt, imported)
    }

}
