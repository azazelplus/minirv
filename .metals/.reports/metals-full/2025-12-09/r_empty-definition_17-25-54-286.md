error id: file://<WORKSPACE>/src/main/scala/adder/Adder.scala:`<none>`.
file://<WORKSPACE>/src/main/scala/adder/Adder.scala
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -chisel3/sum.
	 -chisel3/sum#
	 -chisel3/sum().
	 -sum.
	 -sum#
	 -sum().
	 -scala/Predef.sum.
	 -scala/Predef.sum#
	 -scala/Predef.sum().
offset: 509
uri: file://<WORKSPACE>/src/main/scala/adder/Adder.scala
text:
```scala
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
// 所有的input和output端口都被声明在一个IO类型的对象中. 而且该对象名称必须是io.
//`Bundle` 是 Chisel 提供的数据结构，用来把**多个命名的信号**组织到一起，像这样：

// 
class Adder extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(8.W))   // 输入 a，8 位宽
    val b   = Input(UInt(8.W))   // 输入 b，8 位宽
    val sum@@ = Output(UInt(8.W))  // 输出和，8 位宽
  })

  // 加法逻辑
  io.sum := io.a + io.b
}

/**
 * 运行这个 object 来生成 Verilog 代码
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

```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.