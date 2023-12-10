import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.parser.ParseException
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.HTMLTag
import kotlinx.html.dom.append
import kotlinx.html.js.onClickFunction
import kotlinx.html.js.span
import kotlinx.html.visit
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent

const val defaultFormula = "P & Q -> Q & P"

var theProofTree : ProofTree? = null
var lastMenuEvent: Event? = null


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

fun assert(B: Boolean, m:String? = null) {
    if(!B) throw RuntimeException("Assertion failed. $m")
}

fun FAIL(m: String): Nothing = throw RuntimeException(m)

fun main() {

//    val erg = window.prompt("Formel eingeben") ?: FAIL("???")
//    val formula = formulaGrammar.parseToEnd(erg)
//    window.alert(formula.toString() + " " + formula.toASCII())

    window.onclick = { event ->
        println(event.target)
        if(event != lastMenuEvent && event.target !is HTMLInputElement) {
            val menu = document.getElementById("menu") as HTMLElement
            menu.style.display = "none"
        }
        Unit
    }

    try {
        val qmark = document.URL.indexOf('?')
        val argFormula = if(qmark > 0)
            decode(document.URL.substring(qmark + 1))
        else defaultFormula

        try {
            println(argFormula)
            val goal = formulaGrammar.parseToEnd(argFormula)
            var pt = ProofTree(goal)
//            pt = pt.apply("x", ImplIntro)
//            pt = pt.apply("x0", AndIntro)
//            pt = pt.apply("x00", AndElim2, Atom(Term("P")))
//            pt = pt.apply("x000", AxiomRule)
//            pt = pt.apply("x01", AndElim1, Atom(Term("Q")))
            // pt = pt.apply("x010", AxiomRule)
            setProofTree(pt)
        } catch(e: ParseException) {
            window.alert("Cannot parse '$argFormula")
            e.printStackTrace()
        }

    } catch (e: ParseException) {
        println("Cannot parse: " + e.message)
    }
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