package us.oyanglul.luci.compilers

trait All[E[_]]
    extends WriterTCompiler[E]
    with Http4sClientCompiler[E]
    with ReaderTCompiler[E]
    with StateTCompiler[E]
    with IdCompiler[E]
    with DoobieCompiler[E]
//    with EitherCompiler[E]
