// MiniRV 取指单元 (Instruction Fetch Unit)
package minirv.ifu

import chisel3._
import chisel3.util._
import minirv._

/**
  * IFU - 取指单元
  * 
  * 功能：
  * 1. 使用 PC 模块获取当前指令地址
  * 2. 通过 DPI-C 从存储器读取指令（不再拉到顶层）
  * 3. 将 PC 和指令传递给 IDU
  */
class IFU extends Module {
  val io = IO(new Bundle {
    // 输出到 IDU
    val out = Output(new IF2ID)
    
    // 跳转控制 (来自 EXU)
    val jump_en   = Input(Bool())
    val jump_addr = Input(UInt(Config.ADDR_WIDTH.W))
  })

  // 实例化 PC 模块
  val pc_module = Module(new PC)
  
  // 连接 PC 模块的跳转控制信号
  pc_module.io.jump_en   := io.jump_en
  pc_module.io.jump_addr := io.jump_addr

  // 实例化 DPI-C 存储器读取模块（取指）
  val imem_read = Module(new PMEMRead)
  imem_read.io.clock := clock
  imem_read.io.raddr := pc_module.io.pc

  // 输出到 IDU
  io.out.pc   := pc_module.io.pc
  io.out.inst := imem_read.io.rdata
}
