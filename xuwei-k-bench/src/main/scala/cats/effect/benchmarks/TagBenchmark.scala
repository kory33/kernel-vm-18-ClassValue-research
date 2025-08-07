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

  val values: Array[FakeIO[?]] = Array(
    FakeIO.Pure(1),
    FakeIO.Error(new Throwable()),
    FakeIO.Delay(() => 2),
    FakeIO.RealTime,
    FakeIO.Monotonic,
    FakeIO.ReadEC,
    FakeIO.Map(FakeIO.Pure(3), (x: Int) => x + 1),
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
}
