package us.oyanglul.luci
package effects

sealed trait WriterT[A]
case class Info(log: String) extends WriterT[Unit]
case class Debug(log: String) extends WriterT[Unit]
case class Error(log: String) extends WriterT[Unit]
