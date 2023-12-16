import java.io.BufferedReader
import java.io.InputStreamReader

val P = formulaGrammar::parseToEnd
fun main(args: Array<String>) {

    val argFormula =
        "P&Q -> Q&P"
        // "(P -> 0) -> -P"
        // "P | -P"
        // "P | (P->0)"
        // "P -> -P -> 0"// args[0]
//    val goal = formulaGrammar.parseToEnd(argFormula)
//    val pt = findProof(goal, listOf())
//    pt?.printTree()
//    pt?.verify()

    val commands = mutableListOf("impI x", "andI x0", "ax x00 Q&P", "ax x01 Q&P", "andE1 x000", "andE2 x010")

    // println(forwardProofs(listOf(P("P"), P("(P->Q)->R")), setOf()))

    var pt = ProofTree(formulaGrammar.parseToEnd(argFormula))
    while(true) {
        pt.printTree()
        try {
            print("> ")
            val x = if(commands.isEmpty()) {
                readln()
            } else  {
                commands.removeFirst()
            }
            println("_ $x")
            val command = x.split(" ")
            val rule = ruleMap[command[0]]
            if (rule == null) {
                if (command[0] == "ax") {
                   pt = pt.startForwardProof(command[1], formulaGrammar.parseToEnd(command[2]))
                } else {
                    FAIL("Unknown rule")
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
