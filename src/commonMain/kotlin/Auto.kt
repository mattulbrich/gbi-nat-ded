


fun findProof(formula: Formula, premises: List<Formula> = listOf(), alreadySeen: Set<Formula> = setOf()): ProofTree? {

    if (alreadySeen.contains(formula)) {
        return null
    }

    if(premises.contains(formula)) {
        return ProofTree(formula, AxiomRule, listOf())
    }

    val alreadySeen2 = alreadySeen + formula

    val forwards = forwardProofs(premises, alreadySeen2)
    val forwardProof = forwards.find { it.formula == formula }
    if (forwardProof != null) {
        return forwardProof
    }

    when (formula) {
        is Implication -> {
            val proof = findProof(formula.sub2, premises + formula.sub1, alreadySeen2)
            if (proof != null) {
                return ProofTree(formula, ImplIntro, listOf(proof))
            }
        }

        is Neg -> {
            val proof = findProof(Implication(formula.sub, False), premises, alreadySeen2)
            if (proof != null) {
                return ProofTree(formula, NotIntro, listOf(proof))
            }
        }

        is Conj -> {
            val proofA = findProof(formula.sub1, premises, alreadySeen2)
            if (proofA != null) {
                val proofB = findProof(formula.sub2, premises, alreadySeen2)
                if (proofB != null) {
                    return ProofTree(formula, AndIntro, listOf(proofA, proofB))
                }
            }
        }

        is Disj -> {
            val proofA = findProof(formula.sub1, premises, alreadySeen2)
            if (proofA != null) {
                return ProofTree(formula, OrIntro1, listOf(proofA))
            }
            val proofB = findProof(formula.sub2, premises, alreadySeen2)
            if (proofB != null) {
                return ProofTree(formula, OrIntro2, listOf(proofB))
            }
        }

        else -> {}
    }

    forwards.forEach {
        if(it.formula is Disj) {
            val proofA = findProof(formula, premises + it.formula.sub1, alreadySeen2)
            if(proofA != null) {
                val proofB = findProof(formula, premises + it.formula.sub2, alreadySeen2)
                if (proofB != null) {
                    return ProofTree(formula, OrElim, listOf(it, proofA, proofB))
                }
            }
        }
    }

    val raaProof = forwardProofs(premises + Neg(formula), alreadySeen2).find { it.formula is False }
    if (raaProof != null) {
        return raaProof
    }

    return null
}

fun forwardProofs(premises: List<Formula>, alreadySeen: Set<Formula>): Set<ProofTree> {
    var result = premises.flatMap {
        val ax = ProofTree(it, AxiomRule, listOf())
        forwardProofs(premises, ax, alreadySeen)
    }.toSet()

    outer@for (p1 in result) {
        for (p2 in result) {
            if (p1.formula == Neg(p2.formula)) {
                result += ProofTree(False, NotElim, listOf(p1, p2))
                outer@ break
            }
        }
    }

    return result
}

fun forwardProofs(premises: List<Formula>, proofTree: ProofTree, alreadySeen: Set<Formula>): Set<ProofTree> =
    when(proofTree.formula) {
        is Conj ->
            forwardProofs(premises, ProofTree(proofTree.formula.sub1, AndElim1, listOf(proofTree)), alreadySeen) +
                    forwardProofs(premises, ProofTree(proofTree.formula.sub2, AndElim2, listOf(proofTree)), alreadySeen)

        is Neg -> {
            if(!alreadySeen.contains(proofTree.formula.sub)) {
                val proofA = findProof(proofTree.formula.sub, premises, alreadySeen)
                if (proofA != null) {
                    val proof = ProofTree(False, NotElim, listOf(proofA, proofTree))
                    forwardProofs(premises, proof, alreadySeen)
                } else {
                    setOf()
                }
            } else {
                setOf()
            }
        }

        is Implication -> {
            val proofA = findProof(proofTree.formula.sub1, premises)
            if(proofA != null) {
                val proof = ProofTree(proofTree.formula.sub2, ImplElim, listOf(proofA, proofTree))
                forwardProofs(premises, proof, alreadySeen)
            } else {
                setOf()
            }
        }

        else -> setOf()
    } + proofTree
