package ppl.delite.framework

object Config {
  val buildDir = System.getProperty("delite-build-dir", "generated/")
  val degFilename = System.getProperty("delite-deg-filename", "")
  val deliteHome = System.getProperty("delite-home-dir",".")
  val blasDir = System.getProperty("delite-BLAS-dir")
  val useBlas = if (blasDir == null) false else true
}
