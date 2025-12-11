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
  * 
  * 使用 DPI-C 机制访问存储器，不再需要外部存储器接口
  */
class MiniRV extends Module {
  val io = IO(new Bundle {
    // 调试接口（可选）
    val debug_pc = Output(UInt(Config.ADDR_WIDTH.W))
    val debug_inst = Output(UInt(Config.INST_WIDTH.W))
  })

  // 实例化各模块
  val ifu = Module(new IFU)
  val idu = Module(new IDU)
  val exu = Module(new EXU)
  val lsu = Module(new LSU)
  val wbu = Module(new WBU)
  val regfile = Module(new RegFile)

  // IFU 连接（不再需要外部存储器接口，IFU 内部使用 DPI-C）
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

  // LSU 连接（不再需要外部存储器接口，LSU 内部使用 DPI-C）
  lsu.io.in := exu.io.out

  // WBU 连接
  wbu.io.in := lsu.io.out
  regfile.io.rd_addr := wbu.io.rd_addr
  regfile.io.rd_data := wbu.io.rd_data
  regfile.io.rd_wen  := wbu.io.rd_wen

  // EBREAK 检测（用于仿真终止）
  val ebreak_detect = Module(new EBREAKDetect)
  ebreak_detect.io.clock := clock
  ebreak_detect.io.inst  := ifu.io.out.inst
  ebreak_detect.io.valid := true.B

  // 调试输出
  io.debug_pc   := ifu.io.out.pc
  io.debug_inst := ifu.io.out.inst
}

/**
  * 生成 Verilog
  * 运行命令：./mill minirv.runMain minirv.MiniRV
  */
object MiniRV extends App {
  // 指定输出目录
  val outputDir = "generated"
  
  println(s"正在生成 MiniRV.sv 文件到 $outputDir/ ...")
  ChiselStage.emitSystemVerilogFile(
    new MiniRV,
    args = Array("--target-dir", outputDir),  // 指定输出目录
    firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
  )
  println(s"生成完成！文件位置: $outputDir/MiniRV.sv")
}
