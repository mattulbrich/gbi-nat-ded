import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.div
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.onSubmitFunction
import kotlinx.html.js.span
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.events.Event

fun ProofTree.layout(idx: String, availablePremises: Set<Formula>, atGap: Boolean = false,
                     consumer: TagConsumer<*>) {
    val ruledness = when {
        isLeaf -> "unruled"
        appliedRule is Gap -> "gap"
        else -> "ruled"
    }

    consumer.span("namedRule") {
        span("subproof") {

            val childAvailablePremises =
                ImplIntro.filterAvailableAssumption(this@layout, availablePremises)

            children.zip(children.indices).map { (tree, no) ->
                tree.layout("$idx$no", childAvailablePremises, appliedRule is Gap, this.consumer)
            }

            hr(ruledness)

            //
            // print premises |- formula
            when {
                // actually used premises
                isClosed -> span { +usedAssumptions().joinToString(", ") }

                // available premises if there are any
                availablePremises.isNotEmpty() -> span("openPremises") {
                    var count = 0
                    availablePremises.forEach {
                        if (count > 0) {
                            +", "
                        }
                        span("clickable") {
                            val countval = count
                            onClickFunction = { e -> clickPremise(e, idx, countval) }
                            +it.toString()
                        }
                        count++
                    }
//                if (afterGap) {
//                    onClickFunction = { e -> clickForward(e, idx) }
//                }
                }
            }

            // |-
            +" \u22a2 "

            val clickable = if(isLeaf || atGap) " clickable" else ""
            // the formula
            span("formula$clickable") {

                +formula.toString()

                id = idx
                when {
                    isLeaf -> onClickFunction = { e -> clickLeaf(e, idx) }
                    atGap -> onClickFunction = { e -> clickForward(e, idx) }
                }

            }
        }
        if(appliedRule != null && appliedRule != Gap)
            span("rulename") {
                +("(" + appliedRule.displayName + ")")
            }
    }
}

fun makeAssumptionMenu(idx: String, number: Int): HTMLDivElement {
    val menu = document.getElementById("menu") as HTMLDivElement
    menu.innerHTML = ""
    menu.append {
        div {
            onClickFunction = { e ->
                e.preventDefault()
                println(number)
                // Ignore clicks in input fields
                menu.style.display = "none"
                val tr = theProofTree ?: FAIL("missing main tree")
                val (_, assumptions) = tr.navigate(idx)
                val formula = assumptions.toList()[number]
                val newTree = tr.startForwardProof(idx, formula).removeGaps()
                setProofTree(newTree)
            }
            + "Elaborate"
            div("tooltip") {
                + "Schema for the rule"
                div("schema") {
                    unsafe { +AxiomRule.schema }
                }
            }
        }
    }
    return menu
}

fun ProofTree.makeMenu(idx: String, forward: Boolean): HTMLDivElement {
    val (tree, availAssumptions) = navigate(idx)
    val menu = document.getElementById("menu") as HTMLDivElement
    menu.innerHTML = ""
    menu.append {
        tree.getApplicableRules(availAssumptions, forward).forEach {
            fun submit(e: Event) {
                e.preventDefault()
                // Ignore clicks in input fields
                menu.style.display = "none"
                val tr = theProofTree ?: FAIL("missing main tree")
                println(it.displayName)
                val newTree = tr.apply(idx, it).removeGaps()
                setProofTree(newTree)
            }
            div {
                onClickFunction = ::submit
                +it.displayName
                div("tooltip") {
                    + "Schema for this rule:"
                    div("schema") {
                        unsafe { +it.schema }
                    }
                }
            }
        }
        if (idx != "x") {
            div {
                onClickFunction = {
                    setProofTree((theProofTree ?: FAIL("missing main tree")).remove(idx).removeGaps())
                }
                + "Rückgängig"
            }
        }
    }
    return menu
}