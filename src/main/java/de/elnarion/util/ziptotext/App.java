package de.elnarion.util.ziptotext;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "convert", mixinStandardHelpOptions = true, version = "1.0", subcommands = { TextToZipCommand.class,
		ZipToTextCommand.class }, description = "Startercommand for different subcommands")
public class App implements Callable<Integer> {

	public static void main(String[] args) {
		CommandLine commandLine = new CommandLine(new App());
		commandLine.parseArgs(args);
		if (commandLine.isUsageHelpRequested()) {
			commandLine.usage(System.out);
			return;
		} else if (commandLine.isVersionHelpRequested()) {
			commandLine.printVersionHelp(System.out);
			return;
		}
		System.exit(commandLine.execute(args));
	}

	@Override
	public Integer call() throws Exception {
		return 0;
	}

}
