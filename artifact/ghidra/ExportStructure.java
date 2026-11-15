// Ghidra script: for every function of the analysed program, decompile it and
// write its call targets and integer constants, one function per line:
//
//	name <TAB> callee,callee,... <TAB> const,const,...
//
// Used by eval/corpusrun to compare what a decompiler recovers from a stripped
// binary with what the attacker's normal form of the source contains.
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;
import ghidra.program.model.pcode.HighFunction;
import ghidra.program.model.pcode.PcodeOp;
import ghidra.program.model.pcode.PcodeOpAST;
import ghidra.program.model.pcode.Varnode;

public class ExportStructure extends GhidraScript {
	@Override
	public void run() throws Exception {
		String[] args = getScriptArgs();
		String out = args[0];
		DecompInterface decomp = new DecompInterface();
		decomp.openProgram(currentProgram);
		try (PrintWriter w = new PrintWriter(new FileWriter(out))) {
			FunctionIterator it = currentProgram.getFunctionManager().getFunctions(true);
			while (it.hasNext() && !monitor.isCancelled()) {
				Function f = it.next();
				if (f.isExternal() || f.isThunk()) {
					continue;
				}
				String name = f.getName(true);
				boolean wanted = args.length == 1;
				for (int i = 1; i < args.length; i++) {
					wanted = wanted || name.contains(args[i]);
				}
				if (!wanted) {
					continue;
				}
				DecompileResults res = decomp.decompileFunction(f, 60, monitor);
				HighFunction hf = res.getHighFunction();
				if (hf == null) {
					w.println(name + "\t!\t");
					continue;
				}
				Set<String> calls = new TreeSet<>();
				Set<Long> consts = new TreeSet<>();
				Iterator<PcodeOpAST> ops = hf.getPcodeOps();
				while (ops.hasNext()) {
					PcodeOpAST op = ops.next();
					int first = 0;
					if (op.getOpcode() == PcodeOp.CALL) {
						Function t = getFunctionAt(op.getInput(0).getAddress());
						if (t != null) {
							calls.add(t.getName(true));
						}
						first = 1;
					} else if (op.getOpcode() == PcodeOp.CALLIND) {
						first = 1;
					}
					for (int i = first; i < op.getNumInputs(); i++) {
						Varnode v = op.getInput(i);
						if (v != null && v.isConstant() && v.getOffset() >= 1000) {
							consts.add(v.getOffset());
						}
					}
				}
				StringBuilder cs = new StringBuilder();
				for (Long c : consts) {
					if (cs.length() > 0) {
						cs.append(',');
					}
					cs.append(Long.toUnsignedString(c));
				}
				w.println(name + "\t" + String.join(",", calls) + "\t" + cs);
			}
		}
	}
}
