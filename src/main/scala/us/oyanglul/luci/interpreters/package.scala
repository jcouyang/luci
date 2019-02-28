package us.oyanglul.luci

package object interpreters {
  object all extends ReaderWriterInterp with HttpClientInterp with ProgramInterp
}
