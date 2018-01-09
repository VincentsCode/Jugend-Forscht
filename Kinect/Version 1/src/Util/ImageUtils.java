package Util;

import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class ImageUtils {

	public static void showImage(BufferedImage image) {
		JFrame frame = new JFrame();
		frame.setLayout(new FlowLayout());
		frame.setTitle("Images");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setBounds(0, 0, image.getWidth(), image.getHeight());
		ImageIcon icon = new ImageIcon(image);
		JLabel label = new JLabel();
		label.setIcon(icon);
		frame.add(label);
		frame.setVisible(true);
	}

	public static void showImages(BufferedImage[] images) {
		JFrame frame = new JFrame();
		int w = 0;
		int h = 0;
		frame.setLayout(new FlowLayout());
		frame.setTitle("Images");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		for (int i = 0; i < images.length; i++) {
			ImageIcon icon = new ImageIcon(images[i]);
			JLabel label = new JLabel();
			label.setIcon(icon);
			frame.add(label);
			w += images[i].getWidth();
			h += images[i].getHeight();
		}
		frame.setBounds(0, 0, w, h);
		frame.setVisible(true);
	}
	
	public static void saveImage(BufferedImage image, String file) {
	    try {
		    File outputfile = new File(file);
			ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static BufferedImage resize(BufferedImage imageToScale, int width, int height) {
		BufferedImage scaledImage = null;
		if (imageToScale != null) {
			scaledImage = new BufferedImage(width, height, imageToScale.getType());
			Graphics2D graphics2D = scaledImage.createGraphics();
			graphics2D.drawImage(imageToScale, 0, 0, width, height, null);
			graphics2D.dispose();
		}
		return scaledImage;
	}

	public static BufferedImage matToImage(Mat mat) {
		int type = 0;
		if (mat.channels() == 1) {
			type = BufferedImage.TYPE_BYTE_GRAY;
		} else if (mat.channels() == 3) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
		WritableRaster raster = image.getRaster();
		DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
		byte[] data = dataBuffer.getData();
		mat.get(0, 0, data);

		return image;
	}	
	
	public static BufferedImage[] matsToImages(Mat[] mats) {
		BufferedImage[] result = new BufferedImage[mats.length];
		for (int i = 0; i < mats.length; i++) {
			result[i] = matToImage(mats[i]);
		}
		return result;
	}

	public static Mat imageToMat(BufferedImage in) {
		Mat out;
		byte[] data;
		int r, g, b;
		int height = in.getHeight();
		int width = in.getWidth();
		if (in.getType() == BufferedImage.TYPE_INT_RGB || in.getType() == BufferedImage.TYPE_INT_ARGB) {
			out = new Mat(height, width, CvType.CV_8UC3);
			data = new byte[height * width * (int) out.elemSize()];
			int[] dataBuff = in.getRGB(0, 0, width, height, null, 0, width);
			for (int i = 0; i < dataBuff.length; i++) {
				data[i * 3 + 2] = (byte) ((dataBuff[i] >> 16) & 0xFF);
				data[i * 3 + 1] = (byte) ((dataBuff[i] >> 8) & 0xFF);
				data[i * 3] = (byte) ((dataBuff[i] >> 0) & 0xFF);
			}
		} else {
			out = new Mat(height, width, CvType.CV_8UC1);
			data = new byte[height * width * (int) out.elemSize()];
			int[] dataBuff = in.getRGB(0, 0, width, height, null, 0, width);
			for (int i = 0; i < dataBuff.length; i++) {
				r = (byte) ((dataBuff[i] >> 16) & 0xFF);
				g = (byte) ((dataBuff[i] >> 8) & 0xFF);
				b = (byte) ((dataBuff[i] >> 0) & 0xFF);
				data[i] = (byte) ((0.21 * r) + (0.71 * g) + (0.07 * b)); // luminosity
			}
		}
		out.put(0, 0, data);
		return out;
	}

	public static BufferedImage createMask(BufferedImage image, BufferedImage mask) {
		GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration();

		BufferedImage result = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null),
				Transparency.BITMASK);
		BufferedImage temp = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null),
				Transparency.BITMASK);

		WritableRaster raster = result.getRaster();
		Raster maskData = mask.getRaster();
		Raster tileData = image.getRaster();

		Graphics g;

		int[] pixel = new int[4];
		int width = image.getWidth(null);
		int height = image.getHeight(null);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				pixel = maskData.getPixel(x, y, pixel);

				if (pixel[0] == 0) {
					tileData.getPixel(x, y, pixel);
					pixel[3] = 255;
					raster.setPixel(x, y, pixel);

					pixel = tileData.getPixel(x, y, pixel);
				}
			}
		}

		result.setData(raster);

		g = temp.createGraphics();
		g.drawImage(result, 0, 0, null);
		g.dispose();

		return temp;
	}
}
