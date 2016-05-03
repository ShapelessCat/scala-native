package scala.scalanative
package nir

import scala.collection.mutable
import nir.Attr._

sealed abstract class Attr
object Attr {
  sealed abstract class Inline extends Attr
  final case object MayInline  extends Inline // no information
  final case object InlineHint extends Inline // user hinted at inlining
  final case object NoInline   extends Inline // should never inline
  final case object MustInline extends Inline // should always inline

  final case object Extern extends Attr
  final case class Override(name: Global) extends Attr

  sealed abstract class Pin extends Attr
  final case class PinAlways(dep: Global)           extends Pin
  final case class PinIf(dep: Global, cond: Global) extends Pin
}

final case class Attrs(inline: Inline         = MayInline,
                       isExtern: Boolean      = false,
                       overrides: Seq[Global] = Seq(),
                       pins: Seq[Pin]         = Seq()) {
  def toSeq: Seq[Attr] = {
    val out = mutable.UnrolledBuffer.empty[Attr]

    if (inline != MayInline) out += inline
    if (isExtern) out += Extern
    overrides.foreach { out += Override(_) }
    out ++= pins

    out
  }
}
object Attrs {
  val None = new Attrs()

  def fromSeq(attrs: Seq[Attr]) = {
    var inline: Inline         = None.inline
    var isExtern               = false
    val overrides              = mutable.UnrolledBuffer.empty[Global]
    val pins                   = mutable.UnrolledBuffer.empty[Pin]

    attrs.foreach {
      case attr: Inline   => inline     = attr
      case Extern         => isExtern   = true
      case Override(name) => overrides += name
      case attr: Pin      => pins      += attr
    }

    new Attrs(inline, isExtern, overrides, pins)
  }
}


