

class RuleException(msg: String) : Exception(msg)

fun ProofTree.extendLeaf(rule:ProofRule, children: List<ProofTree>): ProofTree {
    assert(isLeaf, "tree expected to be a leaf");
    return ProofTree(formula, rule, children)
}

sealed class ProofRule(val name: String, val displayName: String) {
    abstract val schema: String
    abstract fun canApply(formula: Formula, forward: Boolean): Boolean
    abstract fun apply(proofTree: ProofTree, input: Formula? = null): ProofTree
    open fun check(proofTree: ProofTree, assumptions: Set<Formula>) {
    }
}

var allRules = listOf(
    // backwards:
    AndIntro, OrIntro1, OrIntro2, ImplIntro, NotIntro, ReductioAdAbsurdum,
    // forwards:
    AndElim1, AndElim2, OrElim, ImplElim, NotElim,
)

val ruleMap = allRules.map { it.name to it }.toMap()

data object Gap: ProofRule("GAP", "GAP") {
    override val schema
        get() = FAIL("does not apply")

    override fun canApply(formula: Formula, forward: Boolean) =
        false // the application mechanism for assumptions is outside the usual mode

    override fun apply(proofTree: ProofTree, input: Formula?): ProofTree =
        FAIL("does not apply")
}

data object AxiomRule: ProofRule("ax", "Ax") {
    override val schema = "<hr> A \u2192 A"

    override fun canApply(formula: Formula, forward: Boolean): Boolean =
        false // the application mechanism for assumptions is outside the usual mode

    override fun apply(proofTree: ProofTree, input: Formula?): ProofTree {
        assert(proofTree.appliedRule == null)
        assert(input != null)
        return ProofTree(
            proofTree.formula, Gap, listOf(
                ProofTree(input!!, this, listOf())
            )
        )
    }

    fun assume(formula: Formula): ProofTree =
        ProofTree(formula, this, listOf())

}

// Backward rules:

data object AndIntro: ProofRule("andI", "\u2227I") {
    override val schema = "A &emsp; B<hr>A \u2227 B"

    override fun canApply(formula: Formula, forward: Boolean): Boolean =
        !forward && formula is Conj

    override fun apply(proofTree: ProofTree, input: Formula?) =
        proofTree.extendLeaf(this,
            listOf(ProofTree((proofTree.formula as Conj).sub1),
                ProofTree(proofTree.formula.sub2)))

}

data object OrIntro1: ProofRule("orI1", "\u2228I\u2097") {
    override val schema = "A<hr>A \u2228 B"

    override fun canApply(formula: Formula, forward: Boolean): Boolean =
        !forward && formula is Disj

    override fun apply(proofTree: ProofTree, input: Formula?): ProofTree =
        proofTree.extendLeaf(this,
            listOf(ProofTree((proofTree.formula as Disj).sub1)))
}

data object OrIntro2: ProofRule("orI2", "\u2228I\u1d63") {
    override val schema = "B<hr>A \u2228 B"

    override fun canApply(formula: Formula, forward: Boolean): Boolean =
        !forward && formula is Disj

    override fun apply(proofTree: ProofTree, input: Formula?): ProofTree =
        proofTree.extendLeaf(this,
            listOf(ProofTree((proofTree.formula as Disj).sub2)))
}


data object ImplIntro: ProofRule("impI", "\u2192I") {
    override val schema = "A \u22a2 B<hr>A \u2192 B"

    override fun canApply(formula: Formula, forward: Boolean): Boolean =
        !forward && formula is Implication

    override fun apply(proofTree: ProofTree, input: Formula?): ProofTree =
        proofTree.extendLeaf(this,
            listOf(ProofTree((proofTree.formula as Implication).sub2)))

    fun filterAvailableAssumption(tree: ProofTree, assumptions: Set<Formula>): Set<Formula> =
        if(tree.appliedRule == ImplIntro) {
            assert(tree.children.size == 1, "Impl intro: ${tree.children.size}")
            assert(tree.formula is Implication, "Impl intro w/o Impl: ${tree.formula}")
            assumptions + (tree.formula as Implication).sub1
        } else {
            assumptions
        }
}


data object NotIntro: ProofRule("notI", "¬I") {
    override val schema = "A \u2192 \u22a5<hr>¬A"
    override fun canApply(formula: Formula, forward: Boolean): Boolean =
        !forward && formula is Neg

    override fun apply(proofTree: ProofTree, input: Formula?): ProofTree =
        proofTree.extendLeaf(this, listOf(
            ProofTree(Implication((proofTree.formula as Neg).sub, False))))
}


// NON INTUITIONISTIC!
data object ReductioAdAbsurdum: ProofRule("RAA", "RAA") {
    override val schema = "¬A \u2192 \u22a5<hr>A"
    override fun canApply(formula: Formula, forward: Boolean): Boolean =
        !forward

    override fun apply(proofTree: ProofTree, input: Formula?): ProofTree =
        proofTree.extendLeaf(this, listOf(
            ProofTree(Implication(Neg(proofTree.formula), False))))
}

// forward rules

data object AndElim1: ProofRule("andE1", "\u2227E\u2097") {
    override val schema = "A \u2227 <span class=\"prompted\">B</span><hr>A"

    override fun canApply(formula: Formula, forward: Boolean): Boolean =
        forward && formula is Conj

    override fun apply(proofTree: ProofTree, input: Formula?): ProofTree =
        ProofTree((proofTree.formula as Conj).sub1, this, listOf(proofTree))

}

data object AndElim2: ProofRule("andE2", "\u2227E\u1d63") {
    override val schema = "<span class=\"prompted\">B</span> \u2227 A<hr>A"

    override fun canApply(formula: Formula, forward: Boolean): Boolean =
        forward && formula is Conj

    override fun apply(proofTree: ProofTree, input: Formula?): ProofTree =
        ProofTree((proofTree.formula as Conj).sub2, this, listOf(proofTree))

}


data object OrElim: ProofRule("orE", "\u2228E") {
    override val schema = "<span class=\"prompted\">A \u2228 B</space> &emsp; A \u2192 C &emsp; B \u2192 C<hr>C"

    override fun canApply(formula: Formula, forward: Boolean): Boolean =
        forward && formula is Disj

    override fun apply(proofTree: ProofTree, input: Formula?): ProofTree {
        if (proofTree.formula is Disj) {
            assert(input != null, "Input must not be null")
            val a = proofTree.formula.sub1
            val b = proofTree.formula.sub2
            return ProofTree(
                input!!, this, listOf(
                    proofTree,
                    ProofTree(Implication(a, input)),
                    ProofTree(Implication(b, input))
                )
            )
        } else {
            throw RuleException("Für diese Regel muss die Eingabe eine Disjunktion sein. $input ist keine Implikation.")
        }
    }
}


data object ImplElim : ProofRule("impE", "\u2192E") {
    override val schema = "<span class=\"prompted\">A</span> &emsp; <span class=\"prompted\">A</span> \u2192 B<hr>B"
    override fun canApply(formula: Formula, forward: Boolean): Boolean =
        forward && formula is Implication

    override fun apply(proofTree: ProofTree, input: Formula?): ProofTree {
        val imp = proofTree.formula as Implication
        return ProofTree(
            imp.sub2, this, listOf(
                ProofTree(imp.sub1),
                proofTree
            )
        )
    }
}


data object NotElim : ProofRule("notE", "¬E") {
    override val schema = "<span class=\"prompted\">A &emsp; ¬A</span><hr>\u22a5"
    override fun canApply(formula: Formula, forward: Boolean): Boolean =
        forward && formula is Neg

    override fun apply(proofTree: ProofTree, input: Formula?): ProofTree {
        if(proofTree.formula is Neg) {
        val imp = proofTree.formula as Neg
        return ProofTree(
            False, this, listOf(
                ProofTree(imp.sub),
                proofTree
            )
        )
        } else {
            return ProofTree(
                False, this, listOf(
                    proofTree,
                    ProofTree(Neg(proofTree.formula)),
                )
            )
        }
    }
}

