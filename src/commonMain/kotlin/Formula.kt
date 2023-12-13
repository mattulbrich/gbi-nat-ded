fun assert(B: Boolean, m:String? = null) {
    if(!B) throw RuntimeException("Assertion failed. $m")
}

fun FAIL(m: String): Nothing = throw RuntimeException(m)

sealed class Formula {
    abstract val precedence: Int
    abstract fun toASCII(): String
}
abstract class BinaryFormula(open val sub1: Formula, open val sub2: Formula, val op: String, val ascii: String, prec: Int): Formula() {
    override val precedence = prec
    override fun toString(): String {
        val t1 = if(sub1.precedence <= precedence) "($sub1)" else sub1.toString()
        val t2 = if(sub2.precedence < precedence) "($sub2)" else sub2.toString()
        return "$t1 $op $t2"
    }

    override fun toASCII() = "(${sub1.toASCII()} $ascii ${sub2.toASCII()})"
}
abstract class UnaryFormula(open val sub: Formula, val op: String, val ascii: String): Formula() {
    override val precedence = 100
    override fun toString(): String {
        val t1 = if(sub.precedence < precedence) "($sub)" else sub.toString()
        return "$op $t1"
    }

    override fun toASCII() = "$ascii${sub.toASCII()}"
}

// 22A2 is |-
// 22a5 is \bot
data class Implication(override val sub1: Formula, override val sub2: Formula) : BinaryFormula(sub1, sub2, "\u2192", "->", 10) {
    override fun toString(): String = super.toString()
}
data class Disj(override val sub1: Formula, override val sub2: Formula) : BinaryFormula(sub1, sub2, "\u2228", "|", 20){
    override fun toString(): String = super.toString()
}
data class Conj(override val sub1: Formula, override val sub2: Formula) : BinaryFormula(sub1, sub2, "\u2227", "&", 30) {
    override fun toString(): String = super.toString()
}
data class Neg(override val sub: Formula) : UnaryFormula(sub, "¬", "-") {
    override fun toString(): String = super.toString()
}
object False : Formula() {
    override val precedence = 100
    override fun toString() = "\u22a5"
    override fun toASCII() = "0"
}
data class All(val id: String, override val sub: Formula) : UnaryFormula(sub, "\u2200$id", "!$id") {
    override fun toString(): String = super.toString()
}
data class Ex(val id: String, override val sub: Formula) : UnaryFormula(sub, "\u2203$id", "?$id") {
    override fun toString(): String = super.toString()
}
data class Atom(val term: Term) : Formula() {
    override val precedence = 100
    override fun toString() = term.toString()
    override fun toASCII() = term.toString()
}

data class Term(val name: String, val args: List<Term> = listOf()) {
    override fun toString() =
        if(args.isEmpty()) name else name + "(" + args.joinToString(", ") + ")"
}

fun <E> List<E>.updated(index: Int, elem: E) = mapIndexed { i, existing ->  if (i == index) elem else existing }

/**
 * Currently the parser framework fails when running the exported code
 * (although that used to work :( )
 */

object formulaGrammar {
    fun parseToEnd(arg: String): Formula {
        val (result, cont) = parseImpl(arg, 0)
        if(cont != arg.length) {
            throw RuntimeException("Unexpected trailing text: '${arg.substring(cont)}'")
        }
        return result
    }

    private fun skipSpaces(arg: String, cont: Int): Int {
        var result = cont
        while(result < arg.length && arg[result] == ' ') {
            result++
        }
        return result
    }

    private fun hasToken(arg: String, from: Int, token: String) =
        if(from + token.length <= arg.length) {
            arg.substring(from, from+token.length) == token
        } else {
            false
        }

    private fun parseRightAssoc(arg: String, from: Int, token: String,
                                constr: (Formula, Formula)->Formula,
                                inner: (String, Int)->Pair<Formula,Int>): Pair<Formula, Int> {
        var (form, cont) = inner(arg, from)
        var forms = listOf(form)
        while(true) {
            cont = skipSpaces(arg, cont)
            if(!hasToken(arg, cont, token))
                break
            cont = skipSpaces(arg, cont+token.length)
            val sub = inner(arg, cont)
            forms += sub.first
            cont = sub.second
        }
        return Pair(forms.reduceRight { f1, f2 -> constr(f1, f2) }, cont)
    }

    private fun parseImpl(arg: String, from: Int): Pair<Formula, Int> =
        parseRightAssoc(arg, from, "->", ::Implication, formulaGrammar::parseDisj)
    private fun parseDisj(arg: String, from: Int): Pair<Formula, Int> =
        parseRightAssoc(arg, from, "|", ::Disj, formulaGrammar::parseConj)

    private fun parseConj(arg: String, from: Int): Pair<Formula, Int> =
        parseRightAssoc(arg, from, "&", ::Conj, formulaGrammar::parseBase)

    private fun parseBase(arg: String, from: Int): Pair<Formula, Int> {
        val actFrom = skipSpaces(arg, from)
        when {
            hasToken(arg, actFrom, "-") -> {
                val (result, cont) = parseBase(arg, skipSpaces(arg, actFrom + 1))
                return Pair(Neg(result), cont)
            }

            hasToken(arg, actFrom, "(") -> {
                val (result, cont) = parseImpl(arg, skipSpaces(arg, actFrom + 1))
                val cont2 = skipSpaces(arg, cont)
                if (!hasToken(arg, cont2, ")")) {
                    throw RuntimeException("Missing closing parenthesis")
                }
                return Pair(result, cont2+1)
            }

            hasToken(arg, actFrom, "0") -> {
                return Pair(False, actFrom + 1)
            }

            actFrom == arg.length -> {
                throw RuntimeException("Unexpected end of string")
            }

            else -> {
                val m = Regex("^[A-Za-z]+").find(arg.substring(actFrom))
                if (m != null) {
                    return Pair(Atom(Term(m.value)), actFrom + m.value.length)
                } else {
                    throw RuntimeException("Unexpected string starting at '" + arg.substring(actFrom) + "'.")
                }
            }
        }
    }
}

//val formulaGrammar = object : Grammar<Formula>() {
//    val id by regexToken("[A-Za-z][A-Za-z0-9]*")
//    val _false by literalToken("0")
//    val imp by literalToken("->")
//    val not by literalToken("-")
//    val and by literalToken("&")
//    val or by literalToken("|")
//    val ws by regexToken("\\s+", ignore = true)
//    val lpar by literalToken("(")
//    val rpar by literalToken(")")
//    val comma by literalToken(",")
//    val all by literalToken("!")
//    val ex by literalToken("?")
//
//    val term: Parser<Term> by
//        ((id * (-lpar) * separatedTerms(parser(this::term), comma, false) * (-rpar))
//                map { (name, args) -> Term(name.text, args) }
//        or (id map { Term(it.text, listOf()) }))
//
/////// FOL
////    val base: Parser<Formula> by
////        (term map { Atom(it) }
////            or (((-not) * parser(this::base)) map { Neg(it) })
////            or (((-all) * id * parser(this::base)) map { (id,f) -> All(id.text,f) })
////            or (((-ex) * id * parser(this::base)) map { (id, f) -> Ex(id.text, f)})
////            or ((-lpar) * parser(this::formula) * (-rpar)))
//
////// Prop
//
//    val base: Parser<Formula> by
//        (id map { Atom(Term(it.text)) }
//         or (_false map { False })
//         or (((-not) * parser(this::base)) map { Neg(it) })
//         or ((-lpar) * parser(this::formula) * (-rpar)))
//
//    val conj: Parser<Formula> by
//        rightAssociative(base, and) { l, _, r -> Conj(l, r) }
//
//    val disj: Parser<Formula> by
//        rightAssociative(conj, or) { l, _, r -> Disj(l, r) }
//
//    val formula: Parser<Formula> by
//        rightAssociative(disj, imp) { l, _, r -> Implication(l, r) }
//
//    override val rootParser by formula
//}
//
