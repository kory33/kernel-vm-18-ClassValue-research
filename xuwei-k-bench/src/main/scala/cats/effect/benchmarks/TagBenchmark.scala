// Adapted from: https://github.com/xuwei-k/cats-effect/commit/f7c2dabfd974bcfb57cff683a360ef7c163a4487

/*
 * Copyright 2020-2024 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats.effect.benchmarks

import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit
import scala.annotation.switch

// Simplified stub classes for benchmarking without Cats Effect runtime initialization
sealed abstract class FakeIO[+A] {
  def tag: Byte
}

object FakeIO {
  final case class Pure[A](value: A) extends FakeIO[A] { def tag: Byte = 0 }
  final case class Error(throwable: Throwable) extends FakeIO[Nothing] {
    def tag: Byte = 1
  }
  final case class Delay[A](thunk: () => A) extends FakeIO[A] {
    def tag: Byte = 2
  }
  case object RealTime extends FakeIO[Long] { def tag: Byte = 3 }
  case object Monotonic extends FakeIO[Long] { def tag: Byte = 4 }
  case object ReadEC extends FakeIO[Any] { def tag: Byte = 5 }
  final case class Map[A, B](fa: FakeIO[A], f: A => B) extends FakeIO[B] {
    def tag: Byte = 6
  }
  final case class FlatMap[A, B](fa: FakeIO[A], f: A => FakeIO[B])
      extends FakeIO[B] { def tag: Byte = 7 }
  final case class Attempt[A](fa: FakeIO[A])
      extends FakeIO[Either[Throwable, A]] { def tag: Byte = 8 }
  final case class HandleErrorWith[A](fa: FakeIO[A], f: Throwable => FakeIO[A])
      extends FakeIO[A] { def tag: Byte = 9 }
  case object Canceled extends FakeIO[Unit] { def tag: Byte = 10 }
  final case class OnCancel[A](fa: FakeIO[A], fin: FakeIO[Unit])
      extends FakeIO[A] { def tag: Byte = 11 }
  final case class Uncancelable[A](body: Any => FakeIO[A]) extends FakeIO[A] {
    def tag: Byte = 12
  }
  final case class UnmaskRunLoop[A](fa: FakeIO[A], id: Int, self: Any)
      extends FakeIO[A] { def tag: Byte = 13 }
  final case class IOCont[A, B](body: Any, cont: Any) extends FakeIO[B] {
    def tag: Byte = 14
  }
  final case class Get[A](state: Any) extends FakeIO[A] { def tag: Byte = 15 }
  case object Cede extends FakeIO[Unit] { def tag: Byte = 16 }
  final case class Start[A](fa: FakeIO[A]) extends FakeIO[Any] {
    def tag: Byte = 17
  }
  final case class RacePair[A, B](fa: FakeIO[A], fb: FakeIO[B])
      extends FakeIO[Either[Any, Any]] { def tag: Byte = 18 }
  final case class Sleep(duration: Long) extends FakeIO[Unit] {
    def tag: Byte = 19
  }
  final case class EvalOn[A](fa: FakeIO[A], ec: Any) extends FakeIO[A] {
    def tag: Byte = 20
  }
  final case class Blocking[A](hint: Any, thunk: Any, blockingOn: Any)
      extends FakeIO[A] { def tag: Byte = 21 }
  final case class Local[A](f: Any) extends FakeIO[A] { def tag: Byte = 22 }
  case object IOTrace extends FakeIO[Any] { def tag: Byte = 23 }
  case object ReadRT extends FakeIO[Any] { def tag: Byte = 24 }
  case object EndFiber extends FakeIO[Unit] { def tag: Byte = -1 }
}

sealed abstract class FakeIOWithObjectLevelTag[+A](
    private[effect] val tagValue: Byte
)
object FakeIOWithObjectLevelTag {
  final case class Pure[A](value: A) extends FakeIOWithObjectLevelTag[A](0)
  final case class Error(throwable: Throwable)
      extends FakeIOWithObjectLevelTag[Nothing](1)
  final case class Delay[A](thunk: () => A)
      extends FakeIOWithObjectLevelTag[A](2)
  case object RealTime extends FakeIOWithObjectLevelTag[Long](3)
  case object Monotonic extends FakeIOWithObjectLevelTag[Long](4)
  case object ReadEC extends FakeIOWithObjectLevelTag[Any](5)
  final case class Map[A, B](fa: FakeIOWithObjectLevelTag[A], f: A => B)
      extends FakeIOWithObjectLevelTag[B](6)
  final case class FlatMap[A, B](
      fa: FakeIOWithObjectLevelTag[A],
      f: A => FakeIOWithObjectLevelTag[B]
  ) extends FakeIOWithObjectLevelTag[B](7)
  final case class Attempt[A](fa: FakeIOWithObjectLevelTag[A])
      extends FakeIOWithObjectLevelTag[Either[Throwable, A]](8)
  final case class HandleErrorWith[A](
      fa: FakeIOWithObjectLevelTag[A],
      f: Throwable => FakeIOWithObjectLevelTag[A]
  ) extends FakeIOWithObjectLevelTag[A](9)
  case object Canceled extends FakeIOWithObjectLevelTag[Unit](10)
  final case class OnCancel[A](
      fa: FakeIOWithObjectLevelTag[A],
      fin: FakeIOWithObjectLevelTag[Unit]
  ) extends FakeIOWithObjectLevelTag[A](11)
  final case class Uncancelable[A](body: Any => FakeIOWithObjectLevelTag[A])
      extends FakeIOWithObjectLevelTag[A](12)
  final case class UnmaskRunLoop[A](
      fa: FakeIOWithObjectLevelTag[A],
      id: Int,
      self: Any
  ) extends FakeIOWithObjectLevelTag[A](13)
  final case class IOCont[A, B](body: Any, cont: Any)
      extends FakeIOWithObjectLevelTag[B](14)
  final case class Get[A](state: Any) extends FakeIOWithObjectLevelTag[A](15)
  case object Cede extends FakeIOWithObjectLevelTag[Unit](16)
  final case class Start[A](fa: FakeIOWithObjectLevelTag[A])
      extends FakeIOWithObjectLevelTag[Any](17)
  final case class RacePair[A, B](
      fa: FakeIOWithObjectLevelTag[A],
      fb: FakeIOWithObjectLevelTag[B]
  ) extends FakeIOWithObjectLevelTag[Either[Any, Any]](18)
  final case class Sleep(duration: Long)
      extends FakeIOWithObjectLevelTag[Unit](19)
  final case class EvalOn[A](fa: FakeIOWithObjectLevelTag[A], ec: Any)
      extends FakeIOWithObjectLevelTag[A](20)
  final case class Blocking[A](hint: Any, thunk: Any, blockingOn: Any)
      extends FakeIOWithObjectLevelTag[A](21)
  final case class Local[A](f: Any) extends FakeIOWithObjectLevelTag[A](22)
  case object IOTrace extends FakeIOWithObjectLevelTag[Any](23)
  case object ReadRT extends FakeIOWithObjectLevelTag[Any](24)
  case object EndFiber extends FakeIOWithObjectLevelTag[Unit](-1)
}

// from https://github.com/typelevel/cats-effect/commit/1472f3d252eb0d79c161c9f5d01dec249ce7252b
private[effect] final class ClassByteMap {
  import ClassByteMap.{Size, Mask}
  import java.util.Arrays

  private val keys = new Array[Class[?]](Size)
  private val values = {
    val a = new Array[Byte](Size)
    Arrays.fill(a, -1: Byte)
    a
  }

  def apply(key: Class[?]): Byte = {
    var i = key.hashCode() & Mask
    while (keys(i) != key) i = (i + 1) & Mask
    values(i)
  }

  def update(key: Class[?], value: Byte): Unit = {
    var i = key.hashCode() & Mask
    while (values(i) != -1) i = (i + 1) & Mask
    keys(i) = key
    values(i) = value
  }
}

private[effect] object ClassByteMap {
  private final val Size = 64
  private final val Mask = Size - 1
}

object TagBenchmark {
  val ioClassValue = new ClassValue[Byte] {
    import FakeIO._
    private val PureClass = classOf[Pure[?]]
    private val ErrorClass = classOf[Error]
    private val DelayClass = classOf[Delay[?]]
    private val ReadTimeClass = classOf[RealTime.type]
    private val MonotonicClass = classOf[Monotonic.type]
    private val ReadEcClass = classOf[ReadEC.type]
    private val MapClass = classOf[Map[?, ?]]
    private val FlatMapClass = classOf[FlatMap[?, ?]]
    private val AttemptClass = classOf[Attempt[?]]
    private val HandleErrorWithClass = classOf[HandleErrorWith[?]]
    private val CanceledClass = classOf[Canceled.type]
    private val OnCancelClass = classOf[OnCancel[?]]
    private val UncancelableClass = classOf[Uncancelable[?]]
    private val UnmaskRunLoopClass = classOf[UnmaskRunLoop[?]]
    private val IOContClass = classOf[IOCont[?, ?]]
    private val GetClass = classOf[Get[?]]
    private val CedeClass = classOf[Cede.type]
    private val StartClass = classOf[Start[?]]
    private val RacePairClass = classOf[RacePair[?, ?]]
    private val SleepClass = classOf[Sleep]
    private val EvalOnClass = classOf[EvalOn[?]]
    private val BlockingClass = classOf[Blocking[?]]
    private val LocalClass = classOf[Local[?]]
    private val IOTraceClass = classOf[IOTrace.type]
    private val ReadRTClass = classOf[ReadRT.type]
    private val EndFiberClass = classOf[EndFiber.type]

    override def computeValue(clazz: Class[?]): Byte =
      (clazz: @unchecked) match {
        case PureClass            => 0
        case ErrorClass           => 1
        case DelayClass           => 2
        case ReadTimeClass        => 3
        case MonotonicClass       => 4
        case ReadEcClass          => 5
        case MapClass             => 6
        case FlatMapClass         => 7
        case AttemptClass         => 8
        case HandleErrorWithClass => 9
        case CanceledClass        => 10
        case OnCancelClass        => 11
        case UncancelableClass    => 12
        case UnmaskRunLoopClass   => 13
        case IOContClass          => 14
        case GetClass             => 15
        case CedeClass            => 16
        case StartClass           => 17
        case RacePairClass        => 18
        case SleepClass           => 19
        case EvalOnClass          => 20
        case BlockingClass        => 21
        case LocalClass           => 22
        case IOTraceClass         => 23
        case ReadRTClass          => 24
        case EndFiberClass        => -1
      }
  }

  val ioClassByteMap: ClassByteMap = {
    val map = new ClassByteMap
    map.update(classOf[FakeIO.Pure[?]], 0)
    map.update(classOf[FakeIO.Error], 1)
    map.update(classOf[FakeIO.Delay[?]], 2)
    map.update(classOf[FakeIO.RealTime.type], 3)
    map.update(classOf[FakeIO.Monotonic.type], 4)
    map.update(classOf[FakeIO.ReadEC.type], 5)
    map.update(classOf[FakeIO.Map[?, ?]], 6)
    map.update(classOf[FakeIO.FlatMap[?, ?]], 7)
    map.update(classOf[FakeIO.Attempt[?]], 8)
    map.update(classOf[FakeIO.HandleErrorWith[?]], 9)
    map.update(classOf[FakeIO.Canceled.type], 10)
    map.update(classOf[FakeIO.OnCancel[?]], 11)
    map.update(classOf[FakeIO.Uncancelable[?]], 12)
    map.update(classOf[FakeIO.UnmaskRunLoop[?]], 13)
    map.update(classOf[FakeIO.IOCont[?, ?]], 14)
    map.update(classOf[FakeIO.Get[?]], 15)
    map.update(classOf[FakeIO.Cede.type], 16)
    map.update(classOf[FakeIO.Start[?]], 17)
    map.update(classOf[FakeIO.RacePair[?, ?]], 18)
    map.update(classOf[FakeIO.Sleep], 19)
    map.update(classOf[FakeIO.EvalOn[?]], 20)
    map.update(classOf[FakeIO.Blocking[?]], 21)
    map.update(classOf[FakeIO.Local[?]], 22)
    map.update(classOf[FakeIO.IOTrace.type], 23)
    map.update(classOf[FakeIO.ReadRT.type], 24)
    map.update(classOf[FakeIO.EndFiber.type], -1)
    map
  }

  val values: Array[FakeIO[?]] = Array(
    // we want to make sure values.length != 26, so that we can easily distinguish 26 (0x1a) (the variant count) from 41 (0x29) (the length of the array)
    // in the assembly output
    FakeIO.Pure(1),
    FakeIO.Error(new Throwable()),
    FakeIO.Delay(() => 2),
    FakeIO.RealTime,
    FakeIO.FlatMap(FakeIO.Pure(4), (_: Int) => FakeIO.Pure(1)),
    FakeIO.Attempt(FakeIO.Pure(5)),
    FakeIO.HandleErrorWith(FakeIO.Pure(6), (_: Throwable) => FakeIO.Pure(1)),
    FakeIO.Canceled,
    FakeIO.OnCancel(FakeIO.Pure(7), FakeIO.Pure(())),
    FakeIO.Uncancelable((_: Any) => FakeIO.Pure(8)),
    FakeIO.UnmaskRunLoop(FakeIO.Pure(9), -1, null),
    FakeIO.IOCont(null, null),
    FakeIO.Get(null),
    FakeIO.Cede,
    FakeIO.Monotonic,
    FakeIO.ReadEC,
    FakeIO.Map(FakeIO.Pure(3), (x: Int) => x + 1),
    FakeIO.FlatMap(FakeIO.Pure(4), (_: Int) => FakeIO.Pure(1)),
    FakeIO.Attempt(FakeIO.Pure(5)),
    FakeIO.HandleErrorWith(FakeIO.Pure(6), (_: Throwable) => FakeIO.Pure(1)),
    FakeIO.Canceled,
    FakeIO.ReadEC,
    FakeIO.Map(FakeIO.Pure(3), (x: Int) => x + 1),
    FakeIO.FlatMap(FakeIO.Pure(4), (_: Int) => FakeIO.Pure(1)),
    FakeIO.Attempt(FakeIO.Pure(5)),
    FakeIO.HandleErrorWith(FakeIO.Pure(6), (_: Throwable) => FakeIO.Pure(1)),
    FakeIO.OnCancel(FakeIO.Pure(7), FakeIO.Pure(())),
    FakeIO.Uncancelable((_: Any) => FakeIO.Pure(8)),
    FakeIO.UnmaskRunLoop(FakeIO.Pure(9), -1, null),
    FakeIO.IOCont(null, null),
    FakeIO.Get(null),
    FakeIO.Cede,
    FakeIO.Start(FakeIO.Pure(10)),
    FakeIO.RacePair(FakeIO.Pure(11), FakeIO.Pure(12)),
    FakeIO.Sleep(3000L),
    FakeIO.EvalOn(FakeIO.Pure(13), null),
    FakeIO.Blocking(null, null, null),
    FakeIO.Local(null),
    FakeIO.IOTrace,
    FakeIO.ReadRT,
    FakeIO.EndFiber
  )

  val valuesWithTags: Array[FakeIOWithObjectLevelTag[?]] = Array(
    // we want to make sure values.length != 26, so that we can easily distinguish 26 (0x1a) (the variant count) from 41 (0x29) (the length of the array)
    // in the assembly output
    FakeIOWithObjectLevelTag.Pure(1),
    FakeIOWithObjectLevelTag.Error(new Throwable()),
    FakeIOWithObjectLevelTag.Delay(() => 2),
    FakeIOWithObjectLevelTag.RealTime,
    FakeIOWithObjectLevelTag.FlatMap(
      FakeIOWithObjectLevelTag.Pure(4),
      (_: Int) => FakeIOWithObjectLevelTag.Pure(1)
    ),
    FakeIOWithObjectLevelTag.Attempt(FakeIOWithObjectLevelTag.Pure(5)),
    FakeIOWithObjectLevelTag.HandleErrorWith(
      FakeIOWithObjectLevelTag.Pure(6),
      (_: Throwable) => FakeIOWithObjectLevelTag.Pure(1)
    ),
    FakeIOWithObjectLevelTag.Canceled,
    FakeIOWithObjectLevelTag.OnCancel(
      FakeIOWithObjectLevelTag.Pure(7),
      FakeIOWithObjectLevelTag.Pure(())
    ),
    FakeIOWithObjectLevelTag.Uncancelable((_: Any) =>
      FakeIOWithObjectLevelTag.Pure(8)
    ),
    FakeIOWithObjectLevelTag
      .UnmaskRunLoop(FakeIOWithObjectLevelTag.Pure(9), -1, null),
    FakeIOWithObjectLevelTag.IOCont(null, null),
    FakeIOWithObjectLevelTag.Get(null),
    FakeIOWithObjectLevelTag.Cede,
    FakeIOWithObjectLevelTag.Monotonic,
    FakeIOWithObjectLevelTag.ReadEC,
    FakeIOWithObjectLevelTag
      .Map(FakeIOWithObjectLevelTag.Pure(3), (x: Int) => x + 1),
    FakeIOWithObjectLevelTag.FlatMap(
      FakeIOWithObjectLevelTag.Pure(4),
      (_: Int) => FakeIOWithObjectLevelTag.Pure(1)
    ),
    FakeIOWithObjectLevelTag.Attempt(FakeIOWithObjectLevelTag.Pure(5)),
    FakeIOWithObjectLevelTag.HandleErrorWith(
      FakeIOWithObjectLevelTag.Pure(6),
      (_: Throwable) => FakeIOWithObjectLevelTag.Pure(1)
    ),
    FakeIOWithObjectLevelTag.Canceled,
    FakeIOWithObjectLevelTag.ReadEC,
    FakeIOWithObjectLevelTag
      .Map(FakeIOWithObjectLevelTag.Pure(3), (x: Int) => x + 1),
    FakeIOWithObjectLevelTag.FlatMap(
      FakeIOWithObjectLevelTag.Pure(4),
      (_: Int) => FakeIOWithObjectLevelTag.Pure(1)
    ),
    FakeIOWithObjectLevelTag.Attempt(FakeIOWithObjectLevelTag.Pure(5)),
    FakeIOWithObjectLevelTag.HandleErrorWith(
      FakeIOWithObjectLevelTag.Pure(6),
      (_: Throwable) => FakeIOWithObjectLevelTag.Pure(1)
    ),
    FakeIOWithObjectLevelTag.OnCancel(
      FakeIOWithObjectLevelTag.Pure(7),
      FakeIOWithObjectLevelTag.Pure(())
    ),
    FakeIOWithObjectLevelTag.Uncancelable((_: Any) =>
      FakeIOWithObjectLevelTag.Pure(8)
    ),
    FakeIOWithObjectLevelTag
      .UnmaskRunLoop(FakeIOWithObjectLevelTag.Pure(9), -1, null),
    FakeIOWithObjectLevelTag.IOCont(null, null),
    FakeIOWithObjectLevelTag.Get(null),
    FakeIOWithObjectLevelTag.Cede,
    FakeIOWithObjectLevelTag.Start(FakeIOWithObjectLevelTag.Pure(10)),
    FakeIOWithObjectLevelTag.RacePair(
      FakeIOWithObjectLevelTag.Pure(11),
      FakeIOWithObjectLevelTag.Pure(12)
    ),
    FakeIOWithObjectLevelTag.Sleep(3000L),
    FakeIOWithObjectLevelTag.EvalOn(FakeIOWithObjectLevelTag.Pure(13), null),
    FakeIOWithObjectLevelTag.Blocking(null, null, null),
    FakeIOWithObjectLevelTag.Local(null),
    FakeIOWithObjectLevelTag.IOTrace,
    FakeIOWithObjectLevelTag.ReadRT,
    FakeIOWithObjectLevelTag.EndFiber
  )
}

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class TagBenchmark {

  @Benchmark
  def tag(bk: Blackhole): Unit = {
    var i = 0
    while (i < TagBenchmark.values.length) {
      val result: String = (TagBenchmark.values(i).tag: @switch) match {
        case 0  => "Pure"
        case 1  => "Error"
        case 2  => "Delay"
        case 3  => "ReadTime"
        case 4  => "Monotonic"
        case 5  => "ReadEC"
        case 6  => "Map"
        case 7  => "FlatMap"
        case 8  => "Attempt"
        case 9  => "HandleErrorWith"
        case 10 => "Canceled"
        case 11 => "OnCancel"
        case 12 => "Uncancelable"
        case 13 => "UnmaskRunLoop"
        case 14 => "IOCont"
        case 15 => "Get"
        case 16 => "Cede"
        case 17 => "Start"
        case 18 => "RacePair"
        case 19 => "Sleep"
        case 20 => "EvalOn"
        case 21 => "Blocking"
        case 22 => "Local"
        case 23 => "IOTrace"
        case 24 => "ReadRT"
        case -1 => "EndFiber"
      }
      bk.consume(result)
      i += 1
    }
  }

  @Benchmark
  def classValue(bk: Blackhole): Unit = {
    var i = 0
    while (i < TagBenchmark.values.length) {
      val result: String =
        (TagBenchmark.ioClassValue.get(
          TagBenchmark.values(i).getClass
        ): @switch) match {
          case 0  => "Pure"
          case 1  => "Error"
          case 2  => "Delay"
          case 3  => "ReadTime"
          case 4  => "Monotonic"
          case 5  => "ReadEC"
          case 6  => "Map"
          case 7  => "FlatMap"
          case 8  => "Attempt"
          case 9  => "HandleErrorWith"
          case 10 => "Canceled"
          case 11 => "OnCancel"
          case 12 => "Uncancelable"
          case 13 => "UnmaskRunLoop"
          case 14 => "IOCont"
          case 15 => "Get"
          case 16 => "Cede"
          case 17 => "Start"
          case 18 => "RacePair"
          case 19 => "Sleep"
          case 20 => "EvalOn"
          case 21 => "Blocking"
          case 22 => "Local"
          case 23 => "IOTrace"
          case 24 => "ReadRT"
          case -1 => "EndFiber"
        }
      bk.consume(result)
      i += 1
    }
  }

  @Benchmark
  def patmat(bk: Blackhole): Unit = {
    var i = 0
    while (i < TagBenchmark.values.length) {
      val result: String =
        TagBenchmark.values(i) match {
          case FakeIO.Pure(_)                => "Pure"
          case FakeIO.Error(_)               => "Error"
          case FakeIO.Delay(_)               => "Delay"
          case FakeIO.RealTime               => "ReadTime"
          case FakeIO.Monotonic              => "Monotonic"
          case FakeIO.ReadEC                 => "ReadEC"
          case FakeIO.Map(_, _)              => "Map"
          case FakeIO.FlatMap(_, _)          => "FlatMap"
          case FakeIO.Attempt(_)             => "Attempt"
          case FakeIO.HandleErrorWith(_, _)  => "HandleErrorWith"
          case FakeIO.Canceled               => "Canceled"
          case FakeIO.OnCancel(_, _)         => "OnCancel"
          case FakeIO.Uncancelable(_)        => "Uncancelable"
          case FakeIO.UnmaskRunLoop(_, _, _) => "UnmaskRunLoop"
          case FakeIO.IOCont(_, _)           => "IOCont"
          case FakeIO.Get(_)                 => "Get"
          case FakeIO.Cede                   => "Cede"
          case FakeIO.Start(_)               => "Start"
          case FakeIO.RacePair(_, _)         => "RacePair"
          case FakeIO.Sleep(_)               => "Sleep"
          case FakeIO.EvalOn(_, _)           => "EvalOn"
          case FakeIO.Blocking(_, _, _)      => "Blocking"
          case FakeIO.Local(_)               => "Local"
          case FakeIO.IOTrace                => "IOTrace"
          case FakeIO.ReadRT                 => "ReadRT"
          case FakeIO.EndFiber               => "EndFiber"
        }
      bk.consume(result)
      i += 1
    }
  }

  @Benchmark
  def classByteMap(bk: Blackhole): Unit = {
    var i = 0
    while (i < TagBenchmark.values.length) {
      val result: String =
        (TagBenchmark.ioClassByteMap(
          TagBenchmark.values(i).getClass
        ): @switch) match {
          case 0  => "Pure"
          case 1  => "Error"
          case 2  => "Delay"
          case 3  => "ReadTime"
          case 4  => "Monotonic"
          case 5  => "ReadEC"
          case 6  => "Map"
          case 7  => "FlatMap"
          case 8  => "Attempt"
          case 9  => "HandleErrorWith"
          case 10 => "Canceled"
          case 11 => "OnCancel"
          case 12 => "Uncancelable"
          case 13 => "UnmaskRunLoop"
          case 14 => "IOCont"
          case 15 => "Get"
          case 16 => "Cede"
          case 17 => "Start"
          case 18 => "RacePair"
          case 19 => "Sleep"
          case 20 => "EvalOn"
          case 21 => "Blocking"
          case 22 => "Local"
          case 23 => "IOTrace"
          case 24 => "ReadRT"
          case -1 => "EndFiber"
        }
      bk.consume(result)
      i += 1
    }
  }

  @Benchmark
  def explicitTag(bk: Blackhole): Unit = {
    var i = 0
    while (i < TagBenchmark.valuesWithTags.length) {
      val result: String =
        (TagBenchmark.valuesWithTags(i).tagValue: @switch) match {
          case 0  => "Pure"
          case 1  => "Error"
          case 2  => "Delay"
          case 3  => "ReadTime"
          case 4  => "Monotonic"
          case 5  => "ReadEC"
          case 6  => "Map"
          case 7  => "FlatMap"
          case 8  => "Attempt"
          case 9  => "HandleErrorWith"
          case 10 => "Canceled"
          case 11 => "OnCancel"
          case 12 => "Uncancelable"
          case 13 => "UnmaskRunLoop"
          case 14 => "IOCont"
          case 15 => "Get"
          case 16 => "Cede"
          case 17 => "Start"
          case 18 => "RacePair"
          case 19 => "Sleep"
          case 20 => "EvalOn"
          case 21 => "Blocking"
          case 22 => "Local"
          case 23 => "IOTrace"
          case 24 => "ReadRT"
          case -1 => "EndFiber"
        }
      bk.consume(result)
      i += 1
    }
  }
}
