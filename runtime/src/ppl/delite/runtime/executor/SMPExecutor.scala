package ppl.delite.runtime.executor

import ppl.delite.walktime.scheduler.StaticSchedule
import ppl.delite.Config

/**
 * Author: Kevin J. Brown
 * Date: Oct 17, 2010
 * Time: 9:55:02 AM
 * 
 * Pervasive Parallelism Laboratory (PPL)
 * Stanford University
 */

class SMPExecutor {

  val numThreads = Config.numThreads

  val threadPool = new ThreadPool(numThreads)

  def run(schedule: StaticSchedule) {
    threadPool.init
    println("Executor initialized")
    threadPool.submitAll(schedule)
    //TODO: we need a hook to return
    Thread.sleep(5000)
    println("Executor exiting")
  }

}