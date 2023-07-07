package uk.me.ruthmills.exifinate.thread;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MjpegStream implements Runnable {

	private static final int INPUT_BUFFER_SIZE = 16384;

	private URLConnection conn;
	private ByteArrayOutputStream outputStream;
	protected byte[] currentFrame = new byte[0];
	private Thread streamReader;
	private boolean connected;
	private String streamURL;

	private static final Logger logger = LoggerFactory.getLogger(MjpegStream.class);

	public void initialise() {
		this.streamReader = new Thread(this, "MJPEG stream reader");
		streamReader.setPriority(6); // higher priority than normal (5).
		streamReader.start();
		logger.info("Started MJPEG stream reader thread");
	}

	public void run() {
		while (true) {
			try (InputStream inputStream = openConnection()) {
				int prev = 0;
				int cur = 0;

				// EOF is -1
				while ((inputStream != null) && ((cur = inputStream.read()) >= 0)) {
					if (prev == 0xFF && cur == 0xD8) {
						outputStream = new ByteArrayOutputStream(INPUT_BUFFER_SIZE);
						outputStream.write((byte) prev);
					}
					if (outputStream != null) {
						outputStream.write((byte) cur);
						if (prev == 0xFF && cur == 0xD9) {
							synchronized (currentFrame) {
								currentFrame = outputStream.toByteArray();
							}
							outputStream.close();
							// the image is now available - read it
							handleNewFrame();
							if (!connected) {
								logger.info("Connected to camera successfully!");
								connected = true;
							}
						}
					}
					prev = cur;
				}
			} catch (Exception ex) {
				if (connected) {
					logger.error("Failed to read stream", ex);
				}
			}

			if (connected) {
				connected = false;
			}

			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				logger.error("Interrupted Exception", e);
			}
		}
	}

	private BufferedInputStream openConnection() throws IOException {
		BufferedInputStream bufferedInputStream = null;
		URL url = new URL(streamURL);
		conn = url.openConnection();
		conn.setReadTimeout(5000); // 5 seconds
		conn.connect();
		bufferedInputStream = new BufferedInputStream(conn.getInputStream(), INPUT_BUFFER_SIZE);
		return bufferedInputStream;
	}

	private void handleNewFrame() throws ImageWriteException {
		LocalDateTime now = LocalDateTime.now();
		String nowFormatted = now.format(DateTimeFormatter.ISO_DATE_TIME);
		logger.info(nowFormatted);
		TiffOutputSet outputSet = new TiffOutputSet();
		final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
		exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, nowFormatted);
	}
}
