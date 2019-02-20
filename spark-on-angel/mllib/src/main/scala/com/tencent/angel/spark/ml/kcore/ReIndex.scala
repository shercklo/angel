package com.tencent.angel.spark.ml.kcore

import scala.collection.mutable.ArrayBuffer

import com.tencent.angel.ml.math2.VFactory
import com.tencent.angel.ml.math2.vector.{IntLongVector, LongIntVector}
import com.tencent.angel.ml.matrix.RowType
import com.tencent.angel.spark.models.PSVector

class ReIndex(numNodes: Long) extends Serializable {
  private val long2intPsVector: PSVector = {
    PSVector.longKeySparse(numNodes, -1, 1, RowType.T_INT_SPARSE_LONGKEY)
  }
  private val int2longPsVector: PSVector = {
    PSVector.dense(numNodes, 1, RowType.T_LONG_DENSE)
  }

  def train(pairs: Iterator[(Long, Long)]): Unit = {
    val keys = new ArrayBuffer[Long]()
    val values = new ArrayBuffer[Int]()
    pairs.foreach { case (key, value) =>
      keys += key
      values += value.toInt
    }
    val longs = keys.toArray
    val ints2 = values.toArray
    long2intPsVector.update(VFactory.sparseLongKeyIntVector(numNodes.toLong, longs, ints2))
    int2longPsVector.update(VFactory.sparseLongVector(numNodes.toInt, ints2, longs))
  }

  def encode(keys: Array[Long], values: Array[Array[Long]]): (Array[Int], Array[Array[Int]]) = {
    val nodes = (values.flatten ++ keys).distinct
    val long2int = long2intPsVector.pull(nodes).asInstanceOf[LongIntVector]
    val newKeys = keys.map(long2int.get)
    val newValues = values.map(_.map(long2int.get))
    (newKeys, newValues)
  }

  def decode(nodes: Array[Int]): Array[Long] = {
    val copy = new Array[Int](nodes.length)
    System.arraycopy(nodes, 0, copy, 0, nodes.length)
    int2longPsVector.pull(copy).asInstanceOf[IntLongVector].get(nodes)
  }
}
