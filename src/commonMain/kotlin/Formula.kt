import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser


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

    override fun toASCII() = "${sub.toASCII()} $ascii"
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



val formulaGrammar = object : Grammar<Formula>() {
    val id by regexToken("[A-Za-z][A-Za-z0-9]*")
    val _false by literalToken("0")
    val imp by literalToken("->")
    val not by literalToken("-")
    val and by literalToken("&")
    val or by literalToken("|")
    val ws by regexToken("\\s+", ignore = true)
    val lpar by literalToken("(")
    val rpar by literalToken(")")
    val comma by literalToken(",")
    val all by literalToken("!")
    val ex by literalToken("?")

    val term: Parser<Term> by
        ((id * (-lpar) * separatedTerms(parser(this::term), comma, false) * (-rpar))
                map { (name, args) -> Term(name.text, args) }
        or (id map { Term(it.text, listOf()) }))

///// FOL
//    val base: Parser<Formula> by
//        (term map { Atom(it) }
//            or (((-not) * parser(this::base)) map { Neg(it) })
//            or (((-all) * id * parser(this::base)) map { (id,f) -> All(id.text,f) })
//            or (((-ex) * id * parser(this::base)) map { (id, f) -> Ex(id.text, f)})
//            or ((-lpar) * parser(this::formula) * (-rpar)))

//// Prop

    val base: Parser<Formula> by
        (id map { Atom(Term(it.text)) }
         or (_false map { False })
         or (((-not) * parser(this::base)) map { Neg(it) })
         or ((-lpar) * parser(this::formula) * (-rpar)))

    val conj: Parser<Formula> by
        rightAssociative(base, and) { l, _, r -> Conj(l, r) }

    val disj: Parser<Formula> by
        rightAssociative(conj, or) { l, _, r -> Disj(l, r) }

    val formula: Parser<Formula> by
        rightAssociative(disj, imp) { l, _, r -> Implication(l, r) }

    override val rootParser by formula
}

