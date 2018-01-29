package sbtjavap

private[sbtjavap] object Serialization {
  object Implicits extends sbinary.DefaultProtocol {
    implicit def seqFormat[A: sbinary.Format] = sbt.Cache.seqFormat[A]
  }
}
