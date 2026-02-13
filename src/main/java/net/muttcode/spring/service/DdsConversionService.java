package net.muttcode.spring.service;

import net.muttcode.spring.model.ProcessedFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class DdsConversionService {
    
    private static final Logger logger = Logger.getLogger(DdsConversionService.class.getName());
    
    private final Path tempPath;
    private final Path outputPath;
    
    private static final int DDS_MAGIC = 0x20534444;
    private static final int DDPF_ALPHAPIXELS = 0x1;
    private static final int DDPF_FOURCC = 0x4;
    private static final int DDPF_RGB = 0x40;
    private static final int FOURCC_DXT1 = 0x31545844;
    private static final int FOURCC_DXT3 = 0x33545844;
    private static final int FOURCC_DXT5 = 0x35545844;
    
    public DdsConversionService(
        @Value("${image.processing.temp.path:/app/temp}") String tempPathStr,
        @Value("${image.processing.output.path:/app/processed}") String outputPathStr
    ) throws IOException {
        this.tempPath = Path.of(tempPathStr);
        this.outputPath = Path.of(outputPathStr);
        Files.createDirectories(this.tempPath);
        Files.createDirectories(this.outputPath);
        logger.info("DDS Conversion Service initialized");
    }
    
    public ProcessedFile ddsToPng(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".dds")) {
            throw new IllegalArgumentException("File must be a DDS file");
        }
        
        String fileId = UUID.randomUUID().toString();
        Path inputPath = tempPath.resolve(fileId + "_input.dds");
        file.transferTo(inputPath.toFile());
        
        try {
            logger.info("Converting DDS to PNG: " + filename);
            BufferedImage image = readDDS(inputPath);
            
            String outputFileName = fileId + "_" + 
                filename.substring(0, filename.lastIndexOf('.')) + ".png";
            Path outputFilePath = outputPath.resolve(outputFileName);
            
            if (!ImageIO.write(image, "PNG", outputFilePath.toFile())) {
                throw new IOException("Failed to write PNG file");
            }
            
            logger.info("DDS converted successfully: " + outputFileName);
            return new ProcessedFile(fileId, outputFileName, outputFilePath, "image/png");
            
        } finally {
            Files.deleteIfExists(inputPath);
        }
    }
    
    public ProcessedFile imageToDds(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Filename is required");
        }
        
        String lower = filename.toLowerCase();
        if (!lower.endsWith(".png") && !lower.endsWith(".jpg") && !lower.endsWith(".jpeg")) {
            throw new IllegalArgumentException("File must be PNG or JPG");
        }
        
        String fileId = UUID.randomUUID().toString();
        Path inputPath = tempPath.resolve(fileId + "_input" + getExtension(filename));
        file.transferTo(inputPath.toFile());
        
        try {
            logger.info("Converting image to DDS: " + filename);
            BufferedImage image = ImageIO.read(inputPath.toFile());
            if (image == null) {
                throw new IOException("Failed to read image file");
            }
            
            BufferedImage argbImage = convertToARGB(image);
            String outputFileName = fileId + "_" + 
                filename.substring(0, filename.lastIndexOf('.')) + ".dds";
            Path outputFilePath = outputPath.resolve(outputFileName);
            
            writeDDS(argbImage, outputFilePath);
            logger.info("Image converted to DDS successfully: " + outputFileName);
            
            return new ProcessedFile(fileId, outputFileName, outputFilePath, "image/vnd.ms-dds");
            
        } finally {
            Files.deleteIfExists(inputPath);
        }
    }
    
    private BufferedImage readDDS(Path ddsFile) throws IOException {
        try (DataInputStream in = new DataInputStream(
            new BufferedInputStream(Files.newInputStream(ddsFile)))) {
            
            int magic = Integer.reverseBytes(in.readInt());
            if (magic != DDS_MAGIC) {
                throw new IOException("Not a valid DDS file");
            }
            
            DdsHeader header = readDdsHeader(in);
            logger.info(String.format("DDS: %dx%d, %s", 
                header.width, header.height, header.isCompressed ? "compressed" : "uncompressed"));
            
            BufferedImage image = new BufferedImage(
                header.width, header.height, BufferedImage.TYPE_INT_ARGB);
            
            if (header.isCompressed) {
                readCompressedDds(in, image, header);
            } else {
                readUncompressedDds(in, image, header);
            }
            return image;
        }
    }
    
    private DdsHeader readDdsHeader(DataInputStream in) throws IOException {
        DdsHeader h = new DdsHeader();
        h.size = Integer.reverseBytes(in.readInt());
        h.flags = Integer.reverseBytes(in.readInt());
        h.height = Integer.reverseBytes(in.readInt());
        h.width = Integer.reverseBytes(in.readInt());
        h.pitchOrLinearSize = Integer.reverseBytes(in.readInt());
        h.depth = Integer.reverseBytes(in.readInt());
        h.mipMapCount = Integer.reverseBytes(in.readInt());
        in.skipBytes(44);
        
        h.pfSize = Integer.reverseBytes(in.readInt());
        h.pfFlags = Integer.reverseBytes(in.readInt());
        h.pfFourCC = Integer.reverseBytes(in.readInt());
        h.pfRGBBitCount = Integer.reverseBytes(in.readInt());
        h.pfRBitMask = Integer.reverseBytes(in.readInt());
        h.pfGBitMask = Integer.reverseBytes(in.readInt());
        h.pfBBitMask = Integer.reverseBytes(in.readInt());
        h.pfABitMask = Integer.reverseBytes(in.readInt());
        in.skipBytes(20);
        
        h.isCompressed = (h.pfFlags & DDPF_FOURCC) != 0;
        return h;
    }
    
    private void readUncompressedDds(DataInputStream in, BufferedImage image, DdsHeader header) throws IOException {
        int bytesPerPixel = header.pfRGBBitCount / 8;
        byte[] rowData = new byte[header.width * bytesPerPixel];
        
        for (int y = 0; y < header.height; y++) {
            in.readFully(rowData);
            for (int x = 0; x < header.width; x++) {
                int offset = x * bytesPerPixel;
                int b = rowData[offset] & 0xFF;
                int g = rowData[offset + 1] & 0xFF;
                int r = rowData[offset + 2] & 0xFF;
                int a = bytesPerPixel == 4 ? (rowData[offset + 3] & 0xFF) : 255;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                image.setRGB(x, y, argb);
            }
        }
    }
    
    private void readCompressedDds(DataInputStream in, BufferedImage image, DdsHeader header) throws IOException {
        int blockSize = (header.pfFourCC == FOURCC_DXT1) ? 8 : 16;
        int blocksWide = (header.width + 3) / 4;
        int blocksHigh = (header.height + 3) / 4;
        byte[] blockData = new byte[blockSize];
        
        for (int by = 0; by < blocksHigh; by++) {
            for (int bx = 0; bx < blocksWide; bx++) {
                in.readFully(blockData);
                if (header.pfFourCC == FOURCC_DXT1) {
                    decompressDXT1Block(blockData, image, bx * 4, by * 4);
                } else if (header.pfFourCC == FOURCC_DXT5) {
                    decompressDXT5Block(blockData, image, bx * 4, by * 4);
                } else {
                    decompressDXT1Block(blockData, image, bx * 4, by * 4);
                }
            }
        }
    }
    
    private void decompressDXT1Block(byte[] blockData, BufferedImage image, int startX, int startY) {
        ByteBuffer buffer = ByteBuffer.wrap(blockData).order(ByteOrder.LITTLE_ENDIAN);
        int color0 = buffer.getShort() & 0xFFFF;
        int color1 = buffer.getShort() & 0xFFFF;
        int indices = buffer.getInt();
        
        int[] colors = new int[4];
        colors[0] = rgb565ToArgb(color0);
        colors[1] = rgb565ToArgb(color1);
        
        if (color0 > color1) {
            colors[2] = interpolateColor(colors[0], colors[1], 1, 2);
            colors[3] = interpolateColor(colors[0], colors[1], 2, 2);
        } else {
            colors[2] = interpolateColor(colors[0], colors[1], 1, 1);
            colors[3] = 0x00000000;
        }
        
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                int index = (indices >> ((y * 4 + x) * 2)) & 0x3;
                int px = startX + x, py = startY + y;
                if (px < image.getWidth() && py < image.getHeight()) {
                    image.setRGB(px, py, colors[index]);
                }
            }
        }
    }
    
    private void decompressDXT5Block(byte[] blockData, BufferedImage image, int startX, int startY) {
        ByteBuffer buffer = ByteBuffer.wrap(blockData).order(ByteOrder.LITTLE_ENDIAN);
        int alpha0 = buffer.get() & 0xFF;
        int alpha1 = buffer.get() & 0xFF;
        long alphaBits = 0;
        for (int i = 0; i < 6; i++) alphaBits |= (long)(buffer.get() & 0xFF) << (i * 8);
        
        int color0 = buffer.getShort() & 0xFFFF;
        int color1 = buffer.getShort() & 0xFFFF;
        int indices = buffer.getInt();
        
        int[] colors = new int[4];
        colors[0] = rgb565ToArgb(color0) & 0x00FFFFFF;
        colors[1] = rgb565ToArgb(color1) & 0x00FFFFFF;
        colors[2] = interpolateColor(colors[0], colors[1], 1, 2) & 0x00FFFFFF;
        colors[3] = interpolateColor(colors[0], colors[1], 2, 2) & 0x00FFFFFF;
        
        int[] alphas = new int[8];
        alphas[0] = alpha0; alphas[1] = alpha1;
        if (alpha0 > alpha1) {
            for (int i = 2; i < 8; i++) alphas[i] = ((8 - i) * alpha0 + (i - 1) * alpha1) / 7;
        } else {
            for (int i = 2; i < 6; i++) alphas[i] = ((6 - i) * alpha0 + (i - 1) * alpha1) / 5;
            alphas[6] = 0; alphas[7] = 255;
        }
        
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                int pixelIndex = y * 4 + x;
                int colorIndex = (indices >> (pixelIndex * 2)) & 0x3;
                int alphaIndex = (int)((alphaBits >> (pixelIndex * 3)) & 0x7);
                int px = startX + x, py = startY + y;
                if (px < image.getWidth() && py < image.getHeight()) {
                    image.setRGB(px, py, (alphas[alphaIndex] << 24) | colors[colorIndex]);
                }
            }
        }
    }
    
    private int rgb565ToArgb(int rgb565) {
        int r = ((rgb565 >> 11) & 0x1F) * 255 / 31;
        int g = ((rgb565 >> 5) & 0x3F) * 255 / 63;
        int b = (rgb565 & 0x1F) * 255 / 31;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    
    private int interpolateColor(int c1, int c2, int num, int denom) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        return ((r1 * (denom - num) + r2 * num) / denom << 16) |
               ((g1 * (denom - num) + g2 * num) / denom << 8) |
               (b1 * (denom - num) + b2 * num) / denom;
    }
    
    private void writeDDS(BufferedImage image, Path outputPath) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(Files.newOutputStream(outputPath)))) {
            out.writeInt(Integer.reverseBytes(DDS_MAGIC));
            writeDdsHeader(out, image);
            writePixelData(out, image);
        }
    }
    
    private void writeDdsHeader(DataOutputStream out, BufferedImage image) throws IOException {
        int w = image.getWidth(), h = image.getHeight();
        out.writeInt(Integer.reverseBytes(124));
        out.writeInt(Integer.reverseBytes(0x1 | 0x2 | 0x4 | 0x1000 | 0x8));
        out.writeInt(Integer.reverseBytes(h));
        out.writeInt(Integer.reverseBytes(w));
        out.writeInt(Integer.reverseBytes(w * 4));
        out.writeInt(0); out.writeInt(0);
        for (int i = 0; i < 11; i++) out.writeInt(0);
        out.writeInt(Integer.reverseBytes(32));
        out.writeInt(Integer.reverseBytes(DDPF_RGB | DDPF_ALPHAPIXELS));
        out.writeInt(0);
        out.writeInt(Integer.reverseBytes(32));
        out.writeInt(Integer.reverseBytes(0x00FF0000));
        out.writeInt(Integer.reverseBytes(0x0000FF00));
        out.writeInt(Integer.reverseBytes(0x000000FF));
        out.writeInt(Integer.reverseBytes(0xFF000000));
        out.writeInt(Integer.reverseBytes(0x1000));
        out.writeInt(0); out.writeInt(0); out.writeInt(0);
        out.writeInt(0);
    }
    
    private void writePixelData(DataOutputStream out, BufferedImage image) throws IOException {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                out.writeByte(argb & 0xFF);
                out.writeByte((argb >> 8) & 0xFF);
                out.writeByte((argb >> 16) & 0xFF);
                out.writeByte((argb >> 24) & 0xFF);
            }
        }
    }
    
    private BufferedImage convertToARGB(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) return source;
        BufferedImage argb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        argb.getGraphics().drawImage(source, 0, 0, null);
        return argb;
    }
    
    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot) : "";
    }
    
    public Path getOutputPath() { return outputPath; }
    
    private static class DdsHeader {
        int size, flags, height, width, pitchOrLinearSize, depth, mipMapCount;
        int pfSize, pfFlags, pfFourCC, pfRGBBitCount;
        int pfRBitMask, pfGBitMask, pfBBitMask, pfABitMask;
        boolean isCompressed;
    }
}
