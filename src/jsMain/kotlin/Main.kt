import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.html
import kotlinx.html.js.span
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent

const val defaultFormula = "P & Q -> Q & P"

var theProofTree : ProofTree? = null
var lastMenuEvent: Event? = null
var autoMode = false
var proof: String? = null


fun clickLeaf(e: Event, idx: String) {
    val mouseEv = e as MouseEvent
    println(idx)

    val proofTree = theProofTree ?: return

    val menu = proofTree.makeMenu(idx, false)
    menu.setAttribute("style", "left: ${mouseEv.pageX}px; top: ${mouseEv.pageY}px;")
    lastMenuEvent = e
}

fun clickForward(e: Event, idx: String) {
    val mouseEv = e as MouseEvent
    println(idx)

    val proofTree = theProofTree ?: return

    val menu = proofTree.makeMenu(idx, forward = true)
    menu.setAttribute("style", "left: ${mouseEv.pageX}px; top: ${mouseEv.pageY}px;")
    lastMenuEvent = e
}

fun clickPremise(e: Event, idx: String, number: Int) {
    val mouseEv = e as MouseEvent
    val menu = makeAssumptionMenu(idx, number)
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

    (document.getElementById("copyLink") as HTMLElement).onclick = { copyLink() }

    val argFormula = interpretURL()

    try {
        println(argFormula)
        val goal = formulaGrammar.parseToEnd(argFormula)
        val pt = when {
            autoMode -> findProof(goal) ?: ProofTree(goal)
            proof != null -> ProofTree.import(goal, proof!!)
            else -> ProofTree(goal)
        }
        setProofTree(pt)
    } catch(e: RuntimeException) {
        window.alert("Cannot parse '$argFormula'")
        e.printStackTrace()
    }
}

fun copyLink() {
    println("Copy Link")
    val qmark = document.URL.indexOf('?')
    val baseURL = if(qmark > 0) document.URL.substring(0, qmark) else document.URL
    val form = theProofTree?.formula?.toASCII()
    val proof = theProofTree?.export()
    val con = if(allRules.contains(ReductioAdAbsurdum)) "" else ";constructive"
    val url = "$baseURL?$form;proof=$proof$con"
    window.navigator.clipboard.writeText(url);

    val copiedBox = document.getElementById("copied") as HTMLElement
    copiedBox.style.visibility = "";
    copiedBox.classList.remove("fadeout")

    window.setTimeout(
        {
        copiedBox.classList.add("fadeout");}, 3000);
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
    autoMode = parts.contains("auto")
    if(parts.contains("constructive") || parts.contains("intuitionsitic") || parts.contains("BHK")) {
        allRules = allRules - ReductioAdAbsurdum
    }
    proof = parts.firstOrNull { it.startsWith("proof=") }?.substring(6)
    if(proof != null) proof = decode(proof!!)
    return decode(result)
}

fun setProofTree(pr: ProofTree) {
    val p2 = document.getElementById("proof") ?: TODO()
    p2.children.asList().forEach { p2.removeChild(it) }
    val container = document.create.html {
        pr.layout("x", setOf(), false, consumer)
    }
    p2.appendChild(container.firstChild ?: FAIL("cannot happen"))
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