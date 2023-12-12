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

fun ProofTree.layout(idx: String, availablePremises: Set<Formula>): HTMLSpanElement {
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


fun ProofTree.makeMenu(idx: String, assumptions: Set<Formula> = setOf()): HTMLDivElement {
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
                            } catch (e: RuleException) {
                                window.alert(e.message ?: "Error while applying rules")
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