// MiniRV 执行单元 (Execution Unit)
package minirv.exu

import chisel3._
import chisel3.util._
import minirv._

/**
  * EXU - 执行单元
  * 
  * 功能：
  * 1. ALU 运算
  * 2. 分支/跳转地址计算
  * 3. 分支条件判断
  */
class EXU extends Module {
  val io = IO(new Bundle {
    // 来自 IDU
    val in = Input(new ID2EX)
    
    // 输出到 LSU
    val out = Output(new EX2LS)
    
    // 跳转控制输出到 IFU
    val jump_en   = Output(Bool())
    val jump_addr = Output(UInt(Config.ADDR_WIDTH.W))
  })

  val in = io.in

  // ALU 操作数
  val alu_a = in.rs1_val
  val alu_b = Mux(in.alu_src, in.imm, in.rs2_val)

  // ALU 计算
  val alu_result = WireDefault(0.U(Config.XLEN.W))
  
  switch(in.alu_op) {
    is(ALUOp.ADD)  { alu_result := alu_a + alu_b }
    is(ALUOp.SUB)  { alu_result := alu_a - alu_b }
    is(ALUOp.AND)  { alu_result := alu_a & alu_b }
    is(ALUOp.OR)   { alu_result := alu_a | alu_b }
    is(ALUOp.XOR)  { alu_result := alu_a ^ alu_b }
    is(ALUOp.SLL)  { alu_result := alu_a << alu_b(4, 0) }
    is(ALUOp.SRL)  { alu_result := alu_a >> alu_b(4, 0) }
    is(ALUOp.SRA)  { alu_result := (alu_a.asSInt >> alu_b(4, 0)).asUInt }
    is(ALUOp.SLT)  { alu_result := (alu_a.asSInt < alu_b.asSInt).asUInt }
    is(ALUOp.SLTU) { alu_result := (alu_a < alu_b).asUInt }
  }

  // 分支条件判断 (根据 funct3，这里简化处理)
  // TODO: 需要从 IDU 传递 funct3 信息
  val branch_taken = WireDefault(false.B)
  when(in.is_branch) {
    // 简化：仅实现 BEQ (funct3 = 000)
    branch_taken := in.rs1_val === in.rs2_val
  }

  // 跳转地址计算
  val branch_addr = in.pc + in.imm         // B-type, JAL
  val jalr_addr   = (in.rs1_val + in.imm) & ~1.U(Config.ADDR_WIDTH.W)  // JALR

  // 跳转控制
  io.jump_en := in.is_jal || in.is_jalr || (in.is_branch && branch_taken)
  io.jump_addr := Mux(in.is_jalr, jalr_addr, branch_addr)

  // 输出到 LSU
  io.out.alu_result := Mux(in.is_jal || in.is_jalr, in.pc + 4.U, alu_result)  // JAL/JALR 存 PC+4
  io.out.rs2_val    := in.rs2_val
  io.out.rd_addr    := in.rd_addr
  io.out.mem_wen    := in.mem_wen
  io.out.mem_ren    := in.mem_ren
  io.out.reg_wen    := in.reg_wen
}
