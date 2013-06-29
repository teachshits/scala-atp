package org.edla.study.parsing.parboiled.semantic

import org.parboiled.errors.ParseError
import org.parboiled.scala.ANY
import org.parboiled.scala.Parser
import org.parboiled.scala.creator4Rule0
import org.parboiled.scala.creator4Rule1

import AST.AccountSpec
import AST.BUY
import AST.BuySell
import AST.Items
import AST.LineItem
import AST.MAX
import AST.MIN
import AST.Order
import AST.PriceSpec
import AST.PriceType
import AST.SELL
import AST.SecuritySpec

class OrderDsl extends Parser {

  def order = rule { items ~ account_spec ~~> Order }

  def items = rule { "( " ~ oneOrMore(line_item, separator = ", ") ~ ") " ~~> Items }

  def line_item = rule {
    security_spec ~ buy_sell ~ price_spec ~~> ((s: SecuritySpec, b: BuySell, p: PriceSpec) => LineItem(s, b, p))
  }

  def buy_sell = rule { "to " ~ (("buy " ~ push(BUY)) | ("sell " ~ push(SELL))) }

  def security_spec = rule {
    numericLit ~ WhiteSpace ~ ident ~~> ((n: Int, s) => SecuritySpec(n.toInt, s.toString)) ~ WhiteSpace ~ "shares "
  }

  def price_spec = rule {
    "at " ~ optional(min_max) ~ numericLit ~~> ((m: Option[PriceType], p) => PriceSpec(m, p.toInt)) ~ WhiteSpace
  }

  def min_max = rule { "min " ~ push(MIN) | "max " ~ push(MAX) }

  def account_spec = rule { "for " ~ "account " ~ stringLit ~~> AccountSpec }

  def stringLit = rule { "\"" ~ zeroOrMore(NormalChar) ~> (_.toString) ~ "\" " }

  def ident = rule { oneOrMore("a" - "z" | "A" - "Z") ~> (_.toString) }

  def numericLit = rule { oneOrMore("0" - "9") ~> (_.toInt) }

  def NormalChar = rule { !anyOf("\"\\") ~ ANY }

  def WhiteSpace = rule { zeroOrMore(anyOf(" \n\r\t\f")) }

  /**
   * We redefine the default string-to-rule conversion to also match trailing whitespace if the string ends with
   * a blank, this keeps the rules free from most whitespace matching clutter
   */
  override implicit def toRule(string: String) =
    if (string.endsWith(" "))
      str(string.trim) ~ WhiteSpace
    else
      str(string)

  def parseErrorMsg(e: ParseError): String = {
    val pos = e.getInputBuffer.getPosition(e.getStartIndex)
    "Parse error at: " + pos + " " + e.getErrorMessage
  }

}