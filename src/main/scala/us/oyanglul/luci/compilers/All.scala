package us.oyanglul.luci
package compilers

trait All[E[_]]
    extends GenericCompiler[E]
    with WriterCompiler[E]
    with Http4sClientCompiler[E]
    with ReaderCompiler[E]
    with StateCompiler[E]
    with IdCompiler[E]
    with DoobieCompiler[E]
    with EitherCompiler[E]
    with RescueCompiler[E]
    with Fs2Compiler[E]
