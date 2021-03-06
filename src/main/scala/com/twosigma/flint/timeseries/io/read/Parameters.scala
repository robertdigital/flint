/*
 *  Copyright 2018 TWO SIGMA OPEN SOURCE, LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.twosigma.flint.timeseries.io.read

import java.time.{ Instant, ZonedDateTime, ZoneOffset }
import javax.annotation.Nullable

import scala.collection.mutable

import com.twosigma.flint.annotation.PythonApi

private[read] class Parameters private (
  val extraOptions: mutable.Map[String, String],
  var range: BeginEndRange = BeginEndRange(None, None, None, None)
) extends Serializable {

  def this(defaultOptions: Map[String, String]) =
    this(mutable.HashMap[String, String](defaultOptions.toSeq: _*))

  def option(key: String, valueOpt: Option[String]): Unit = valueOpt match {
    case Some(v) => extraOptions += key -> v
    case None => extraOptions -= key
  }

  /**
   * Convenience method for the Python API that provides a Java Map compatible with py4j.
   * Exposed in the Python API as `_extra_options` to return a dict of key-value options.
   */
  @PythonApi
  private[read] def extraOptionsAsJavaMap: java.util.Map[String, String] = {
    import scala.collection.JavaConverters._
    extraOptions.asJava
  }

}

private[read] case class BeginEndRange(
  rawBeginNanosOpt: Option[Long] = None,
  rawEndNanosOpt: Option[Long] = None,
  expandBeginNanosOpt: Option[Long] = None,
  expandEndNanosOpt: Option[Long] = None
) {

  def beginNanos: Long = beginNanosOpt.getOrElse(
    throw new IllegalArgumentException("'begin' range must be set")
  )

  def endNanos: Long = endNanosOpt.getOrElse(
    throw new IllegalArgumentException("'end' range must be set")
  )

  def beginNanosOpt: Option[Long] = {
    rawBeginNanosOpt.map(_ - expandBeginNanosOpt.getOrElse(0L))
  }

  def endNanosOpt: Option[Long] = {
    rawEndNanosOpt.map(_ + expandEndNanosOpt.getOrElse(0L))
  }

  @PythonApi
  private[read] def beginNanosOrNull: java.lang.Long = beginNanosOpt.map(Long.box).orNull

  @PythonApi
  private[read] def endNanosOrNull: java.lang.Long = endNanosOpt.map(Long.box).orNull
}
