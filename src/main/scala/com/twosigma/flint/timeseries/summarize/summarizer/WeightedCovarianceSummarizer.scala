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

package com.twosigma.flint.timeseries.summarize.summarizer

import com.twosigma.flint.rdd.function.summarize.summarizer.{
  WeightedCovarianceOutput,
  WeightedCovarianceState,
  WeightedCovarianceSummarizer => OWeightedCovarianceSummarizer
}
import com.twosigma.flint.timeseries.row.Schema
import com.twosigma.flint.timeseries.summarize.ColumnList.Sequence
import com.twosigma.flint.timeseries.summarize._
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.types._

case class WeightedCovarianceSummarizerFactory(
  xColumn: String,
  yColumn: String,
  weightColumn: String
) extends BaseSummarizerFactory(xColumn, yColumn, weightColumn) {
  override def apply(inputSchema: StructType): WeightedCovarianceSummarizer =
    WeightedCovarianceSummarizer(inputSchema, prefixOpt, requiredColumns)
}

case class WeightedCovarianceSummarizer(
  override val inputSchema: StructType,
  override val prefixOpt: Option[String],
  override val requiredColumns: ColumnList
) extends FlippableSummarizer
  with FilterNullInput {
  private[this] val Sequence(Seq(xColumn, yColumn, weightColumn)) =
    requiredColumns
  private[this] val xColumnIndex = inputSchema.fieldIndex(xColumn)
  private[this] val yColumnIndex = inputSchema.fieldIndex(yColumn)
  private[this] val weightColumnIndex = inputSchema.fieldIndex(weightColumn)
  private[this] val xExtractor =
    asDoubleExtractor(inputSchema(xColumnIndex).dataType, xColumnIndex)
  private[this] val yExtractor =
    asDoubleExtractor(inputSchema(yColumnIndex).dataType, yColumnIndex)
  private[this] val weightExtractor = asDoubleExtractor(
    inputSchema(weightColumnIndex).dataType,
    weightColumnIndex
  )
  private val columnPrefix = s"${xColumn}_${yColumn}_$weightColumn"

  override type T = (Double, Double, Double)
  override type U = WeightedCovarianceState
  override type V = WeightedCovarianceOutput

  override val summarizer = new OWeightedCovarianceSummarizer()

  override val schema: StructType = Schema.of(
    s"${columnPrefix}_weightedCovariance" -> DoubleType
  )

  override def toT(r: InternalRow): T =
    (
      xExtractor(r),
      yExtractor(r),
      weightExtractor(r)
    )

  override def fromV(v: V): InternalRow = InternalRow(v.covariance)
}
