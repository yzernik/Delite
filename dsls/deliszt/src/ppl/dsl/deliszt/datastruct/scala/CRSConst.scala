package ppl.dsl.deliszt.datastruct.scala

/**
 * author: Michael Wu (mikemwu@stanford.edu)
 * last modified: 03/29/2011
 *
 * Pervasive Parallelism Laboratory (PPL)
 * Stanford University
 */

class CRSConst(_values : Array[Int], mult : Int) {
  def apply(row : Int, i : Int) = {_values(row*mult + i)}
  def update(row : Int, i : Int, v : Int) = {_values(row*mult + i) = v}
}