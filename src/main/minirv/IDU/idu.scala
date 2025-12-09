// MiniRV 译码单元 (Instruction Decode Unit)
package minirv.idu

import chisel3._
import chisel3.util._
import minirv._

/**
  * IDU - 译码单元
  * 
  * 功能：
  * 1. 解析指令字段 (opcode, rd, rs1, rs2, funct3, funct7)
  * 2. 生成立即数 (I/S/B/U/J 类型)
  * 3. 读取寄存器堆
  * 4. 生成控制信号
  */
class IDU extends Module {
  val io = IO(new Bundle {
    // 来自 IFU
    val in = Input(new IF2ID)
    
    // 寄存器堆读端口
    val rs1_addr = Output(UInt(Config.REG_ADDR_W.W))
    val rs2_addr = Output(UInt(Config.REG_ADDR_W.W))
    val rs1_data = Input(UInt(Config.XLEN.W))
    val rs2_data = Input(UInt(Config.XLEN.W))
    
    // 输出到 EXU
    val out = Output(new ID2EX)
  })

  val inst = io.in.inst

  // 指令字段解析
  val opcode = inst(6, 0)
  val rd     = inst(11, 7)
  val funct3 = inst(14, 12)
  val rs1    = inst(19, 15)
  val rs2    = inst(24, 20)
  val funct7 = inst(31, 25)

  // 立即数生成
  val imm_i = Cat(Fill(20, inst(31)), inst(31, 20))                              // I-type
  val imm_s = Cat(Fill(20, inst(31)), inst(31, 25), inst(11, 7))                 // S-type
  val imm_b = Cat(Fill(19, inst(31)), inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)) // B-type
  val imm_u = Cat(inst(31, 12), 0.U(12.W))                                        // U-type
  val imm_j = Cat(Fill(11, inst(31)), inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)) // J-type

  // 根据 opcode 选择立即数
  val imm = MuxLookup(opcode, 0.U)(Seq(
    Opcode.I_TYPE -> imm_i,
    Opcode.LOAD   -> imm_i,
    Opcode.JALR   -> imm_i,
    Opcode.STORE  -> imm_s,
    Opcode.BRANCH -> imm_b,
    Opcode.LUI    -> imm_u,
    Opcode.AUIPC  -> imm_u,
    Opcode.JAL    -> imm_j
  ))

  // ALU 操作选择 (根据 opcode, funct3, funct7)
  val alu_op = WireDefault(ALUOp.ADD)
  
  when(opcode === Opcode.R_TYPE || opcode === Opcode.I_TYPE) {
    alu_op := MuxLookup(funct3, ALUOp.ADD)(Seq(
      "b000".U -> Mux(opcode === Opcode.R_TYPE && funct7(5), ALUOp.SUB, ALUOp.ADD),
      "b001".U -> ALUOp.SLL,
      "b010".U -> ALUOp.SLT,
      "b011".U -> ALUOp.SLTU,
      "b100".U -> ALUOp.XOR,
      "b101".U -> Mux(funct7(5), ALUOp.SRA, ALUOp.SRL),
      "b110".U -> ALUOp.OR,
      "b111".U -> ALUOp.AND
    ))
  }

  // 控制信号生成
  val is_r_type  = opcode === Opcode.R_TYPE
  val is_i_type  = opcode === Opcode.I_TYPE
  val is_load    = opcode === Opcode.LOAD
  val is_store   = opcode === Opcode.STORE
  val is_branch  = opcode === Opcode.BRANCH
  val is_jal     = opcode === Opcode.JAL
  val is_jalr    = opcode === Opcode.JALR
  val is_lui     = opcode === Opcode.LUI
  val is_auipc   = opcode === Opcode.AUIPC

  // 寄存器读地址
  io.rs1_addr := rs1
  io.rs2_addr := rs2

  // 输出
  io.out.pc       := io.in.pc
  io.out.rs1_val  := io.rs1_data
  io.out.rs2_val  := io.rs2_data
  io.out.imm      := imm
  io.out.rd_addr  := rd
  io.out.alu_op   := alu_op
  io.out.alu_src  := !is_r_type  // 非 R-type 使用立即数
  io.out.mem_wen  := is_store
  io.out.mem_ren  := is_load
  io.out.reg_wen  := is_r_type || is_i_type || is_load || is_jal || is_jalr || is_lui || is_auipc
  io.out.is_branch := is_branch
  io.out.is_jal   := is_jal
  io.out.is_jalr  := is_jalr
}
