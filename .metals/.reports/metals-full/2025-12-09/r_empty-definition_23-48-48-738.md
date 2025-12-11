error id: file://<WORKSPACE>/src/main/minirv/MiniRV.scala:`<error>`#`<error>`.
file://<WORKSPACE>/src/main/minirv/MiniRV.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -chisel3/ChiselStage.
	 -chisel3/util/ChiselStage.
	 -minirv/ifu/ChiselStage.
	 -minirv/idu/ChiselStage.
	 -minirv/exu/ChiselStage.
	 -minirv/lsu/ChiselStage.
	 -minirv/wbu/ChiselStage.
	 -_root_/circt/stage/ChiselStage.
	 -ChiselStage.
	 -scala/Predef.ChiselStage.
offset: 2156
uri: file://<WORKSPACE>/src/main/minirv/MiniRV.scala
text:
```scala
// MiniRV 顶层模块.
// chisel使用伴生对象的方式生成verilog代码.
// 所以, 判断哪个module是顶层模块, 就看哪个class有伴生object, 其内部生成verilog代码.


// scala的命名空间没有子继承关系.  minirv和minirv_ifu完全是平级的, 只是易读其关系.
package minirv


import chisel3._
import chisel3.util._
import minirv.ifu._
import minirv.idu._
import minirv.exu._
import minirv.lsu._
import minirv.wbu._
import _root_.circt.stage.ChiselStage


/**
  * MiniRV 顶层模块
  * 
  * 连接所有子模块：IFU -> IDU -> EXU -> LSU -> WBU
  * 单周期实现，无流水线
  */
class MiniRV extends Module {
  val io = IO(new Bundle {
    // 指令存储器接口.
    // imem=instruction memory指令存储器; dmem=data memory数据存储器
    // Input()和Output()是chisel提供的方向修饰器.
    // ADDR_WIDTH = 32， INST_WIDTH = 32
    // XLEN = 32
    val imem_addr = Output(UInt(Config.ADDR_WIDTH.W))
    val imem_data = Input(UInt(Config.INST_WIDTH.W))
    
    // 数据存储器接口
    val dmem_addr  = Output(UInt(Config.ADDR_WIDTH.W))
    val dmem_wdata = Output(UInt(Config.XLEN.W))
    val dmem_wen   = Output(Bool())
    val dmem_ren   = Output(Bool())
    val dmem_rdata = Input(UInt(Config.XLEN.W))
  })

  // 实例化各模块
  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)
  val lsu = Module(new LSU)
  val wbu = Module(new WBU)
  val regfile = Module(new RegFile)

  // IFU 连接
  io.imem_addr     := ifu.io.imem_addr
  ifu.io.imem_data := io.imem_data
  ifu.io.jump_en   := exu.io.jump_en
  ifu.io.jump_addr := exu.io.jump_addr

  // IDU 连接
  idu.io.in := ifu.io.out
  idu.io.rs1_addr <> regfile.io.rs1_addr
  idu.io.rs2_addr <> regfile.io.rs2_addr
  idu.io.rs1_data := regfile.io.rs1_data
  idu.io.rs2_data := regfile.io.rs2_data

  // EXU 连接
  exu.io.in := idu.io.out

  // LSU 连接
  lsu.io.in := exu.io.out
  io.dmem_addr  := lsu.io.dmem_addr
  io.dmem_wdata := lsu.io.dmem_wdata
  io.dmem_wen   := lsu.io.dmem_wen
  io.dmem_ren   := lsu.io.dmem_ren
  lsu.io.dmem_rdata := io.dmem_rdata

  // WBU 连接
  wbu.io.in := lsu.io.out
  regfile.io.rd_addr := wbu.io.rd_addr
  regfile.io.rd_data := wbu.io.rd_data
  regfile.io.rd_wen  := wbu.io.rd_wen
}

/**
  * 生成 Verilog
  * 运行命令：./mill chisel_template.runMain minirv.MiniRV
  */
object MiniRV extends App {
  println("正在生成 MiniRV.sv 文件...")
  ChiselS@@tage.emitSystemVerilogFile(
    new MiniRV,
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
  println("生成完成！")
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 