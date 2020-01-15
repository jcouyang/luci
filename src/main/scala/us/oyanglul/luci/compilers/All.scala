package us.oyanglul.luci
package compilers

trait All[E[_]]
    extends WriterTCompiler[E]
    with Http4sClientCompiler[E]
    with ReaderTCompiler[E]
    with StateTCompiler[E]
    with IdCompiler[E]
    with DoobieCompiler[E]
    with EitherTCompiler[E]
    with RescueCompiler[E]
    with Fs2Compiler[E]

trait AllFreeT[E[_]] extends freetcompilers.GenericCompiler[E] with All[E]

trait AllFree[E[_]] extends GenericCompiler[E] with All[E]
