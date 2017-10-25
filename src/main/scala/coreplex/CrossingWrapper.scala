// See LICENSE.SiFive for license details.

package freechips.rocketchip.coreplex

import Chisel._
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.util._

/** Enumerates the three types of clock crossing between tiles and system bus */
sealed trait CoreplexClockCrossing
case class SynchronousCrossing(params: BufferParams = BufferParams.default) extends CoreplexClockCrossing
case class RationalCrossing(direction: RationalDirection = FastToSlow) extends CoreplexClockCrossing
case class AsynchronousCrossing(depth: Int, sync: Int = 3) extends CoreplexClockCrossing

trait HasCrossingMethods extends LazyScope
{
  this: LazyModule =>

  // TileLink

  def crossTLSyncInOut(out: Boolean)(params: BufferParams = BufferParams.default)(implicit p: Parameters): TLNode = {
    this { LazyModule(new TLBuffer(params)).node }
  }

  def crossTLAsyncInOut(out: Boolean)(depth: Int = 8, sync: Int = 3)(implicit p: Parameters): TLNode = {
    def sourceGen = LazyModule(new TLAsyncCrossingSource(sync))
    def sinkGen = LazyModule(new TLAsyncCrossingSink(depth, sync))
    val source = if (out) this { sourceGen } else sourceGen
    val sink = if (out) sinkGen else this { sinkGen }
    sink.node :=? source.node
    NodeHandle(source.node, sink.node)
  }

  def crossTLRationalInOut(out: Boolean)(direction: RationalDirection)(implicit p: Parameters): TLNode = {
    def sourceGen = LazyModule(new TLRationalCrossingSource)
    def sinkGen = LazyModule(new TLRationalCrossingSink(if (out) direction else direction.flip))
    val source = if (out) this { sourceGen } else sourceGen
    val sink = if (out) sinkGen else this { sinkGen }
    sink.node :=? source.node
    NodeHandle(source.node, sink.node)
  }

  def crossTLSyncIn (params: BufferParams = BufferParams.default)(implicit p: Parameters): TLNode = crossTLSyncInOut(false)(params)
  def crossTLSyncOut(params: BufferParams = BufferParams.default)(implicit p: Parameters): TLNode = crossTLSyncInOut(true )(params)
  def crossTLAsyncIn (depth: Int = 8, sync: Int = 3)(implicit p: Parameters): TLNode = crossTLAsyncInOut(false)(depth, sync)
  def crossTLAsyncOut(depth: Int = 8, sync: Int = 3)(implicit p: Parameters): TLNode = crossTLAsyncInOut(true )(depth, sync)
  def crossTLRationalIn (direction: RationalDirection)(implicit p: Parameters): TLNode = crossTLRationalInOut(false)(direction)
  def crossTLRationalOut(direction: RationalDirection)(implicit p: Parameters): TLNode = crossTLRationalInOut(true )(direction)

  def crossTLIn(arg: CoreplexClockCrossing)(implicit p: Parameters): TLNode = arg match {
    case x: SynchronousCrossing  => crossTLSyncIn(x.params)
    case x: AsynchronousCrossing => crossTLAsyncIn(x.depth, x.sync)
    case x: RationalCrossing     => crossTLRationalIn(x.direction)
  }

  def crossTLOut(arg: CoreplexClockCrossing)(implicit p: Parameters): TLNode = arg match {
    case x: SynchronousCrossing  => crossTLSyncOut(x.params)
    case x: AsynchronousCrossing => crossTLAsyncOut(x.depth, x.sync)
    case x: RationalCrossing     => crossTLRationalOut(x.direction)
  }

  // Interrupts

  def crossIntSyncInOut(out: Boolean)(alreadyRegistered: Boolean = false)(implicit p: Parameters): IntNode = {
    def sourceGen = LazyModule(new IntSyncCrossingSource(alreadyRegistered))
    def sinkGen = LazyModule(new IntSyncCrossingSink(0))
    val source = if (out) this { sourceGen } else sourceGen
    val sink = if (out) sinkGen else this { sinkGen }
    sink.node :=? source.node
    NodeHandle(source.node, sink.node)
  }

  def crossIntAsyncInOut(out: Boolean)(sync: Int = 3, alreadyRegistered: Boolean = false)(implicit p: Parameters): IntNode = {
    def sourceGen = LazyModule(new IntSyncCrossingSource(alreadyRegistered))
    def sinkGen = LazyModule(new IntSyncCrossingSink(sync))
    val source = if (out) this { sourceGen } else sourceGen
    val sink = if (out) sinkGen else this { sinkGen }
    sink.node :=? source.node
    NodeHandle(source.node, sink.node)
  }

  def crossIntRationalInOut(out: Boolean)(alreadyRegistered: Boolean = false)(implicit p: Parameters): IntNode = {
    def sourceGen = LazyModule(new IntSyncCrossingSource(alreadyRegistered))
    def sinkGen = LazyModule(new IntSyncCrossingSink(1))
    val source = if (out) this { sourceGen } else sourceGen
    val sink = if (out) sinkGen else this { sinkGen }
    sink.node :=? source.node
    NodeHandle(source.node, sink.node)
  }

  def crossIntSyncIn (alreadyRegistered: Boolean = false)(implicit p: Parameters): IntNode = crossIntSyncInOut(false)(alreadyRegistered)
  def crossIntSyncOut(alreadyRegistered: Boolean = false)(implicit p: Parameters): IntNode = crossIntSyncInOut(true )(alreadyRegistered)
  def crossIntAsyncIn (sync: Int = 3, alreadyRegistered: Boolean = false)(implicit p: Parameters): IntNode = crossIntAsyncInOut(false)(sync, alreadyRegistered)
  def crossIntAsyncOut(sync: Int = 3, alreadyRegistered: Boolean = false)(implicit p: Parameters): IntNode = crossIntAsyncInOut(true )(sync, alreadyRegistered)
  def crossIntRationalIn (alreadyRegistered: Boolean = false)(implicit p: Parameters): IntNode = crossIntRationalInOut(false)(alreadyRegistered)
  def crossIntRationalOut(alreadyRegistered: Boolean = false)(implicit p: Parameters): IntNode = crossIntRationalInOut(true )(alreadyRegistered)

  def crossIntIn(arg: CoreplexClockCrossing, alreadyRegistered: Boolean)(implicit p: Parameters): IntNode = arg match {
    case x: SynchronousCrossing  => crossIntSyncIn(alreadyRegistered)
    case x: AsynchronousCrossing => crossIntAsyncIn(x.sync, alreadyRegistered)
    case x: RationalCrossing     => crossIntRationalIn(alreadyRegistered)
  }

  def crossIntOut(arg: CoreplexClockCrossing, alreadyRegistered: Boolean)(implicit p: Parameters): IntNode = arg match {
    case x: SynchronousCrossing  => crossIntSyncOut(alreadyRegistered)
    case x: AsynchronousCrossing => crossIntAsyncOut(x.sync, alreadyRegistered)
    case x: RationalCrossing     => crossIntRationalOut(alreadyRegistered)
  }

  def crossIntIn (arg: CoreplexClockCrossing)(implicit p: Parameters): IntNode = crossIntIn (arg, false)
  def crossIntOut(arg: CoreplexClockCrossing)(implicit p: Parameters): IntNode = crossIntOut(arg, false)
}

trait HasCrossing extends HasCrossingMethods
{
  this: LazyModule =>
  val crossing: CoreplexClockCrossing

  def crossTLIn  (implicit p: Parameters): TLNode  = crossTLIn  (crossing)
  def crossTLOut (implicit p: Parameters): TLNode  = crossTLOut (crossing)
  def crossIntIn (implicit p: Parameters): IntNode = crossIntIn (crossing)
  def crossIntOut(implicit p: Parameters): IntNode = crossIntOut(crossing)
  def crossIntIn (alreadyRegistered: Boolean)(implicit p: Parameters): IntNode = crossIntIn (crossing, alreadyRegistered)
  def crossIntOut(alreadyRegistered: Boolean)(implicit p: Parameters): IntNode = crossIntOut(crossing, alreadyRegistered)
}

class CrossingWrapper(val crossing: CoreplexClockCrossing)(implicit p: Parameters) extends SimpleLazyModule with HasCrossing
