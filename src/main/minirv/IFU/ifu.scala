// MiniRV 取指单元 (Instruction Fetch Unit)
package minirv.ifu

import chisel3._
import chisel3.util._
import minirv._

/**
  * IFU - 取指单元
  * 
  * 功能：
  * 1. 维护 PC (程序计数器)
  * 2. 从指令存储器读取指令
  * 3. 计算下一条 PC (顺序/跳转)
  */
class IFU extends Module {
  val io = IO(new Bundle {
    // 指令存储器接口
    val imem_addr = Output(UInt(Config.ADDR_WIDTH.W))
    val imem_data = Input(UInt(Config.INST_WIDTH.W))
    
    // 输出到 IDU
    val out = Output(new IF2ID)
    
    // 跳转控制 (来自 EXU)
    val jump_en   = Input(Bool())
    val jump_addr = Input(UInt(Config.ADDR_WIDTH.W))
  })

  // PC 寄存器，初始值为 0x80000000 (可根据需求修改)
  val pc = RegInit("h80000000".U(Config.ADDR_WIDTH.W))

  // 下一条 PC：跳转或顺序 +4
  val next_pc = Mux(io.jump_en, io.jump_addr, pc + 4.U)

  // 更新 PC
  pc := next_pc

  // 输出
  io.imem_addr := pc
  io.out.pc    := pc
  io.out.inst  := io.imem_data
}
