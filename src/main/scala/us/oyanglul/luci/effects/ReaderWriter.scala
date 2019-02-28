package us.oyanglul.luci
package effects

sealed trait Writer[A]
case class Info(log: String) extends Writer[Unit]
case class Debug(log: String) extends Writer[Unit]
case class Error(log: String) extends Writer[Unit]
