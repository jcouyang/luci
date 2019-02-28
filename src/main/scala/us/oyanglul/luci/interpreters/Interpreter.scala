package us.oyanglul.luci
package interpreters

import cats.data.Kleisli
import cats.~>

trait Interpreter[E[_], Eff[_], C] {
  def translate: Eff ~> Kleisli[E, C, ?]
}
