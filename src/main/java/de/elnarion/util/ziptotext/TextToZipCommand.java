package de.elnarion.util.ziptotext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.io.IOUtils;

import net.lingala.zip4j.ZipFile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "textToZip", mixinStandardHelpOptions = true, description = "Merges base 64 encoded text files, creates a zip from it and unzips the result to the target directory.")
public class TextToZipCommand implements Callable<Integer> {

	@ParentCommand
	private App app;

	@Parameters(index = "0", description = "The first text file.")
	private File file;

	@Parameters(index = "1", description = "The target directory.")
	private File targetdirectory;

	@Option(names = { "-p", "--password" }, description = "Use a password for unzipping.")
	private String password = null;
	
	@Option(names = { "-vb", "--verbose" }, description = "Write all actions to console and preserve tempfiles.")
	private Boolean verbose = false;

	private File tempZipFile;

	@Override
	public Integer call() throws Exception {
		tempZipFile = new File(targetdirectory, UUID.randomUUID().toString() + ".zip");
		tempZipFile.getParentFile().mkdirs();
		String filename = file.getName();
		char[] passwordCharset = null;
		if(password!=null)
			passwordCharset = password.toCharArray();
		int lastIndex = filename.lastIndexOf(".1.txt");
		if (lastIndex > 0)
		{
			filename = filename.substring(0, lastIndex);
			int counter = 1;
			boolean unfinished = true;
			List<FileInputStream> inputStreams = new ArrayList<>();
			do {
				File nextFile = new File(file.getParentFile(),filename+"."+counter+".txt");
				if(nextFile.exists())
					inputStreams.add(new FileInputStream(nextFile));
				else
					unfinished = false;
				counter++;
			} while (unfinished);
			SequenceInputStream sequenceInputStream = new SequenceInputStream(Collections.enumeration(inputStreams));
			writeTextToZipTarget(sequenceInputStream);
			ZipFile zipFile = new ZipFile(tempZipFile,passwordCharset);
			zipFile.extractAll(targetdirectory.getAbsolutePath());
			if(!verbose)
				tempZipFile.delete();
		}
		return null;
	}

	private void writeTextToZipTarget(InputStream paramSource) throws IOException {
			Base64InputStream bis = new Base64InputStream(paramSource);
			IOUtils.copy(bis, new FileOutputStream(tempZipFile,true));
	}

}
