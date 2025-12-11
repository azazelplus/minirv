// DPI-C 存储器访问接口
// 通过 DPI-C 机制与 C++ 仿真环境交互
package minirv

import chisel3._
import chisel3.util._

/**
  * DPI-C 存储器读取模块
  * 
  * 使用 Chisel 的 BlackBox 机制，定义外部 DPI-C 函数接口。
  * 实际的函数实现在 C++ 代码中。
  * 
  * pmem_read: 从存储器读取 32 位数据
  *   - raddr: 读地址（需按 4 字节对齐）
  *   - 返回: 32 位数据
  */
class PMEMRead extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock  = Input(Clock())
    val raddr  = Input(UInt(32.W))   // 读地址
    val rdata  = Output(UInt(32.W))  // 读数据
  })

  // 内联 SystemVerilog 代码，使用 DPI-C 调用
  setInline("PMEMRead.sv",
    """module PMEMRead(
      |  input         clock,
      |  input  [31:0] raddr,
      |  output [31:0] rdata
      |);
      |
      |  // DPI-C 函数声明
      |  import "DPI-C" function int pmem_read(input int raddr);
      |
      |  // 调用 DPI-C 函数读取存储器
      |  // 地址按 4 字节对齐
      |  assign rdata = pmem_read({raddr[31:2], 2'b00});
      |
      |endmodule
      |""".stripMargin)
}

/**
  * DPI-C 存储器写入模块
  * 
  * pmem_write: 向存储器写入数据
  *   - waddr: 写地址（需按 4 字节对齐）
  *   - wdata: 写数据
  *   - wmask: 写掩码（按字节，4 位）
  */
class PMEMWrite extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock  = Input(Clock())
    val wen    = Input(Bool())       // 写使能
    val waddr  = Input(UInt(32.W))   // 写地址
    val wdata  = Input(UInt(32.W))   // 写数据
    val wmask  = Input(UInt(4.W))    // 写掩码（按字节）
  })

  // 内联 SystemVerilog 代码
  setInline("PMEMWrite.sv",
    """module PMEMWrite(
      |  input         clock,
      |  input         wen,
      |  input  [31:0] waddr,
      |  input  [31:0] wdata,
      |  input  [3:0]  wmask
      |);
      |
      |  // DPI-C 函数声明
      |  import "DPI-C" function void pmem_write(
      |    input int waddr,
      |    input int wdata,
      |    input byte wmask
      |  );
      |
      |  // 在时钟上升沿且写使能有效时，调用 DPI-C 函数
      |  always @(posedge clock) begin
      |    if (wen) begin
      |      pmem_write({waddr[31:2], 2'b00}, wdata, {4'b0, wmask});
      |    end
      |  end
      |
      |endmodule
      |""".stripMargin)
}

/**
  * EBREAK 检测模块
  * 用于在仿真中检测 EBREAK 指令并终止仿真
  */
class EBREAKDetect extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock   = Input(Clock())
    val inst    = Input(UInt(32.W))  // 当前指令
    val valid   = Input(Bool())      // 指令有效
  })

  setInline("EBREAKDetect.sv",
    """module EBREAKDetect(
      |  input         clock,
      |  input  [31:0] inst,
      |  input         valid
      |);
      |
      |  // DPI-C 函数声明
      |  import "DPI-C" function void ebreak_handler();
      |
      |  // EBREAK 指令编码: 0x00100073
      |  always @(posedge clock) begin
      |    if (valid && inst == 32'h00100073) begin
      |      ebreak_handler();
      |    end
      |  end
      |
      |endmodule
      |""".stripMargin)
}
