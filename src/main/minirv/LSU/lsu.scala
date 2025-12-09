// MiniRV 访存单元 (Load/Store Unit)
package minirv.lsu

import chisel3._
import chisel3.util._
import minirv._

/**
  * LSU - 访存单元
  * 
  * 功能：
  * 1. 数据存储器读写
  * 2. 生成写回数据 (来自 ALU 或内存)
  */
class LSU extends Module {
  val io = IO(new Bundle {
    // 来自 EXU
    val in = Input(new EX2LS)
    
    // 数据存储器接口
    val dmem_addr  = Output(UInt(Config.ADDR_WIDTH.W))
    val dmem_wdata = Output(UInt(Config.XLEN.W))
    val dmem_wen   = Output(Bool())
    val dmem_ren   = Output(Bool())
    val dmem_rdata = Input(UInt(Config.XLEN.W))
    
    // 输出到 WBU
    val out = Output(new LS2WB)
  })

  val in = io.in

  // 数据存储器接口
  io.dmem_addr  := in.alu_result  // ALU 结果作为访存地址
  io.dmem_wdata := in.rs2_val     // rs2 作为写数据
  io.dmem_wen   := in.mem_wen
  io.dmem_ren   := in.mem_ren

  // 选择写回数据：Load 指令从内存读，其他从 ALU
  val wb_data = Mux(in.mem_ren, io.dmem_rdata, in.alu_result)

  // 输出到 WBU
  io.out.wb_data := wb_data
  io.out.rd_addr := in.rd_addr
  io.out.reg_wen := in.reg_wen
}
