error id: file://<WORKSPACE>/build.mill:`<none>`.
file://<WORKSPACE>/build.mill
empty definition using pc, found symbol in pc: `<none>`.
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -mill/os/up.
	 -mill/os/up#
	 -mill/os/up().
	 -scalalib/os/up.
	 -scalalib/os/up#
	 -scalalib/os/up().
	 -mill/bsp/os/up.
	 -mill/bsp/os/up#
	 -mill/bsp/os/up().
	 -os/up.
	 -os/up#
	 -os/up().
	 -scala/Predef.os.up.
	 -scala/Predef.os.up#
	 -scala/Predef.os.up().
offset: 823
uri: file://<WORKSPACE>/build.mill
text:
```scala
// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import mill.scalalib.TestModule.ScalaTest
import scalalib._
// support BSP
import mill.bsp._

// Note: This project requires .mill-jvm-opts file containing:
//   -Dchisel.project.root=${PWD}
// This is needed because Chisel needs to know the project root directory
// to properly generate and handle test directories and output files.
// See: https://github.com/com-lihaoyi/mill/issues/3840
// build.mill是mill项目中的构建定义文件. 它本质是个.scala文件. 它被要求放在./mill同级目录.

//"Mill 把“构建脚本就是代码”这一思路做得比 sbt 更整洁轻量。"


// 运行 .mill ./mill chisel_template.test 时, mill会:
// 1. 根据约定查找源代码(在./src下).
// 2. 处理依赖关系. 此处需要chisel库.
// 3. 编译 所有 源代码.
// 



object `chisel_template` extends SbtModule { m =>
  override def millSourcePath = super.millSourcePath / os.@@up
  override def scalaVersion = "2.13.16"
  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit",
    "-Ymacro-annotations",
  )
  override def ivyDeps = Agg(
    ivy"org.chipsalliance::chisel:7.0.0-RC1",
  )
  override def scalacPluginIvyDeps = Agg(
    ivy"org.chipsalliance:::chisel-plugin:7.0.0-RC1",
  )
  object test extends SbtTests with TestModule.ScalaTest {
    override def ivyDeps = m.ivyDeps() ++ Agg(
      ivy"org.scalatest::scalatest::3.2.19"
    )
  }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: `<none>`.