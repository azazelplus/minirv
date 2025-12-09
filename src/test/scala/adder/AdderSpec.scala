// 加法器的测试代码
package adder

import chisel3._
import chisel3.simulator.scalatest.ChiselSim
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

/**
  * 测试加法器是否正确工作
  * 运行命令：./mill chisel_template.test.testOnly adder.AdderSpec
  */
class AdderSpec extends AnyFreeSpec with Matchers with ChiselSim {

  "Adder should correctly add two numbers" in {
    simulate(new Adder) { dut =>
      // 测试 1: 3 + 5 = 8
      dut.io.a.poke(3.U)
      dut.io.b.poke(5.U)
      dut.io.sum.expect(8.U)
      
      // 测试 2: 10 + 20 = 30
      dut.io.a.poke(10.U)
      dut.io.b.poke(20.U)
      dut.io.sum.expect(30.U)
      
      // 测试 3: 0 + 0 = 0
      dut.io.a.poke(0.U)
      dut.io.b.poke(0.U)
      dut.io.sum.expect(0.U)
      
      // 测试 4: 255 + 1 = 0 (溢出，因为是 8 位)
      dut.io.a.poke(255.U)
      dut.io.b.poke(1.U)
      dut.io.sum.expect(0.U)  // 8位无符号数溢出
      
      println("所有加法器测试通过！")
    }
  }
}
