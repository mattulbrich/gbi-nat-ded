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
                availablePremises.isNotEmpty() -> {
                    availablePremises.forEach {
                        var count = 0
                        span("openPremise") {
                            onClickFunction = { e -> clickPremise(e, idx, count) }
                            +it.toString()
                        }
                        if (count > 0) {
                            +", "
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

            // the formula
            span("formula") {

                +formula.toString()

                id = idx
                when {
                    isLeaf -> onClickFunction = { e -> clickLeaf(e, idx) }
                    atGap -> onClickFunction = { e -> clickForward(e, idx) }
                }

            }
        }
        span("rulename") {
            +("(" + appliedRule.displayName + ")")
        }
    }
}


fun ProofTree.makeMenu(idx: String, forward: Boolean): HTMLDivElement {
    val (tree, availAssumptions) = navigate(idx)
    val menu = document.getElementById("menu") as HTMLDivElement
    menu.innerHTML = ""
    menu.append {
        tree.getApplicableRules(availAssumptions).forEach {
            fun submit(e: Event) {
                if (e.type == "click" && e.target is HTMLInputElement) {
                    return
                }
                e.preventDefault()
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
                    } catch (e: RuntimeException) {
                        window.alert("Cannot parse '$inputText' as a formula or term")
                    } catch (e: RuleException) {
                        window.alert(e.message ?: "Error while applying rules")
                    }
                } else {
                    setProofTree(tr.apply(idx, it))
                }
            }
            form {
                onSubmitFunction = ::submit
                div {
                    onClickFunction = ::submit
                    +it.displayName
                    if (it.promptedVar != null) {
                        unsafe { +"&nbsp;&nbsp;${it.promptedVar}=" }
                        val id = "input-${it.name}"
                        input(InputType.text) {
                            this.id = id
                        }
                    }
                    div("tooltip") {
                        +"Schema for this rule:"
                        div("schema") {
                            unsafe { +it.schema }
                        }
                        if (it.promptedVar != null) {
                            +"${it.promptedVar} must be provided!"
                        }
                    }
                }
            }
        }
        if (idx != "x") {
            div {
                onClickFunction = {
                    setProofTree((theProofTree ?: FAIL("missing main tree")).remove(idx))
                }
                +"Rückgängig"
            }
        }
    }
    return menu
}