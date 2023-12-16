data class ProofTree(val formula: Formula, val appliedRule: ProofRule?, val children: List<ProofTree>) {

    constructor(formula: Formula): this(formula, null, listOf())

    val isLeaf: Boolean
        get() = appliedRule == null

    val isClosed: Boolean
        get() = !isLeaf && children.all { it.isClosed }

    fun apply(idx: String, rule: ProofRule): ProofTree {
        return apply(idx, rule, null)
    }

    private fun apply(idx: String, rule: ProofRule, belowFormula: Formula?): ProofTree {
        // println("Applying onto $idx")
        val dropped = idx.substring(1);
        if(dropped.length == 0 ) {
            val newTree = rule.apply(this, belowFormula)
            return newTree
        } else {
            val no = dropped[0].digitToInt()
            val updChildren = children.updated(no, children[no].apply(dropped, rule, formula))
            return ProofTree(formula, appliedRule, updChildren)
        }
    }

    fun startForwardProof(idx: String, assumption: Formula): ProofTree {
        val dropped = idx.substring(1);
        if(dropped.length == 0 ) {
            return AxiomRule.apply(this, assumption)
        } else {
            val no = dropped[0].digitToInt()
            val updChildren = children.updated(no, children[no].startForwardProof(dropped, assumption))
            return ProofTree(this.formula, appliedRule, updChildren)
        }
    }

    fun verify(assumptions: Set<Formula> = setOf()) {
        if (appliedRule != null) {
            appliedRule.check(this, assumptions)
            val newAssumptions = if(appliedRule == ImplIntro) assumptions + (formula as Implication).sub1 else assumptions
            children.forEach { it.verify(newAssumptions) }
        }
    }

    fun navigate(idx: String): Pair<ProofTree, Set<Formula>> {
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

    fun getApplicableRules(availAssumptions: Set<Formula>, forward: Boolean): List<ProofRule> =
        allRules.filter { it.canApply(formula, forward) }

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

    fun removeGaps() : ProofTree =
        if(appliedRule == Gap && formula == children[0].formula) children[0]
        else ProofTree(formula, appliedRule, children.map { it.removeGaps() })
}
