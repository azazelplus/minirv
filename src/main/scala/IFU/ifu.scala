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
  * 2. 从指令存储器读取指令
  * 3. 将 PC 和指令传递给 IDU
  */
class IFU extends Module {
  val io = IO(new Bundle {
    // 指令存储器接口
    val imem_addr = Output(UInt(Config.ADDR_WIDTH.W))  // 指令地址输出
    val imem_data = Input(UInt(Config.INST_WIDTH.W))   // 指令数据输入
    
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

  // 使用 PC 模块的输出作为指令地址
  io.imem_addr := pc_module.io.pc
  
  // 输出到 IDU
  io.out.pc   := pc_module.io.pc
  io.out.inst := io.imem_data
}
