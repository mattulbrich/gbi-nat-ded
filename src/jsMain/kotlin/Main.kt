import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent

const val defaultFormula = "P & Q -> Q & P"

var theProofTree : ProofTree? = null
var lastMenuEvent: Event? = null
var autoMode = false


fun clickLeaf(e: Event, idx: String) {
    val mouseEv = e as MouseEvent
    println(idx)

    val proofTree = theProofTree ?: return

    println(proofTree.getApplicableRules(idx).map { it.name });
    val menu = proofTree.makeMenu(idx)
//    val mousePos =
    menu.setAttribute("style", "left: ${mouseEv.pageX}px; top: ${mouseEv.pageY}px;")
    lastMenuEvent = e
}


fun main() {

    window.onclick = { event ->
        println(event.target)
        if(event != lastMenuEvent && event.target !is HTMLInputElement) {
            val menu = document.getElementById("menu") as HTMLElement
            menu.style.display = "none"
        }
        Unit
    }

    val argFormula = interpretURL()

    try {
        println(argFormula)
        val goal = formulaGrammar.parseToEnd(argFormula)
        val pt = if(autoMode) findProof(goal) ?: ProofTree(goal) else ProofTree(goal)
        setProofTree(pt)
    } catch(e: RuntimeException) {
        window.alert("Cannot parse '$argFormula'")
        e.printStackTrace()
    }

}

private fun interpretURL(): String {
    val qmark = document.URL.indexOf('?')
    if (qmark < 0) {
        val input = window.prompt("Input formula to be proved", defaultFormula)
        return input ?: defaultFormula
    }
    val query = document.URL.substring(qmark + 1)
    val parts = query.split(";")
    val result = parts.first()
    allRules = allRules.filterNot { parts.drop(1).contains(it.name) }
    autoMode = parts.contains("auto")
    return decode(result)
}

fun setProofTree(pr: ProofTree) {
    val p2 = document.getElementById("proof2") ?: TODO()
    p2.children.asList().forEach { p2.removeChild(it) }
    val span = pr.layout("x", setOf())
    p2.appendChild(span)
    theProofTree = pr
}

fun decode(s: String): String {
    val str = s.replace('+', ' ')
    val result = StringBuilder()
    var start = 0
    var i = str.indexOf('%', start)
    while(i >= 0) {
        result.append(str.substring(start, i))
        start = i + 1
        val encoded = str.substring(start, start + 2)
        println(encoded)
        val decoded = encoded.toInt(16)
        result.append(decoded.toChar())
        start += 2
        i = str.indexOf('%', start)
    }
    result.append(str.substring(start))
    return result.toString()
}