package de.elnarion.util.ziptotext;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.codec.binary.Base64OutputStream;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "zipToText", mixinStandardHelpOptions = true, description = "Zips a folder, converts it to a base64 encoded and splitted textfile and writes the files to a target.")
public class ZipToTextCommand implements Callable<Integer> {

	@ParentCommand
	private App app;

	@Parameters(index = "0", description = "The file/directory to zip.")
	private File file;

	@Parameters(index = "1", description = "The target file.")
	private File targetfile;

	@Option(names = { "-p", "--password" }, description = "Use a password for zipping.")
	private String password = null;

	@Option(names = { "-s", "--max-size-in-mb" }, description = "Maximum size for the target text file.")
	private Double maxSize = null;

	@Option(names = { "-vb", "--verbose" }, description = "Write all actions to console and preserve tempfiles.")
	private Boolean verbose = false;

	private static final int BUFFER_SIZE = 3 * 1024;

	@Override
	public Integer call() throws Exception {
		char[] passwordChars = null;
		ZipParameters parameters = null;
		if (password != null) {
			parameters = new ZipParameters();
			parameters.setEncryptFiles(true);
			parameters.setEncryptionMethod(EncryptionMethod.AES);
			passwordChars = password.toCharArray();
		}
		if (targetfile.exists())
			targetfile.delete();
		targetfile.getParentFile().mkdirs();
		ZipFile zipFile = new ZipFile(targetfile, passwordChars);
		if (verbose)
			System.out.println("zipping " + file.getAbsolutePath());
		if (file.isDirectory()) {
			zipFile.addFolder(file, parameters);
		} else {
			zipFile.addFile(file, parameters);
		}
		File textTargetFile = writeTargetToText();
		if (!verbose)
			targetfile.delete();
		if (maxSize != null) {
			if (verbose)
				System.out.println("splitting");
			splitFile(textTargetFile);
			if (!verbose)
				textTargetFile.delete();
		} else {
			if (verbose)
				System.out.println("not splitting");
			textTargetFile.renameTo(new File(textTargetFile.getAbsolutePath() + ".1.txt"));
		}
		return 0;
	}

	private File writeTargetToText() throws FileNotFoundException, IOException {
		try (FileInputStream input = new FileInputStream(targetfile)) {
			try (BufferedInputStream in = new BufferedInputStream(input, BUFFER_SIZE);) {
				return writeTargetToTextEncoded(in);
			}
		}
	}

	private File writeTargetToTextEncoded(BufferedInputStream in) throws IOException, FileNotFoundException {
		int len = 0;
		byte[] chunk = new byte[BUFFER_SIZE];
		File textTargetFile = new File(targetfile.getCanonicalPath() + ".txt");
		try (Base64OutputStream bos = new Base64OutputStream(new FileOutputStream(textTargetFile))) {
			while ((len = in.read(chunk)) == BUFFER_SIZE) {
				bos.write(chunk);
			}
			if (len > 0) {
				chunk = Arrays.copyOf(chunk, len);
				bos.write(chunk);
			}
		}
		return textTargetFile;
	}

	public List<Path> splitFile(File paramSourceFile) throws IOException {

		List<Path> partFiles = new ArrayList<>();
		final long sourceSize = Files.size(paramSourceFile.toPath());
		final long bytesPerSplit = (long) (1024L * 1024L * maxSize);
		final long numSplits = sourceSize / bytesPerSplit;
		final long remainingBytes = sourceSize % bytesPerSplit;
		int position = 0;

		if (verbose)
			System.out.println("Number of Splits " + numSplits);

		try (RandomAccessFile sourceFile = new RandomAccessFile(paramSourceFile, "r");
				FileChannel sourceChannel = sourceFile.getChannel()) {
			for (; position < numSplits; position++) {
				writePartToFile(bytesPerSplit, position * bytesPerSplit, sourceChannel, partFiles, position,
						paramSourceFile);
			}
			if (remainingBytes > 0) {
				writePartToFile(remainingBytes, position * bytesPerSplit, sourceChannel, partFiles, position,
						paramSourceFile);
			}
		}
		return partFiles;
	}

	private void writePartToFile(long byteSize, long position, FileChannel sourceChannel, List<Path> partFiles,
			int counter, File paramSourceFile) throws IOException {
		Path fileName = new File(targetfile.getAbsolutePath() + "." + (counter + 1) + ".txt").toPath();
		if (verbose)
			System.out.println("writing part to " + fileName.toString());
		try (RandomAccessFile toFile = new RandomAccessFile(fileName.toFile(), "rw");
				FileChannel toChannel = toFile.getChannel()) {
			sourceChannel.position(position);
			toChannel.transferFrom(sourceChannel, 0, byteSize);
		}
		partFiles.add(fileName);
	}

}
