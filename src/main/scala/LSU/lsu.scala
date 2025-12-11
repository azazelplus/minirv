// MiniRV 访存单元 (Load/Store Unit)
package minirv.lsu

import chisel3._
import chisel3.util._
import minirv._

/**
  * 内存操作类型 (funct3)
  */
object MemOp {
  val LB  = "b000".U(3.W)  // Load Byte (有符号扩展)
  val LH  = "b001".U(3.W)  // Load Halfword (有符号扩展)
  val LW  = "b010".U(3.W)  // Load Word
  val LBU = "b100".U(3.W)  // Load Byte Unsigned (零扩展)
  val LHU = "b101".U(3.W)  // Load Halfword Unsigned (零扩展)
  val SB  = "b000".U(3.W)  // Store Byte
  val SH  = "b001".U(3.W)  // Store Halfword
  val SW  = "b010".U(3.W)  // Store Word
}

/**
  * LSU - 访存单元
  * 
  * 功能：
  * 1. 通过 DPI-C 进行数据存储器读写
  * 2. 处理不同宽度的访存 (lw/lbu/sw/sb)
  * 3. 生成写回数据
  */
class LSU extends Module {
  val io = IO(new Bundle {
    // 来自 EXU
    val in = Input(new EX2LS)
    
    // 输出到 WBU
    val out = Output(new LS2WB)
  })

  val in = io.in
  val addr = in.alu_result
  val byte_offset = addr(1, 0)  // 地址的低 2 位，用于选择字节

  // ============ DPI-C 存储器读取 ============
  val dmem_read = Module(new PMEMRead)
  dmem_read.io.clock := clock
  dmem_read.io.raddr := addr
  
  // 从 32 位数据中根据地址偏移选择需要的字节/半字/字
  val rdata_raw = dmem_read.io.rdata
  
  // 根据 mem_op 和地址偏移提取正确的数据
  val load_data = WireDefault(0.U(Config.XLEN.W))
  
  switch(in.mem_op) {
    is(MemOp.LW) {
      // Load Word: 直接使用 32 位数据
      load_data := rdata_raw
    }
    is(MemOp.LB) {
      // Load Byte (有符号扩展)
      val byte_data = MuxLookup(byte_offset, 0.U(8.W))(Seq(
        0.U -> rdata_raw(7, 0),
        1.U -> rdata_raw(15, 8),
        2.U -> rdata_raw(23, 16),
        3.U -> rdata_raw(31, 24)
      ))
      load_data := Cat(Fill(24, byte_data(7)), byte_data)  // 符号扩展
    }
    is(MemOp.LBU) {
      // Load Byte Unsigned (零扩展)
      val byte_data = MuxLookup(byte_offset, 0.U(8.W))(Seq(
        0.U -> rdata_raw(7, 0),
        1.U -> rdata_raw(15, 8),
        2.U -> rdata_raw(23, 16),
        3.U -> rdata_raw(31, 24)
      ))
      load_data := Cat(0.U(24.W), byte_data)  // 零扩展
    }
    is(MemOp.LH) {
      // Load Halfword (有符号扩展)
      val half_data = Mux(byte_offset(1), rdata_raw(31, 16), rdata_raw(15, 0))
      load_data := Cat(Fill(16, half_data(15)), half_data)
    }
    is(MemOp.LHU) {
      // Load Halfword Unsigned (零扩展)
      val half_data = Mux(byte_offset(1), rdata_raw(31, 16), rdata_raw(15, 0))
      load_data := Cat(0.U(16.W), half_data)
    }
  }

  // ============ DPI-C 存储器写入 ============
  val dmem_write = Module(new PMEMWrite)
  dmem_write.io.clock := clock
  dmem_write.io.wen   := in.mem_wen
  dmem_write.io.waddr := addr
  
  // 根据 mem_op 和地址偏移生成写数据和写掩码
  val wdata = WireDefault(0.U(32.W))
  val wmask = WireDefault(0.U(4.W))
  
  switch(in.mem_op) {
    is(MemOp.SW) {
      // Store Word
      wdata := in.rs2_val
      wmask := "b1111".U
    }
    is(MemOp.SH) {
      // Store Halfword
      wdata := Mux(byte_offset(1),
        Cat(in.rs2_val(15, 0), 0.U(16.W)),
        Cat(0.U(16.W), in.rs2_val(15, 0))
      )
      wmask := Mux(byte_offset(1), "b1100".U, "b0011".U)
    }
    is(MemOp.SB) {
      // Store Byte
      wdata := MuxLookup(byte_offset, 0.U)(Seq(
        0.U -> Cat(0.U(24.W), in.rs2_val(7, 0)),
        1.U -> Cat(0.U(16.W), in.rs2_val(7, 0), 0.U(8.W)),
        2.U -> Cat(0.U(8.W), in.rs2_val(7, 0), 0.U(16.W)),
        3.U -> Cat(in.rs2_val(7, 0), 0.U(24.W))
      ))
      wmask := MuxLookup(byte_offset, 0.U)(Seq(
        0.U -> "b0001".U,
        1.U -> "b0010".U,
        2.U -> "b0100".U,
        3.U -> "b1000".U
      ))
    }
  }
  
  dmem_write.io.wdata := wdata
  dmem_write.io.wmask := wmask

  // ============ 输出到 WBU ============
  // 选择写回数据：Load 指令从内存读，其他从 ALU
  val wb_data = Mux(in.mem_ren, load_data, in.alu_result)

  io.out.wb_data := wb_data
  io.out.rd_addr := in.rd_addr
  io.out.reg_wen := in.reg_wen
}
