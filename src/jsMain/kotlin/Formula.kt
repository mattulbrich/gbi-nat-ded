import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser

sealed class Formula {
    abstract val precedence: Int
}
abstract class BinaryFormula(open val sub1: Formula, open val sub2: Formula, val op: String, prec: Int): Formula() {
    override val precedence = prec
    override fun toString(): String {
        val t1 = if(sub1.precedence < precedence) "($sub1)" else sub1.toString()
        val t2 = if(sub2.precedence < precedence) "($sub2)" else sub2.toString()
        return "$t1 $op $t2"
    }
}
abstract class UnaryFormula(open val sub: Formula, val op: String): Formula() {
    override val precedence = 100
    override fun toString(): String {
        val t1 = if(sub.precedence < precedence) "($sub)" else sub.toString()
        return "$op $t1"
    }
}

// 22A2 is |-
// 22a5 is \bot
data class Implication(override val sub1: Formula, override val sub2: Formula) : BinaryFormula(sub1, sub2, "\u2192", 10) {
    override fun toString(): String = super.toString()
}
data class Disj(override val sub1: Formula, override val sub2: Formula) : BinaryFormula(sub1, sub2, "\u2228", 20){
    override fun toString(): String = super.toString()
}
data class Conj(override val sub1: Formula, override val sub2: Formula) : BinaryFormula(sub1, sub2, "\u2227", 30) {
    override fun toString(): String = super.toString()
}
data class Neg(override val sub: Formula) : UnaryFormula(sub, "¬") {
    override fun toString(): String = super.toString()
}
data class All(val id: String, override val sub: Formula) : UnaryFormula(sub, "\u2200$id") {
    override fun toString(): String = super.toString()
}
data class Ex(val id: String, override val sub: Formula) : UnaryFormula(sub, "\u2203$id") {
    override fun toString(): String = super.toString()
}
data class Atom(val term: Term) : Formula() {
    override val precedence = 100
    override fun toString() = term.toString()
}

data class Term(val name: String, val args: List<Term> = listOf()) {
    override fun toString() =
        if(args.isEmpty()) name else name + "(" + args.joinToString(", ") + ")"
}

fun <E> List<E>.updated(index: Int, elem: E) = mapIndexed { i, existing ->  if (i == index) elem else existing }



val formulaGrammar = object : Grammar<Formula>() {
    val id by regexToken("[A-Za-z][A-Za-z0-9]*")
    val not by literalToken("-")
    val and by literalToken("&")
    val imp by literalToken("=>")
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

    val base: Parser<Formula> by
        (term map { Atom(it) }
            or (((-not) * parser(this::base)) map { Neg(it) })
            or (((-all) * id * parser(this::base)) map { (id,f) -> All(id.text,f) })
            or (((-ex) * id * parser(this::base)) map { (id, f) -> Ex(id.text, f)})
            or ((-lpar) * parser(this::formula) * (-rpar)))

    val conj: Parser<Formula> by
        leftAssociative(base, and) { l, _, r -> Conj(l, r)}

    val disj: Parser<Formula> by
        leftAssociative(conj, or) { l, _, r -> Disj(l, r)}

    val formula: Parser<Formula> by
        leftAssociative(disj, imp) { l, _, r -> Implication(l, r)}

    override val rootParser by formula
}

fun parmain() {
    val ast = formulaGrammar.parseToEnd("!x (P(x) | Q(c))") //  | b & -(a | c)
    println(ast)
}



