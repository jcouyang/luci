package us.oyanglul.luci.interpreters

trait All
    extends WriterTInterp
    with Http4sClientInterp
    with ReaderTInterp
    with StateTInterp
    with IoInterp
    with DoobieInterp
    with EitherInterp
