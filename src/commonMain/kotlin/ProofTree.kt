data class ProofTree(val formula: Formula, val appliedRule: ProofRule?, val children: List<ProofTree>) {

    constructor(formula: Formula): this(formula, null, listOf())

    val isLeaf: Boolean
        get() = appliedRule == null

    val isClosed: Boolean
        get() = !isLeaf && children.all { it.isClosed }

    fun apply(idx: String, rule: ProofRule, input: Formula? = null): ProofTree {
        return apply(idx, rule, input, setOf<Formula>())
    }

    private fun apply(idx: String, rule: ProofRule, input: Formula?, assumptions: Set<Formula>): ProofTree {
        // println("Applying onto $idx")
        val dropped = idx.substring(1);
        if(dropped.length == 0 ) {
            assert(appliedRule == null, "There is already rule applied: $appliedRule")
            val newkids = rule.apply(formula, assumptions, input)
            val newtrees = newkids.map { ProofTree(it) }
            return ProofTree(formula, rule, newtrees)
        } else {
            val no = dropped[0].digitToInt()
            val newass = if(appliedRule is ImplIntro) assumptions + formula else assumptions
            val updChildren = children.updated(no, children[no].apply(dropped, rule, input, newass))
            return ProofTree(formula, appliedRule, updChildren)
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

    fun getApplicableRules(idx: String): List<ProofRule> {
        val (tree, availAssumptions) = navigate(idx)
        return tree.getApplicableRules(availAssumptions)
    }

    fun getApplicableRules(availAssumptions: Set<Formula>): List<ProofRule> =
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