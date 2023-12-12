


fun findProof(formula: Formula, premises: List<Formula>): ProofTree? {
    if(premises.contains(formula)) {
        return ProofTree(formula, AxiomRule, listOf())
    }
    
    val forwards = forwardProofs(premises)
    val forwardProof = forwards.find { it.formula == formula }
    if (forwardProof != null) {
        return forwardProof
    }

    when {

        formula is Implication -> {
            val proof = findProof(formula.sub2, premises + formula.sub1)
            if (proof != null) {
                return ProofTree(formula, ImplIntro, listOf(proof))
            }
        }

        formula is Conj -> {
            val proofA = findProof(formula.sub1, premises)
            if (proofA != null) {
                val proofB = findProof(formula.sub2, premises)
                if (proofB != null) {
                    return ProofTree(formula, AndIntro, listOf(proofA, proofB))
                }
            }
        }

        formula is Disj -> {
            val proofA = findProof(formula.sub1, premises)
            if (proofA != null) {
                return ProofTree(formula, OrIntro1, listOf(proofA))
            }
            val proofB = findProof(formula.sub2, premises)
            if (proofB != null) {
                return ProofTree(formula, OrIntro2, listOf(proofB))
            }
        }
    }

    forwards.forEach {
        if(it.formula is Disj) {
            val proofA = findProof(formula, premises + it.formula.sub1)
            if(proofA != null) {
                val proofB = findProof(formula, premises + it.formula.sub2)
                if (proofB != null) {
                    return ProofTree(formula, OrElim, listOf(it, proofA, proofB))
                }
            }
        }
    }

    val raaProof = forwardProofs(premises + Neg(formula)).find { it.formula is False }
    if (raaProof != null) {
        return raaProof
    }

    return null
}

fun forwardProofs(premises: List<Formula>): Set<ProofTree> =
    premises.flatMap {
        val ax = ProofTree(it, AxiomRule, listOf())
        forwardProofs(premises, ax)
    }.toSet()

fun forwardProofs(premises: List<Formula>, proofTree: ProofTree): Set<ProofTree> =
    when(proofTree.formula) {
        is Conj ->
            forwardProofs(premises, ProofTree(proofTree.formula.sub1, AndElim1, listOf(proofTree))) +
            forwardProofs(premises, ProofTree(proofTree.formula.sub2, AndElim2, listOf(proofTree)))
        is Disj -> {
            val proofA = findProof(proofTree.formula.sub1, premises)
            if(proofA != null) {
                val proof = ProofTree(proofTree.formula.sub2, ImplElim, listOf(proofA, proofTree))
                forwardProofs(premises, proof)
            } else {
                setOf()
            }
        }
        else -> setOf()
    } + proofTree
