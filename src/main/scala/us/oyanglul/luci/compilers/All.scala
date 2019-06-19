package us.oyanglul.luci.compilers

trait All[E[_]]
    extends GenericCompiler[E]
    with WriterTCompiler[E]
    with Http4sClientCompiler[E]
    with ReaderTCompiler[E]
    with StateTCompiler[E]
    with IdCompiler[E]
    with DoobieCompiler[E]
    with EitherTCompiler[E]
    with RescueCompiler[E]
    with Fs2Compiler[E]
