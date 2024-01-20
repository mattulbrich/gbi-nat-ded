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

    fun export(ass: List<Formula> = listOf()): String {
        if (appliedRule == null) return "open|";

        val assNow = if(appliedRule == ImplIntro && !ass.contains((formula as Implication).sub1))
            ass + formula.sub1 else ass

        if (appliedRule == AxiomRule) {
            val index = ass.indexOf(formula)
            assert(index >= 0, "Unknown formula $formula")
            return appliedRule.name + " $index|"
        }

        return appliedRule.name +
                "|" + children.map { it.export(assNow) }.joinToString("")
    }

    companion object {
        fun import(formula: Formula, proof: String): ProofTree {
            val pt = ProofTree(formula)
            val steps = proof.substring(0, proof.length - 1).split("|").toMutableList()
            return pt.importFrom(steps, listOf())
        }
    }

    private fun importFrom(steps: MutableList<String>, ass: List<Formula>): ProofTree {

        assert(appliedRule == null, "There must not yet be a rule app")

        val step = steps.removeFirst()

        if(step.startsWith("ax ")) {
            val idx = step.substring(3).toInt()
            val formula = ass[idx]
            return AxiomRule.assume(formula)
        }

        if(step == "open") {
            // it is as it is ...
            return this
        }

        if(step == "gap") {
            val fwstep = this.importFrom(steps, ass)
            return ProofTree(formula, Gap, listOf(fwstep))
        }

        val rule = ruleMap[step]
        if(rule == null) FAIL("Unknown rule $step")

        if(rule.canApply(formula, false)) {
            // backward application in depth. ...
            val onestep = rule.apply(this)
            val assNow = if(rule == ImplIntro && !ass.contains((formula as Implication).sub1))
                ass + formula.sub1 else ass
            val newchildren = onestep.children.map { it.importFrom(steps, assNow) }
            return ProofTree(formula, rule, newchildren)
        }

        val forward = this.importFrom(steps, ass)
        if(rule.canApply(forward.formula, true)) {
            val fwstep = rule.apply(forward, formula)
            val newChildren = fwstep.children.mapIndexed { index, tree ->
                if(index == 0) tree
                else tree.importFrom(steps, ass)
            }
//            assert(fwstep.formula == formula, "different formulas ${fwstep.formula} and $formula")
            return ProofTree(fwstep.formula, rule, newChildren)
        }

        FAIL("Nope. cannot proof apply here.")
    }

}

/*
if (appliedRule.isForward) {
            var pt: ProofTree = this
            val result = StringBuilder()
            while (true) {
                assert(pt.children.size <= 1, "Expecting at most one child, not " + children.size)
                if(pt.children.isEmpty()) {
                    assert(pt.appliedRule == AxiomRule, "Expected axiom at leaf, not " + pt.appliedRule?.name)
                    val index = ass.indexOf(pt.formula)
                    assert(index >= 0, "Unknown formula $formula")
                    result.insert(0, pt.appliedRule?.name + " $index|")
                    return result.toString()
                }
                result.insert(0, pt.appliedRule?.name + "|")
                pt = pt.children.first()
            }
        }

 */
