package millfork.compiler.mos

import millfork.assembly.mos.AssemblyLine
import millfork.compiler.{BranchSpec, CompilationContext}
import millfork.env.{Label, NumericConstant, Type, VariableInMemory}
import millfork.node._
import millfork.assembly.mos.Opcode._

/**
  * @author Karol Stasiak
  */
object MosBulkMemoryOperations {

  def compileMemset(ctx: CompilationContext, target: IndexedExpression, source: Expression, f: ForStatement): List[AssemblyLine] = {
    if (ctx.options.zpRegisterSize < 2 ||
      target.name != f.variable ||
      target.index.containsVariable(f.variable) ||
      !target.index.isPure ||
      f.direction == ForDirection.DownTo) return MosStatementCompiler.compileForStatement(ctx, f)
    val sizeExpr = f.direction match {
      case ForDirection.DownTo =>
        SumExpression(List(false -> f.start, true -> f.end, false -> LiteralExpression(1, 1)), decimal = false)
      case ForDirection.To | ForDirection.ParallelTo =>
        SumExpression(List(false -> f.end, true -> f.start, false -> LiteralExpression(1, 1)), decimal = false)
      case ForDirection.Until | ForDirection.ParallelUntil =>
        SumExpression(List(false -> f.end, true -> f.start), decimal = false)
    }
    val reg = ctx.env.get[VariableInMemory]("__reg.loword")
    val w = ctx.env.get[Type]("word")
    val size = ctx.env.eval(sizeExpr) match {
      case Some(c) => c.quickSimplify
      case _ => return MosStatementCompiler.compileForStatement(ctx, f)
    }
    val loadReg = MosExpressionCompiler.compile(ctx, SumExpression(List(false -> f.start, false -> target.index), decimal = false), Some(w -> reg), BranchSpec.None)
    val loadSource = MosExpressionCompiler.compileToA(ctx, source)
    val loadAll = if (MosExpressionCompiler.changesZpreg(loadSource, 0) || MosExpressionCompiler.changesZpreg(loadSource, 1)) {
      loadSource ++ MosExpressionCompiler.preserveRegisterIfNeeded(ctx, MosRegister.A, loadReg)
    } else {
      loadReg ++ loadSource
    }
    val wholePageCount = size.hiByte
    val setWholePages = wholePageCount match {
      case NumericConstant(0, _) => Nil
      case NumericConstant(1, _) =>
        val label = ctx.nextLabel("ms")
        List(
            AssemblyLine.immediate(LDY, 0),
            AssemblyLine.label(label),
            AssemblyLine.indexedY(STA, reg),
            AssemblyLine.implied(INY),
            AssemblyLine.relative(BNE, label))
      case _ =>
        val labelX = ctx.nextLabel("ms")
        val labelXSkip = ctx.nextLabel("ms")
        val labelY = ctx.nextLabel("ms")
        List(
          AssemblyLine.immediate(LDX, wholePageCount),
          AssemblyLine.relative(BEQ, labelXSkip),
          AssemblyLine.label(labelX),
          AssemblyLine.immediate(LDY, 0),
          AssemblyLine.label(labelY),
          AssemblyLine.indexedY(STA, reg),
          AssemblyLine.implied(INY),
          AssemblyLine.relative(BNE, labelY),
          AssemblyLine.zeropage(INC, reg, 1),
          AssemblyLine.implied(DEX),
          AssemblyLine.relative(BNE, labelX),
          AssemblyLine.label(labelXSkip))
    }
    val restSize = size.loByte
    val setRest = restSize match {
      case NumericConstant(0, _) => Nil
      case NumericConstant(1, _) =>
        List(AssemblyLine.indexedY(STA, reg))
      case NumericConstant(2, _) => List(
        AssemblyLine.indexedY(STA, reg),
        AssemblyLine.implied(INY),
        AssemblyLine.indexedY(STA, reg))
      case _ =>
        val label = ctx.nextLabel("ms")
        val labelSkip = ctx.nextLabel("ms")
        if (f.direction == ForDirection.ParallelUntil) {
          List(
            AssemblyLine.immediate(LDY, restSize),
            AssemblyLine.relative(BEQ, labelSkip),
            AssemblyLine.label(label),
            AssemblyLine.implied(DEY),
            AssemblyLine.indexedY(STA, reg),
            AssemblyLine.relative(BNE, label),
            AssemblyLine.label(labelSkip))
        } else {
          List(
            AssemblyLine.immediate(LDY, 0),
            AssemblyLine.label(label),
            AssemblyLine.immediate(CPY, restSize),
            AssemblyLine.relative(BCS, labelSkip),
            AssemblyLine.indexedY(STA, reg),
            AssemblyLine.implied(INY),
            AssemblyLine.relative(BNE, label),
            AssemblyLine.label(labelSkip))
        }
    }
    loadAll ++ setWholePages ++ setRest
  }
}