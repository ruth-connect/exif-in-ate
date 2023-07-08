package uk.me.ruthmills.exifinate.controller;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import uk.me.ruthmills.exifinate.service.ImageService;

@Controller
public class MjpegStreamController {

	private static final String NL = "\r\n";
	private static final String BOUNDARY = "--boundary";
	private static final String HEAD = NL + NL + BOUNDARY + NL + "Content-Type: image/jpeg" + NL + "Content-Length: ";

	@Autowired
	private ImageService imageService;

	private static final Logger logger = LoggerFactory.getLogger(MjpegStreamController.class);

	@GetMapping(path = "/", produces = "multipart/x-mixed-replace;boundary=" + BOUNDARY)
	public StreamingResponseBody getMjpegStream() {

		return new StreamingResponseBody() {

			@Override
			public void writeTo(OutputStream outputStream) throws IOException {
				// Grab the first image from the stream and ditch it, because it will have an
				// old timestamp.
				try {
					imageService.getNextImage();
				} catch (InterruptedException ex) {
					logger.error("Interrupted Exception", ex);
				}

				try {
					while (true) {
						try {
							byte[] image = imageService.getNextImage();
							outputStream.write((HEAD + image.length + NL + NL).getBytes());
							outputStream.write(image);
						} catch (InterruptedException ex) {
							logger.error("Interrupted Exception", ex);
						}
					}
				} catch (Exception ex) {
					logger.error("Exception when writing output stream", ex);
				}
			}
		};
	}
}
