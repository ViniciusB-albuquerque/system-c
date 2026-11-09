import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class provides functionality to apply a mean filter to an image
 * using multiple threads.
 */
public class ImageMeanFilter {

    /**
     * Applies mean filter to an image using multiple threads.
     *
     * @param inputPath  Path to input image
     * @param outputPath Path to output image
     * @param kernelSize Size of mean kernel
     * @param numThreads Number of threads to use
     * @throws IOException If there is an error reading/writing
     */
    public static ImageWorker[] applyMeanFilter(
            String inputPath,
            String outputPath,
            int kernelSize,
            int numThreads
    ) throws IOException {

        BufferedImage originalImage = ImageIO.read(new File(inputPath));

        BufferedImage filteredImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        int height = originalImage.getHeight();

        Thread[] threads = new Thread[numThreads];
        ImageWorker[] workers = new ImageWorker[numThreads];

        int base = height / numThreads;
        int rest = height % numThreads;

        int startY = 0;

        for (int i = 0; i < numThreads; i++) {
            int endY = startY + base;
            if (i < rest) {
                endY++;
            }

            workers[i] = new ImageWorker(
                    originalImage,
                    filteredImage,
                    kernelSize,
                    startY,
                    endY
            );

            threads[i] = new Thread(workers[i]);
            threads[i].start();

            startY = endY;
        }

        for (int i = 0; i < numThreads; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread interrupted: " + e.getMessage());
            }
        }

        ImageIO.write(filteredImage, "jpg", new File(outputPath));

        return workers;
    }

    private static class ImageWorker implements Runnable {
        private final BufferedImage originalImage;
        private final BufferedImage filteredImage;
        private final int kernelSize;
        private final int startY;
        private final int endY;
        private int alterados = 0;
        private int iguais = 0;

        public ImageWorker(
                BufferedImage originalImage,
                BufferedImage filteredImage,
                int kernelSize,
                int startY,
                int endY
        ) {
            this.originalImage = originalImage;
            this.filteredImage = filteredImage;
            this.kernelSize = kernelSize;
            this.startY = startY;
            this.endY = endY;
        }

        @Override
        public void run() {
            int width = originalImage.getWidth();

            for (int y = startY; y < endY; y++) {
                for (int x = 0; x < width; x++) {
                    int[] avgColor = calculateNeighborhoodAverage(
                            originalImage,
                            x,
                            y,
                            kernelSize
                    );

                    int rgb =
                            (avgColor[0] << 16) |
                            (avgColor[1] << 8) |
                            avgColor[2];

                    filteredImage.setRGB(x, y, rgb);

                    if ((originalImage.getRGB(x, y) & 0xFFFFFF) == rgb) {
                        iguais++;
                    } else {
                        alterados++;
                    }
                }
            }
        }
    }

    /**
     * Calculates average colors in a pixel's neighborhood.
     *
     * @param image      Source image
     * @param centerX    X coordinate of center pixel
     * @param centerY    Y coordinate of center pixel
     * @param kernelSize Kernel size
     * @return Array with R, G, B averages
     */
    private static int[] calculateNeighborhoodAverage(
            BufferedImage image,
            int centerX,
            int centerY,
            int kernelSize
    ) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pad = kernelSize / 2;

        long redSum = 0;
        long greenSum = 0;
        long blueSum = 0;
        int pixelCount = 0;

        for (int dy = -pad; dy <= pad; dy++) {
            for (int dx = -pad; dx <= pad; dx++) {
                int x = centerX + dx;
                int y = centerY + dy;

                if (x >= 0 && x < width && y >= 0 && y < height) {
                    int rgb = image.getRGB(x, y);

                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;

                    redSum += red;
                    greenSum += green;
                    blueSum += blue;
                    pixelCount++;
                }
            }
        }

        return new int[]{
                (int) (redSum / pixelCount),
                (int) (greenSum / pixelCount),
                (int) (blueSum / pixelCount)
        };
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ImageMeanFilter <input_file> <num_threads>");
            System.exit(1);
        }

        String inputFile = args[0];
        int numThreads = Integer.parseInt(args[1]);

        if (numThreads < 2) {
            System.err.println("Number of threads must be at least 2.");
            System.exit(1);
        }

        try {
            ImageWorker[] workers = applyMeanFilter(inputFile, "filtered_output.jpg", 7, numThreads);

            long alterados = 0;
            long iguais = 0;
            for (ImageWorker w : workers) {
                alterados += w.alterados;
                iguais += w.iguais;
            }

            System.out.println("Pixels alterados: " + alterados);
            System.out.println("Pixels iguais: " + iguais);
        } catch (IOException e) {
            System.err.println("Error processing image: " + e.getMessage());
        }
    }
}
