

class RuleException(msg: String) : Exception(msg)

sealed class ProofRule(val name: String, val displayName: String) {
    abstract val schema: String
    abstract val promptedVar: String?
    abstract fun canApply(formula: Formula, assumptions: Set<Formula>): Boolean
    abstract fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula? = null): List<Formula>
}

var allRules = listOf(
    AxiomRule,
    AndIntro, AndElim1, AndElim2,
    OrIntro1, OrIntro2, OrElim,
    ImplIntro, ImplElim,
    NotIntro, NotElim,
    ReductioAdAbsurdum
)

// needed for saving and loading
// val ruleMap = allRules.map {it.name to it}.toMap()

object AxiomRule: ProofRule("ax", "Ax") {
    override val schema = "<hr> A \u2192 A"
    override val promptedVar = null

    override fun canApply(formula: Formula, assumptions: Set<Formula>): Boolean
    {
        val contains = assumptions.contains(formula)
        println("$formula in $assumptions is $contains")
        return contains
    }

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

object OrIntro1: ProofRule("orI1", "\u2228I\u2097") {
    override val schema = "A<hr>A \u2228 B"
    override val promptedVar = null

    override fun canApply(formula: Formula, assumptions: Set<Formula>) =
        formula is Disj

    override fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula?) =
        listOf((formula as Disj).sub1)
}

object OrIntro2: ProofRule("orI2", "\u2228I\u1d63") {
    override val schema = "B<hr>A \u2228 B"
    override val promptedVar = null

    override fun canApply(formula: Formula, assumptions: Set<Formula>) =
        formula is Disj

    override fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula?) =
        listOf((formula as Disj).sub2)
}

object OrElim: ProofRule("orE", "\u2228E") {
    override val schema = "<span class=\"prompted\">A \u2228 B</space> &emsp; A \u2192 C &emsp; B \u2192 C<hr>C";
    override val promptedVar = "A \u2228 B"

    override fun canApply(formula: Formula, assumptions: Set<Formula>) = true
    override fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula?): List<Formula> {
        if (input is Disj) {
            val a = input.sub1
            val b = input.sub2
            return listOf(input, Implication(a, formula), Implication(b, formula))
        } else {
            throw RuleException("Für diese Regel muss die Eingabe eine Disjunktion sein. $input ist keine Implikation.")
        }
    }
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

object ImplElim: ProofRule("impE", "\u2192E") {
    override val schema = "<span class=\"prompted\">A</span> &emsp; <span class=\"prompted\">A</span> \u2192 B<hr>B"
    override val promptedVar = "A"
    override fun canApply(formula: Formula, assumptions: Set<Formula>) = true
    override fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula?): List<Formula> {
        val inp = input ?: FAIL("Missing formula")
        return listOf(inp, Implication(inp, formula))
    }
}

object NotIntro: ProofRule("notI", "¬I") {
    override val schema = "A \u2192 \u22a5<hr>¬A"
    override val promptedVar = null
    override fun canApply(formula: Formula, assumptions: Set<Formula>) =
        formula is Neg

    override fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula?) =
        listOf(Implication((formula as Neg).sub, False))
}

object NotElim: ProofRule("notE", "¬E") {
    override val schema = "<span class=\"prompted\">A &emsp; ¬A</span><hr>\u22a5"
    override val promptedVar = "A"
    override fun canApply(formula: Formula, assumptions: Set<Formula>) =
        formula is False

    override fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula?): List<Formula> {
        val inp = input ?: FAIL("Missing formula")
        return listOf(inp, Neg(inp))
    }
}


// NON INTUITIONISTIC!
object ReductioAdAbsurdum: ProofRule("RAA", "RAA") {
    override val schema = "¬A \u2192 \u22a5<hr>A"
    override val promptedVar = null
    override fun canApply(formula: Formula, assumptions: Set<Formula>) =
        true

    override fun apply(formula: Formula, assumptions: Set<Formula>, input: Formula?) =
        listOf(Implication(Neg(formula), False))
}