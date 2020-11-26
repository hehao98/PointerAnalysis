package edu.pku;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.PackManager;
import soot.Transform;

public class PointerAnalyzer {

	private static final Logger LOG = LoggerFactory.getLogger(PointerAnalyzer.class);

	public static String mainClass;

	private static void printUsageAndExit() {
		LOG.error("Usage: java -jar analyzer.jar <inputPath> <org.package.mainClass>");
		System.exit(-1);
	}

	public static void main(String[] args) {
		if (args.length < 2) printUsageAndExit();
		File path = new File(args[0]);
		if (!path.isDirectory()) printUsageAndExit();
		String classpath = args[0] 
				+ File.pathSeparator + args[0] + File.separator + "rt.jar"
				+ File.pathSeparator + args[0] + File.separator + "jce.jar";
		mainClass = args[1];
		LOG.info("classPaths={}, mainClass={}", classpath, args[1]);

		PackManager.v().getPack("wjtp")  // Add
				.add(new Transform("wjtp.mypta", new WholeProgramTransformer()));

		soot.Main.main(new String[] {
			"-w",                               // Run in whole program mode
			"-p", "cg.spark", "enabled:true",   // Enable a phase named "cg.spark", which builds a call graph
												//     and generates information about the target of pointers
			"-p", "wjtp.mypta", "enabled:true", // Enable a phase named "wjtp.mypta", as defined before
			"-soot-class-path", classpath,      // Class paths that we concatenate before
			"-f", "J",                          // Generate Jimple file in the args[0] folder
			args[1]		                        // Main class name
		});
	}
}
