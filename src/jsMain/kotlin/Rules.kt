import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.parser.ParseException
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.div
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.span
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSpanElement

data class ProofTree(val formula: Formula, val appliedRule: ProofRule?, val children: List<ProofTree>) {

    constructor(formula: Formula): this(formula, null, listOf())

    val isLeaf: Boolean
        get() = appliedRule == null

    val isClosed: Boolean
        get() = !isLeaf && children.all { it.isClosed }

    fun layout(idx: String, availablePremises: Set<Formula>): HTMLSpanElement {
        val ruledness = if (isLeaf) "unruled" else "ruled"

        val result = document.create.span("subproof $ruledness") {
            id = idx
            if(isLeaf) {
                onClickFunction = { e -> clickLeaf(e, idx) }
            }
        }

        val childAvailablePremises =
            ImplIntro.filterAvailableAssumption(this, availablePremises)

        children.zip(children.indices).map { (tree, no) ->
            result.appendChild(tree.layout("$idx$no", childAvailablePremises))
        }

        //
        // print premises |- formula
        result.append {
            hr(ruledness)
            when {
                // actually used premises
                isClosed -> span { +usedAssumptions().joinToString(", ") }
                // available premises if there are any
                availablePremises.isNotEmpty() ->
                    span("openPremises") {
                        +"?"
                        span("tooltip") {
                            +"Available premises:"
                            availablePremises.forEach {
                                br
                                +it.toString()
                            }
                        }
                    }
            }
            // the formula
            span("formula") { +" \u22a2 "
                +formula.toString() }
        }
        val actResult = document.create.span("namedRule")
        actResult.appendChild(result)

        if(appliedRule != null)
            actResult.append {
                span("rulename") {  + ("(" + appliedRule.displayName + ")") }
            }

        return actResult
    }

    fun makeMenu(idx: String, assumptions: Set<Formula> = setOf()): HTMLDivElement {
        val (tree, availAssumptions) = navigate(idx)
        val menu = document.getElementById("menu") as HTMLDivElement
        menu.innerHTML = ""
        menu.append {
            tree.getApplicableRules(availAssumptions).forEach {
                div {
                        onClickFunction = { e ->
                            if(e.target !is HTMLInputElement) {
                                // Ignore clicks in input fields
                                menu.style.display = "none"
                                val tr = theProofTree ?: FAIL("missing main tree")
                                println(it.displayName)
                                if (it.promptedVar != null) {
                                    val inputField = document.getElementById("input-${it.name}") as HTMLInputElement
                                    println("IF '${inputField}'")
                                    println("IF '${inputField.value}'")
                                    val inputText = inputField.value
                                    try {
                                        val inputFormula = formulaGrammar.parseToEnd(inputText)
                                        println(inputFormula)
                                        setProofTree(tr.apply(idx, it, inputFormula))
                                    } catch (e: ParseException) {
                                        window.alert("Cannot parse '$inputText' as a formula or term")
                                    }
                                } else {
                                    setProofTree(tr.apply(idx, it))
                                }
                            }
                        }
                        +it.displayName
                    if (it.promptedVar != null) {
                        unsafe {+ "&nbsp;&nbsp;${it.promptedVar}=" }
                        input(InputType.text) {
                            id = "input-${it.name}"
                        }
                    }
                    div("tooltip") {
                        + "Schema for this rule:"
                        div("schema") {
                            unsafe { + it.schema }
                        }
                        if (it.promptedVar != null) {
                            + "${it.promptedVar} must be provided!"
                        }
                    }
                }
            }
            if(idx != "x") {
                div {
                    onClickFunction = { e ->
                        setProofTree((theProofTree ?: FAIL("missing main tree")).remove(idx)) }
                    + "Rückgängig"
                }
            }
        }
        return (menu as HTMLDivElement)
    }

    fun apply(idx: String, rule: ProofRule, input: Formula? = null): ProofTree {
        return apply(idx, rule, input, setOf<Formula>())
    }

    private fun apply(idx: String, rule: ProofRule, input: Formula?, assumptions: Set<Formula>): ProofTree {
        // println("Applying onto $idx")
        val dropped = idx.substring(1);
        if(dropped.length == 0 ) {
            assert(appliedRule == null, "There is already rule applied: $appliedRule")
            val newkids = rule.apply(formula, assumptions, input)
            val newtrees = newkids.map { ProofTree(it)}
            return ProofTree(formula, rule, newtrees)
        } else {
            val no = dropped[0].digitToInt()
            val newass = if(appliedRule is ImplIntro) assumptions + formula else assumptions
            val updChildren = children.updated(no, children[no].apply(dropped, rule, input, newass))
            return ProofTree(formula, appliedRule, updChildren)
        }

    }

    private fun navigate(idx: String): Pair<ProofTree, Set<Formula>> {
        var dropped = idx.substring(1);
        var tree = this
        var availAssumptions = setOf<Formula>()
        while(dropped.length > 0) {
            val no = dropped[0].digitToInt()
            availAssumptions = ImplIntro.filterAvailableAssumption(tree, availAssumptions)
            tree = tree.children[no]
            dropped = dropped.substring(1)
        }
        return tree to availAssumptions
    }

    fun getApplicableRules(idx: String): List<ProofRule> {
        val (tree, availAssumptions) = navigate(idx)
        return tree.getApplicableRules(availAssumptions)
    }

    private fun getApplicableRules(availAssumptions: Set<Formula>): List<ProofRule> =
        allRules.filter { it.canApply(formula, availAssumptions) }

    fun remove(idx: String): ProofTree {
        val dropped = idx.substring(1)
        if(dropped.length == 1) {
            return ProofTree(formula, null, listOf())
        } else {
            val no = dropped[0].digitToInt()
            val updChildren = children.updated(no, children[no].remove(dropped))
            return ProofTree(formula, appliedRule, updChildren)
        }
    }

    fun usedAssumptions(): Set<Formula> {
        var result = children.map { it.usedAssumptions() }.flatten().toSet()
        if(appliedRule is AxiomRule) {
            result += formula
        }
        if (appliedRule is ImplIntro) {
            result -= (formula as Implication).sub1
        }
        return result
    }
}

sealed class ProofRule(val name: String, val displayName: String) {
    abstract val schema: String
    abstract val promptedVar: String?
    abstract fun canApply(formula: Formula, assumptions: Set<Formula>): Boolean
    abstract fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula? = null): List<Formula>
}

val allRules = listOf(
    AxiomRule,
    AndIntro, AndElim1, AndElim2,
    ImplIntro
)

val ruleMap = allRules.map {it.name to it}.toMap()

object AxiomRule: ProofRule("ax", "Ax") {
    override val schema = "<hr> A \u2192 A"
    override val promptedVar = null

    override fun canApply(formula: Formula, assumptions: Set<Formula>): Boolean
    {
        val contains = assumptions.contains(formula)
        println("$formula in $assumptions is $contains")
        return contains
    }

//    =
//        assumptions.contains(formula)

    override fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula?) =
        listOf<Formula>()

}

object AndIntro: ProofRule("andI", "\u2227I") {
    override val schema = "A &emsp; B<hr>A \u2227 B"
    override val promptedVar = null

    override fun canApply(formula: Formula, assumptions: Set<Formula>) =
        formula is Conj

    override fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula?) =
        listOf((formula as Conj).sub1, formula.sub2)
}

object AndElim1: ProofRule("andE1", "\u2227E\u2097") {
    override val schema = "A \u2227 <span class=\"prompted\">B</span><hr>A"
    override val promptedVar = "B"

    override fun canApply(formula: Formula, assumptions: Set<Formula>) = true

    override fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula?) =
        listOf(Conj(formula, input ?: FAIL("input required!")))
}

object AndElim2: ProofRule("andE2", "\u2227E\u1d63") {
    override val schema = "<span class=\"prompted\">B</span> \u2227 A<hr>A"
    override val promptedVar = "B"

    override fun canApply(formula: Formula, assumptions: Set<Formula>) = true

    override fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula?) =
        listOf(Conj(input ?: FAIL("input required!"), formula))
}

object ImplIntro: ProofRule("impI", "\u2192I") {
    override val schema = "A \u22a2 B<hr>A \u2192 B"
    override val promptedVar = null

    override fun canApply(formula: Formula, assumptions: Set<Formula>) =
        formula is Implication

    override fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula?) =
        listOf((formula as Implication).sub2)

    fun filterAvailableAssumption(tree: ProofTree, assumptions: Set<Formula>): Set<Formula> =
        if(tree.appliedRule == ImplIntro) {
            assert(tree.children.size == 1, "Impl intro: ${tree.children.size}")
            assert(tree.formula is Implication, "Impl intro w/o Impl: ${tree.formula}")
            assumptions + (tree.formula as Implication).sub1
        } else {
            assumptions
        }
}
