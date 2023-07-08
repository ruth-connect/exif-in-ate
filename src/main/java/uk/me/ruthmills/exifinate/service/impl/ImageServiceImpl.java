package uk.me.ruthmills.exifinate.service.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.springframework.stereotype.Service;

import uk.me.ruthmills.exifinate.service.ImageService;

@Service
public class ImageServiceImpl implements ImageService {

	private BlockingQueue<byte[]> imageQueue = new ArrayBlockingQueue<>(1);

	/**
	 * Gets the next image from the MJPEG stream. Blocks until the next image is
	 * available.
	 * 
	 * @return The next image from the MJPEG stream.
	 * @throws InterruptedException Thrown if the thread was interrupted.
	 */
	@Override
	public byte[] getNextImage() throws InterruptedException {
		return imageQueue.take();
	}

	/**
	 * Sets the next image from the MJPEG stream.
	 * 
	 * @param image The next image from the MJPEG stream.
	 */
	@Override
	public void setNextImage(byte[] image) {
		imageQueue.offer(image);
	}
}
