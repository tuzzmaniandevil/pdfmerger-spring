package co.tuzza.pdfmerger;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author dylan
 */
@Service
public class PdfMergingService {

    public ByteArrayOutputStream mergeFilesToPdf(MultipartFile[] files, PdfSettings settings) throws IOException, ImageProcessingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationStream(baos);

        PdfAdjustBean[] fileSettings = settings != null ? settings.getFiles() : null;

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            PdfAdjustBean fileSetting = (fileSettings != null && fileSettings.length - 1 >= i) ? fileSettings[i] : null;

            if (isPdf(file)) {
                if (fileSetting != null) {
                    byte[] bytes = rotatePdf(file, fileSetting);

                    ByteArrayInputStream in = new ByteArrayInputStream(bytes);

                    merger.addSource(in);
                } else {
                    merger.addSource(file.getInputStream());
                }
            } else if (isJpg(file)) {
                BufferedImage image = convertJpg(file);
                byte[] bs = convertImage(image);
                merger.addSource(new ByteArrayInputStream(bs));
            } else if (isImage(file)) {
                byte[] bytes = convertImage(file);
                merger.addSource(new ByteArrayInputStream(bytes));
            }
        }

        merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());

        return baos;
    }

    public ByteArrayOutputStream compressPdf(byte[] pdfBytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PDDocument doc = PDDocument.load(pdfBytes);

        if (doc != null) {
            for (PDPage page : doc.getPages()) {
                scanResources(page.getResources(), doc);
            }
            doc.save(baos);

            try {
                doc.close();
            } catch (Throwable t) {
            }
        }

        return baos;
    }

    public ByteArrayOutputStream splitPdf(MultipartFile file, Integer startPage, Integer endPage, PdfController.ExportType exportType) throws IOException {
        if (isPdf(file)) {
            Splitter splitter = new Splitter();

            splitter.setMemoryUsageSetting(MemoryUsageSetting.setupTempFileOnly());

            PDDocument pdd = PDDocument.load(file.getInputStream());

            if (startPage != null && startPage >= 1) {
                splitter.setStartPage(startPage);
            }

            if (endPage != null && endPage >= 1) {
                splitter.setEndPage(endPage);
            }

            List<PDDocument> splitDocs = splitter.split(pdd);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            switch (exportType) {
                case SinglePDF: {
                    PDFMergerUtility merger = new PDFMergerUtility();
                    merger.setDestinationStream(baos);

                    for (int i = 0; i < splitDocs.size(); i++) {
                        PDDocument splitDoc = splitDocs.get(i);

                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        splitDoc.save(output);

                        merger.addSource(new ByteArrayInputStream(output.toByteArray()));
                    }

                    merger.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());

                    break;
                }
                case SeperateJPEG: {
                    try (ZipOutputStream zos = new ZipOutputStream(baos)) {

                        for (int i = 0; i < splitDocs.size(); i++) {
                            int pageNum = i + 1;
                            PDDocument splitDoc = splitDocs.get(i);

                            List<PdfThumnail> jpgs = generatePdfThumbnails(splitDoc, 300, "jpg");

                            if (jpgs != null && jpgs.size() > 0) {
                                ZipEntry zipEntry = new ZipEntry("page-" + pageNum + ".jpg");
                                zos.putNextEntry(zipEntry);

                                PdfThumnail jpg = jpgs.iterator().next();

                                zos.write(jpg.imageBytes());
                                zos.closeEntry();
                            }

                            try {
                                splitDoc.close();
                            } catch (Exception ex) {
                            }
                        }

                        zos.flush();
                        zos.close();
                    }

                    break;
                }
                case SeperatePDF:
                default: {
                    try (ZipOutputStream zos = new ZipOutputStream(baos)) {

                        for (int i = 0; i < splitDocs.size(); i++) {
                            int pageNum = i + 1;
                            PDDocument splitDoc = splitDocs.get(i);

                            ZipEntry zipEntry = new ZipEntry("page-" + pageNum + ".pdf");
                            zos.putNextEntry(zipEntry);

                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            splitDoc.save(output);
                            zos.write(output.toByteArray());
                            zos.closeEntry();
                        }

                        zos.flush();
                        zos.close();
                    }

                    break;
                }
            }

            return baos;
        }

        return null;
    }

    public List<PdfThumnail> generatePdfThumbnails(PDDocument pdf, int dpi, String imageType) throws IOException {
        List<PdfThumnail> thumbnails = new ArrayList<>();

        int pageCount = pdf.getNumberOfPages();
        PDFRenderer r = new PDFRenderer(pdf);

        for (int i = 0; i < pageCount; i++) {
            BufferedImage bim = r.renderImageWithDPI(i, dpi, ImageType.RGB);

            final Iterator<ImageWriter> jpgWriters
                    = ImageIO.getImageWritersByFormatName("jpeg");
            final ImageWriter jpgWriter = jpgWriters.next();
            final ImageWriteParam iwp = jpgWriter.getDefaultWriteParam();

            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwp.setCompressionQuality(0.3f);

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            OutputStream b64 = Base64.getEncoder().wrap(out);

            jpgWriter.setOutput(ImageIO.createImageOutputStream(b64));
            jpgWriter.write(null, new IIOImage(bim, null, null), iwp);

            //ImageIOUtil.writeImage(bim, imageType, b64);
            String encodedImg = out.toString("UTF-8");

            PdfThumnail t = new PdfThumnail(pageCount, encodedImg, imageType);

            thumbnails.add(t);
        }

        pdf.close();

        return thumbnails;
    }

    private void scanResources(final PDResources rList, final PDDocument doc)
            throws FileNotFoundException, IOException {

        for (COSName xObjectName : rList.getXObjectNames()) {
            final PDXObject xObj = rList.getXObject(xObjectName);

            if (xObj instanceof PDFormXObject) {
                scanResources(((PDFormXObject) xObj).getResources(), doc);
            }
            if (!(xObj instanceof PDImageXObject)) {
                continue;
            }
            PDImageXObject img = (PDImageXObject) xObj;

            final Iterator<ImageWriter> jpgWriters
                    = ImageIO.getImageWritersByFormatName("jpeg");
            final ImageWriter jpgWriter = jpgWriters.next();
            final ImageWriteParam iwp = jpgWriter.getDefaultWriteParam();
            iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwp.setCompressionQuality(0.40f);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            jpgWriter.setOutput(ImageIO.createImageOutputStream(baos));
            jpgWriter.write(null, new IIOImage(img.getImage(), null, null), iwp);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

            PDImageXObject jpg = JPEGFactory.createFromStream(doc, bais);

            rList.put(xObjectName, jpg);
        }
    }

    private byte[] rotatePdf(MultipartFile file, PdfAdjustBean fileSetting) throws IOException {
        PDDocument doc = PDDocument.load(file.getInputStream());
        List<PdfPageAdjustBean> pages = fileSetting.getPages();

        int pageCount = doc.getNumberOfPages();
        for (int i = 0; i < pageCount; i++) {
            PDPage page = doc.getPage(i);
            PdfPageAdjustBean pageSettings = pages.get(i);

            if (pageSettings != null && pageSettings.getRotate() != 0) {
                int rotate = pageSettings.getRotate();
                if (rotate < 0) {
                    rotate = rotate + 360;
                }

                // Flip it
                rotate = rotate - 360;
                if (rotate < 0) {
                    rotate = rotate * -1;
                }

                PDPageContentStream cs = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.PREPEND, false, false);
                Matrix matrix = Matrix.getRotateInstance(Math.toRadians(rotate), 0, 0);
                cs.transform(matrix);
                cs.close();

                PDRectangle cropBox = page.getCropBox();
                Rectangle rectangle = cropBox.transform(matrix).getBounds();
                PDRectangle newBox = new PDRectangle((float) rectangle.getX(), (float) rectangle.getY(), (float) rectangle.getWidth(), (float) rectangle.getHeight());
                page.setCropBox(newBox);
                page.setMediaBox(newBox);
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        doc.close();

        return out.toByteArray();
    }

    public boolean isPdf(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String ext = FilenameUtils.getExtension(originalFilename);

        return StringUtils.equalsIgnoreCase(ext, "pdf") || StringUtils.containsIgnoreCase(contentType, "/pdf");
    }

    private boolean isJpg(MultipartFile file) {
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        String ext = FilenameUtils.getExtension(originalFilename);

        return StringUtils.equalsIgnoreCase(ext, "jpg")
                || StringUtils.equalsIgnoreCase(ext, "jpeg")
                || StringUtils.containsIgnoreCase(contentType, "image/jpeg")
                || StringUtils.containsIgnoreCase(contentType, "image/jpg");
    }

    private boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return StringUtils.startsWithIgnoreCase(contentType, "image/") && !StringUtils.startsWithIgnoreCase(contentType, "image/svg");
    }

    private byte[] convertImage(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());

        return convertImage(image);
    }

    private byte[] convertImage(BufferedImage bufferedImage) throws IOException {
        ByteArrayOutputStream out;
        try (PDDocument pdd = new PDDocument()) {
            PDImageXObject pdImage = LosslessFactory.createFromImage(pdd, bufferedImage);
            PDRectangle size = new PDRectangle(pdImage.getWidth(), pdImage.getHeight());
            PDPage page = new PDPage(size);
            pdd.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(pdd, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.drawImage(pdImage, 0, 0);
                contentStream.close();
            }
            out = new ByteArrayOutputStream();
            pdd.save(out);

            pdd.close();
        }

        return out.toByteArray();
    }

    private BufferedImage convertJpg(MultipartFile file) throws IOException, ImageProcessingException {
        byte[] bytes = file.getBytes();

        ByteArrayInputStream in = new ByteArrayInputStream(bytes);

        Metadata metadata = ImageMetadataReader.readMetadata(in);
        in.reset();
        BufferedImage image = ImageIO.read(in);

        if (metadata.containsDirectoryOfType(ExifIFD0Directory.class)) {
            Collection<ExifIFD0Directory> dirs = metadata.getDirectoriesOfType(ExifIFD0Directory.class);
            for (ExifIFD0Directory dir : dirs) {
                if (dir != null) {
                    Integer orientation = dir.getInteger(ExifIFD0Directory.TAG_ORIENTATION);

                    if (orientation != null) {
                        return getRotatedImage(orientation, image);
                    }
                }
            }
        }

        return image;
    }

    private BufferedImage getRotatedImage(int orientation, BufferedImage image) throws IOException {
        AffineTransform t = new AffineTransform();

        int width = image.getWidth();
        int height = image.getHeight();

        int newWidth = width;
        int newHeight = height;

        switch (orientation) {
            case 1:
                break;
            case 2: // Flip X
                t.scale(-1.0, 1.0);
                t.translate(-width, 0);
                break;
            case 3: // PI rotation
                t.translate(width, height);
                t.rotate(Math.PI);
                break;
            case 4: // Flip Y
                t.scale(1.0, -1.0);
                t.translate(0, -height);
                break;
            case 5: // - PI/2 and Flip X
                t.rotate(-Math.PI / 2);
                t.scale(-1.0, 1.0);
                break;
            case 6: // -PI/2 and -width
                t.translate(height, 0);
                t.rotate(Math.PI / 2);
                newWidth = height;
                newHeight = width;
                break;
            case 7: // PI/2 and Flip
                t.scale(-1.0, 1.0);
                t.translate(-height, 0);
                t.translate(0, width);
                t.rotate(3 * Math.PI / 2);
                break;
            case 8: // PI / 2
                t.translate(0, width);
                t.rotate(3 * Math.PI / 2);
                newWidth = height;
                newHeight = width;
                break;
        }

        BufferedImage rot = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = rot.createGraphics();
        g.drawImage(image, t, null);
        g.dispose();

        return rot;
    }
}
