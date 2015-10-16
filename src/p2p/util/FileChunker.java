package p2p.util;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileChunker {

	Logger logger = Logger.getLogger(FileChunker.class.getName());
	public static final String fileName = "43394921.txt";
	private static final String fileContent = "\nThis content is just a repetitive boring one line that has been written to make this line look longer and the file size to grow really faster. If you are still reading this, Just stop. - \nLine:";

	private static FileChunker chunker = null;

	private FileChunker(int noOfChunks) {
		// Initialization code to read the files.
		try {
			File f = createFile(noOfChunks);
			splitFile(f, noOfChunks);
		} catch (SecurityException | IOException e) {
			logger.log(Level.SEVERE, e.getMessage());
		}

	}

	private File createFile(int noOfChunks) throws SecurityException,
			IOException {
		File f = new File(fileName);
		if (!f.exists() || f.length() == 0L) {
			f.createNewFile();
			FileWriter fileWritter = new FileWriter(f.getName(), true);
			BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
			long timestamp = System.currentTimeMillis();
			long localVar = 0L;
			StringBuffer sb;
			while (f.length() < (noOfChunks + 1) * 100 * 1024) {
				sb = new StringBuffer();
				for (int i = 0; i < 1000; i++) {
					sb.append(fileContent + localVar++ + "\n");
				}
				fileWritter.append(sb);
				bufferWritter.flush();
			}
			bufferWritter.close();
			logger.info("Created File of size : " + f.length() + "bytes in "
					+ (System.currentTimeMillis() - timestamp)
					+ " milliseconds");
		}
		return f;

	}

	private void splitFile(File f, int noOfChunks)
			throws FileNotFoundException, IOException {
		if (f.length() > 0L) {
			int partCtr = 1;
			int sizeOfChunks = 100 * 1024;// 100KB chunks.
			byte[] buffer = new byte[sizeOfChunks];

			try (BufferedInputStream bis = new BufferedInputStream(
					new FileInputStream(f))) {
				int tmp = 0;
				while ((tmp = bis.read(buffer)) > 0) {
					File newFile = new File(f.getParent(), f.getName() + "."
							+ String.format("%03d", partCtr++));
					try (FileOutputStream out = new FileOutputStream(newFile)) {
						out.write(buffer, 0, tmp);
						logger.info("Created File : " + newFile.getName()
								+ " size : " + newFile.length() + "bytes");
					}
				}
			}
		}
	}

	public static FileChunker getInstance(int noOfChunks) {
		if (chunker == null) {
			chunker = new FileChunker(noOfChunks);
		}
		return chunker;
	}

//	public static void main(String[] args) throws Exception {
//		FileChunker fc = new FileChunker(10);
//	}

}
