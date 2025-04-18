package java.lang

import scala.scalanative.runtime.{Throwable => NativeThrowable}

class Throwable protected (
    s: String,
    private var e: Throwable,
    enableSuppression: scala.Boolean,
    writableStackTrace: scala.Boolean
) extends NativeThrowable(writableStackTrace = writableStackTrace)
    with java.io.Serializable {

  def this(message: String, cause: Throwable) =
    this(message, cause, true, true)

  def this() = this(null, null)

  def this(s: String) = this(s, null)

  def this(e: Throwable) = this(if (e == null) null else e.toString, e)

  // We use an Array rather than, say, a List, so that Throwable does not
  // depend on the Scala collections.
  private var suppressed: Array[Throwable] = _

  final def addSuppressed(exception: Throwable): Unit = {
    if (exception eq null) {
      throw new java.lang.NullPointerException(
        "Cannot suppress a null exception."
      )
    }

    if (exception eq this) {
      throw new java.lang.IllegalArgumentException(
        "Self-suppression not permitted"
      )
    }

    if (enableSuppression) this.synchronized {
      if (suppressed eq null) {
        suppressed = Array(exception)
      } else {
        val length = suppressed.length
        val newSuppressed = new Array[Throwable](length + 1)
        System.arraycopy(suppressed, 0, newSuppressed, 0, length)
        newSuppressed(length) = exception
        suppressed = newSuppressed
      }
    }
  }

  override def fillInStackTrace(): Throwable = {
    super.fillInStackTrace()
    this
  }

  def getCause(): Throwable = e

  def getLocalizedMessage(): String = getMessage()

  def getMessage(): String = s

  def getStackTrace(): Array[StackTraceElement] = {
    // Be robust! Test this.stackTrace against null rather than relying upon
    // the value of writableStackTrace.
    //
    // subclass scala.util.control.NoStackTrace overrides fillInStackTrace()
    // so that it never touches this.stackTrace. This creates the situation
    // where writeableStackTrace is true and this.stackTrace is null.
    //
    // If scala code creates this situation, then by the Gell-Mann principle
    // user code in the wild is bound to do so.
    //
    // If stackTrace is null, no profit to calling fillInStackTrace().
    // If writableStackTrace is true, then fillInStackTrace has already
    // been called in the constructor. If the stack is not writable, then
    // it can not be filled in.

    if (stackTrace eq null) {
      new Array[StackTraceElement](0) // as specified by Java 8.
    } else {
      // stackTrace is read-only at this point, no synchronized necessary.
      stackTrace.clone
    }
  }

  final def getSuppressed(): Array[Throwable] = {
    this.synchronized { // workaround SN Issue #1091
      if (suppressed eq null) {
        new Array[Throwable](0)
      } else {
        suppressed.clone()
      }
    }
  }

  def initCause(cause: Throwable): Throwable = {
    // Java 8 spec says initCause has "at-most-once" semantics,
    // where implied use in a constructor counts.
    if (cause eq this) {
      throw new java.lang.IllegalArgumentException(
        "Self-causation not permitted"
      )
    }

    this.synchronized {
      if (e != null) {
        val msg = if (cause eq null) "a null" else cause.toString
        throw new java.lang.IllegalStateException(
          s"Can't overwrite cause with ${msg}"
        )
      } else {
        e = cause
      }
    }

    this
  }

  def printStackTrace(): Unit =
    printStackTrace(System.err)

  def printStackTrace(ps: java.io.PrintStream): Unit = ps.synchronized {
    printStackTrace(ps.println(_: String))
  }

  def printStackTrace(pw: java.io.PrintWriter): Unit = pw.synchronized {
    printStackTrace(pw.println(_: String))
  }

  private def printStackTrace(println: String => Unit): Unit = {
    val trace = getStackTrace()

    // Print current stack trace
    println(toString)
    if (trace.nonEmpty) {
      var i = 0
      while (i < trace.length) {
        println("\tat " + trace(i))
        i += 1
      }
    } else {
      println("")
    }

    // Print causes
    var parentStack = trace
    var throwable = getCause()
    while (throwable != null) {
      println("Caused by: " + throwable)

      val currentStack = throwable.getStackTrace()
      if (currentStack.nonEmpty) {
        val duplicates = countDuplicates(currentStack, parentStack)
        var i = 0
        while (i < currentStack.length - duplicates) {
          println("\tat " + currentStack(i))
          i += 1
        }
        if (duplicates > 0) {
          println("\t... " + duplicates + " more")
        }
      } else {
        println("")
      }

      parentStack = currentStack
      throwable = throwable.getCause()
    }
  }

  private def countDuplicates(
      currentStack: Array[StackTraceElement],
      parentStack: Array[StackTraceElement]
  ): Int = {
    var duplicates = 0
    var parentIndex = parentStack.length - 1
    var i = currentStack.length - 1
    while (i >= 0 && parentIndex >= 0) {
      val parentFrame = parentStack(parentIndex)
      if (parentFrame == currentStack(i)) {
        duplicates += 1
      } else {
        return duplicates
      }
      i -= 1
      parentIndex -= 1
    }
    duplicates
  }

  override def toString(): String = {
    val className = getClass.getName
    val message = getMessage()
    if (message eq null) className
    else className + ": " + message
  }
}

class ThreadDeath() extends Error()

/* java.lang.*Error.java */

class AbstractMethodError(s: String) extends IncompatibleClassChangeError(s) {
  def this() = this(null)
}

class AssertionError private (s: String, e: Throwable) extends Error(s, e) {
  def this() = this(null, null)
  def this(o: Object) = this(o.toString, null)
  def this(b: scala.Boolean) = this(b.toString, null)
  def this(c: scala.Char) = this(c.toString, null)
  def this(i: scala.Int) = this(i.toString, null)
  def this(l: scala.Long) = this(l.toString, null)
  def this(f: scala.Float) = this(f.toString, null)
  def this(d: scala.Double) = this(d.toString, null)
}

class BootstrapMethodError(s: String, e: Throwable) extends LinkageError(s, e) {
  def this(e: Throwable) = this(if (e == null) null else e.toString, e)
  def this(s: String) = this(s, null)
  def this() = this(null, null)
}

class ClassCircularityError(s: String) extends LinkageError(s) {
  def this() = this(null)
}

class ClassFormatError(s: String) extends LinkageError(s) {
  def this() = this(null)
}

class Error protected (
    s: String,
    e: Throwable,
    enabledSuppression: scala.Boolean,
    writableStackTrace: scala.Boolean
) extends Throwable(s, e, enabledSuppression, writableStackTrace) {
  def this(s: String, e: Throwable) = this(s, e, true, true)
  def this() = this(null, null)
  def this(s: String) = this(s, null)
  def this(e: Throwable) = this(if (e == null) null else e.toString, e)
}

class ExceptionInInitializerError private (s: String, private val e: Throwable)
    extends LinkageError(s) {
  def this(thrown: Throwable) = this(null, thrown)
  def this(s: String) = this(s, null)
  def this() = this(null, null)
  def getException(): Throwable = e
  override def getCause(): Throwable = e
}

class IllegalAccessError(s: String) extends IncompatibleClassChangeError(s) {
  def this() = this(null)
}

class IncompatibleClassChangeError(s: String) extends LinkageError(s) {
  def this() = this(null)
}

class InstantiationError(s: String) extends IncompatibleClassChangeError(s) {
  def this() = this(null)
}

class InternalError(s: String, e: Throwable) extends VirtualMachineError(s, e) {
  def this(s: String) = this(s, null)
  def this(e: Throwable) = this(null, e)
  def this() = this(null, null)
}

class LinkageError(s: String, e: Throwable) extends Error(s, e) {
  def this(s: String) = this(s, null)
  def this() = this(null)
}

class NoClassDefFoundError(s: String) extends LinkageError(s) {
  def this() = this(null)
}

class NoSuchFieldError(s: String) extends IncompatibleClassChangeError(s) {
  def this() = this(null)
}

class NoSuchMethodError(s: String) extends IncompatibleClassChangeError(s) {
  def this() = this(null)
}

class OutOfMemoryError(s: String) extends VirtualMachineError(s) {
  def this() = this(null)
}

class StackOverflowError(s: String) extends VirtualMachineError(s) {
  def this() = this(null)
}

class UnknownError(s: String) extends VirtualMachineError(s) {
  def this() = this(null)
}

class UnsatisfiedLinkError(s: String) extends LinkageError(s) {
  def this() = this(null)
}

class UnsupportedClassVersionError(s: String) extends ClassFormatError(s) {
  def this() = this(null)
}

class VerifyError(s: String) extends LinkageError(s) {
  def this() = this(null)
}

abstract class VirtualMachineError(s: String, e: Throwable)
    extends Error(s, e) {
  def this(s: String) = this(s, null)
  def this(e: Throwable) = this(null, e)
  def this() = this(null, null)
}

/* java.lang.*Exception.java */

class ArithmeticException(s: String) extends RuntimeException(s) {
  def this() = this(null)
}

class ArrayIndexOutOfBoundsException(s: String)
    extends IndexOutOfBoundsException(s) {
  def this(index: Int) = this("Array index out of range: " + index)
  def this() = this(null)
}

class ArrayStoreException(s: String) extends RuntimeException(s) {
  def this() = this(null)
}

class ClassCastException(s: String) extends RuntimeException(s) {
  def this() = this(null)
}

class ClassNotFoundException(s: String, e: Throwable)
    extends ReflectiveOperationException(s) {
  def this(s: String) = this(s, null)
  def this() = this(null, null)
  def getException(): Throwable = e
  override def getCause(): Throwable = e
}

class CloneNotSupportedException(s: String) extends Exception(s) {
  def this() = this(null)
}

class EnumConstantNotPresentException(e: Class[_ <: Enum[_]], c: String)
    extends RuntimeException(e.getName() + "." + c) {
  def enumType(): Class[_ <: Enum[_]] = e
  def constantName(): String = c
}

class Exception protected (
    s: String,
    e: Throwable,
    enabledSuppression: scala.Boolean,
    writableStackTrace: scala.Boolean
) extends Throwable(s, e, enabledSuppression, writableStackTrace) {
  def this(s: String, e: Throwable) = this(s, e, true, true)
  def this(e: Throwable) = this(if (e == null) null else e.toString, e)
  def this(s: String) = this(s, null)
  def this() = this(null, null)
}

class IllegalAccessException(s: String)
    extends ReflectiveOperationException(s) {
  def this() = this(null)
}

class IllegalArgumentException(s: String, e: Throwable)
    extends RuntimeException(s, e) {
  def this(e: Throwable) = this(if (e == null) null else e.toString, e)
  def this(s: String) = this(s, null)
  def this() = this(null, null)
}

class IllegalMonitorStateException(s: String) extends RuntimeException(s) {
  def this() = this(null)
}

class IllegalStateException(s: String, e: Throwable)
    extends RuntimeException(s, e) {
  def this(e: Throwable) = this(if (e == null) null else e.toString, e)
  def this(s: String) = this(s, null)
  def this() = this(null, null)
}

class IllegalThreadStateException(s: String)
    extends IllegalArgumentException(s) {
  def this() = this(null)
}

class IndexOutOfBoundsException(s: String) extends RuntimeException(s) {
  def this(index: scala.Long) = this(s"Index out of range: ${index}") // JDK 16
  def this(index: Int) = this(index.toLong) // JDK 9
  def this() = this(null.asInstanceOf[String])
}

class InstantiationException(s: String)
    extends ReflectiveOperationException(s) {
  def this() = this(null)
}

class InterruptedException(s: String) extends Exception(s) {
  def this() = this(null)
}

class NegativeArraySizeException(s: String) extends RuntimeException(s) {
  def this() = this(null)
}

class NoSuchFieldException(s: String) extends ReflectiveOperationException(s) {
  def this() = this(null)
}

class NoSuchMethodException(s: String) extends ReflectiveOperationException(s) {
  def this() = this(null)
}

class NumberFormatException(s: String) extends IllegalArgumentException(s) {
  def this() = this(null)
}

class ReflectiveOperationException(s: String, e: Throwable)
    extends Exception(s, e) {
  def this(e: Throwable) = this(if (e == null) null else e.toString, e)
  def this(s: String) = this(s, null)
  def this() = this(null, null)
}

class RejectedExecutionException(s: String, e: Throwable)
    extends RuntimeException(s, e) {
  def this(e: Throwable) = this(if (e == null) null else e.toString, e)
  def this(s: String) = this(s, null)
  def this() = this(null, null)
}

class RuntimeException protected (
    s: String,
    e: Throwable,
    enabledSuppression: scala.Boolean,
    writableStackTrace: scala.Boolean
) extends Exception(s, e, enabledSuppression, writableStackTrace) {
  def this(s: String, e: Throwable) = this(s, e, true, true)
  def this(e: Throwable) = this(if (e == null) null else e.toString, e)
  def this(s: String) = this(s, null)
  def this() = this(null, null)
}

class SecurityException(s: String, e: Throwable)
    extends RuntimeException(s, e) {
  def this(e: Throwable) = this(if (e == null) null else e.toString, e)
  def this(s: String) = this(s, null)
  def this() = this(null, null)
}

class StringIndexOutOfBoundsException(s: String)
    extends IndexOutOfBoundsException(s) {
  def this(index: Int) = this("String index out of range: " + index)
  def this() = this(null)
}

class TypeNotPresentException(t: String, e: Throwable)
    extends RuntimeException("Type " + t + " not present", e) {
  def typeName(): String = t
}

class UnsupportedOperationException(s: String, e: Throwable)
    extends RuntimeException(s, e) {
  def this() = this(null, null)
  def this(s: String) = this(s, null)
  def this(e: Throwable) = this(if (e == null) null else e.toString, e)
}
