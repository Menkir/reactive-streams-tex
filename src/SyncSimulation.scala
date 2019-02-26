package benchmark

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import com.typesafe.scalalogging.Logger
import prototype.async.client.CarConfiguration
import prototype.sync.client.Car

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import ExecutionContext.Implicits.global

class SyncSimulation(hostInfo: InetSocketAddress = new InetSocketAddress("127.0.0.1", 1337)) extends Simulation {
  val logger: Logger = Logger[SyncSimulation]
  val executors: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(8))
  val durationList: List[Int] = List range(0,10) map(n => Math.pow(2, n toDouble).toInt * 1000)

  def run(config: CarConfiguration = new CarConfiguration()): Unit ={
    logger info "START TEST"
    warmUp()

    Future sequence(durationList map(duration => Future((duration, benchmark(duration, config))))) onComplete(
      result => {
        printResult(result get)
        saveResult("SyncBenchmarkResult.txt", result get)
        logger info "END TEST"
      })
  }

  override def benchmark(runtime: Int, config: CarConfiguration): Int ={
    logger.info("START BENCHMARK")
    val car = new Car(hostInfo, config)

    Future{
      car connect()
      car send()
    }(executors)
    Thread sleep runtime
    logger.info("Throughput: {} processed requests", car.getFlowrate)
    logger.info("END BENCHMARK")
    car.close()
    car.getFlowrate
  }

  def warmUp(): Unit={
    logger.info("START WARMUP")
    val car = new Car(hostInfo)
    car connect()
    car send 1500
    car close()
    logger.info("END WARMUP")
  }
}

object SyncSimulation{
  def main(args: Array[String]): Unit = {
    val simulation = new SyncSimulation()
    simulation.run()

  }
}