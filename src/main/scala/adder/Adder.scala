// 简单的加法器模块示例

//把本文件放在adder命名空间.
package adder


// 引入Chisel库的所有文件
import chisel3._

// 引入ChiselStage. 用于生成Verilog代码
import _root_.circt.stage.ChiselStage



/**
  * 一个简单的 8 位加法器
  * 输入两个数字，输出它们的和
  */

// chisel3提供一个基类 Module. 所有的模块都继承自它.
// 所有的input和output端口都被声明在一个IO类型的对象中. 而且该对象名称必须是io. 初始化一个IO类, 要传入一个Bundle对象作为参数.
// 综上, input/output端口声明必须写成 val io = IO(new Bundle {...})
// Bundle类 是 Chisel 提供的数据结构，用来把**多个命名的信号**组织到一起. 它的成员就是一坨input/output变量.

class Adder extends Module {

  //input/output端口声明  
  val io = IO(new Bundle {
    val a   = Input(UInt(8.W))   // 输入 a，8 位宽
    val b   = Input(UInt(8.W))   // 输入 b，8 位宽
    val sum = Output(UInt(8.W))  // 输出和sum，8 位宽
  })

  // 加法逻辑
  // := 是chisel运算符. 表示右侧信号驱动左侧信号.
  io.sum := io.a + io.b
}


/**

 * scala允许 类class + 同名单例对象object 共存在同一个文件.
 * 而且给它们起了个名字: 伴生类/伴生对象 (companion class/object).
 * 伴生类和对象, 互相可以访问彼此的 private成员.
 * 
 * chisel提供 App类, 用于快速创建可执行程序.
 * 运行这个 object 来生成 Verilog 代码:
 * 命令：./mill chisel_template.runMain adder.Adder
 */
object Adder extends App {
  println("正在生成 Adder.sv 文件...")
  ChiselStage.emitSystemVerilogFile(
    new Adder,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
  println("生成完成！查看 Adder.sv 文件")
}
