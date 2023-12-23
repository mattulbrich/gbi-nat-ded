import java.io.BufferedReader
import java.io.InputStreamReader
import javax.swing.JOptionPane
import kotlin.system.exitProcess

val P = formulaGrammar::parseToEnd
fun main(args: Array<String>) {

    val argFormula =
        readInput() ?: TODO()
        //  "((a->b)->a)->a"
        // "P&Q -> Q&P"
        // "(P -> 0) -> -P"
        // "P | -P"
        // "P | (P->0)"
        // "P -> -P -> 0"// args[0]
//    val goal = formulaGrammar.parseToEnd(argFormula)
//    val pt = findProof(goal, listOf())
//    pt?.printTree()
//    pt?.verify()

    val commands = mutableListOf<String>(
        "auto x",
        //"impI x", "RAA x0", "impI x00", "ax x000 (a->b)->a", "impE x0000", "impI x00000"
//        "impI x", "andI x0", "ax x00 Q&P", "ax x01 Q&P", "andE1 x000", "andE2 x010"
    )

    // println(forwardProofs(listOf(P("P"), P("(P->Q)->R")), setOf()))

    var pt = ProofTree(formulaGrammar.parseToEnd(argFormula))
    while(true) {
        pt.printTree()
        try {
            print("> ")
            val x = if(commands.isEmpty()) {
                readInput() ?: exitProcess(0)
            } else  {
                commands.removeFirst()
            }.trim()
            if (x.isEmpty()) return

            println("_ $x")
            val command = x.split(" ")
            val rule = ruleMap[command[0]]
            if (rule == null) {
                when(command[0]) {
                    "ax" -> pt = pt.startForwardProof(command[1], formulaGrammar.parseToEnd(command[2]))
                    "auto" -> pt = applyAuto(pt, command[1])
                    "exit" -> exitProcess(0)
                    else -> FAIL("Unknown rule")
                }
            } else {
                pt = pt.apply(command[1], rule)
            }
            pt = pt.removeGaps()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}

fun applyAuto(pt: ProofTree, s: String, ps: Set<Formula> = setOf()): ProofTree {
    val idx = s.drop(1)
    if(idx == "") {
        assert(pt.appliedRule == null)
        val newTree = findProof(pt.formula, ps) ?: FAIL("No proof found.")
        return newTree
    }
    val ps2 = ImplIntro.filterAvailableAssumption(pt, ps)
    val no = s[0].digitToInt()
    val updChildren = pt.children.updated(no, applyAuto(pt.children[no], idx, ps2))
    return ProofTree(pt.formula, pt.appliedRule, updChildren)
}

fun readInput(): String? =
    // readln() // ;-(
    JOptionPane.showInputDialog("Command")

fun ProofTree.printTree(indent: Int = 0, idx: String = "x") {
    print("   ".repeat(indent))
    val ass = usedAssumptions()
    println("$idx: $ass |- $formula by ${appliedRule?.name}")
    var count = 0
    children.forEach {
        it.printTree(indent + 1, idx + count)
        count ++
    }
}
