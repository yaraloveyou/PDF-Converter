package org.converter.service;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.converter.entity.FileEntity;
import org.converter.repository.FileRepository;
import org.converter.utils.FileActionConstants;
import org.converter.utils.FileExtractor;
import org.converter.utils.PageExtract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Primary
public class FileServiceImpl implements FileService {
    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);
    private final FileRepository fileRepository;

    @Autowired
    public FileServiceImpl(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    public List<FileEntity> getAllFiles() {
        return fileRepository.findAll();
    }

    public FileEntity saveFile(FileEntity file) {
        return fileRepository.save(file);
    }

    public FileEntity getFileById(Long id) {
        return fileRepository.findById(id).orElse(null);
    }

    public void deleteFile(Long id) {
        fileRepository.deleteById(id);
    }

    public byte[] convertPdfToText(MultipartFile file) throws IOException {
        byte[] fileContentBytes = IOUtils.toByteArray(file.getInputStream());
        String fileFormat = FileExtractor.extractFormat(file);
        String filename = FileExtractor.extractName(file);

        FileEntity fileEntity = new FileEntity(filename, fileFormat, FileActionConstants.ACTION_CONVERT_PFD_TO_TEXT,
                fileContentBytes, file.getSize(), LocalDateTime.now());

        fileRepository.save(fileEntity);

        PDDocument document = PDDocument.load(file.getInputStream());
        PDFTextStripper textStripper = new PDFTextStripper();
        String text = textStripper.getText(document);
        document.close();

        return text.getBytes();
    }

    public byte[] mergePdf(List<MultipartFile> pdfs) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();

        for (MultipartFile pdf : pdfs) {
            merger.addSource(pdf.getInputStream());
            String filename = FileExtractor.extractName(pdf);
            String fileFormat = FileExtractor.extractFormat(pdf);
            FileEntity fileEntity = new FileEntity(filename, fileFormat, FileActionConstants.ACTION_MERGE_PDF,
                    pdf.getBytes(), pdf.getSize(), LocalDateTime.now());

            fileRepository.save(fileEntity);
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            merger.setDestinationStream(outputStream);
            merger.mergeDocuments();

            return outputStream.toByteArray();
        }
    }

    public int getPageCount(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            return document.getNumberOfPages();
        }
    }

    public byte[] separationPdf(MultipartFile file, String parts) throws IOException {
        File tempDirectory = FileUtils.getTempDirectory();
        File outputZipFile = new File(tempDirectory, "split.zip");
        String filename = FileExtractor.extractName(file);
        String fileFormat = FileExtractor.extractFormat(file);
        FileEntity fileEntity = new FileEntity(filename, fileFormat, FileActionConstants.ACTION_SEPARATION_DPF + parts,
                file.getBytes(), file.getSize(), LocalDateTime.now());

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(FileUtils.openOutputStream(outputZipFile));
            PDDocument document = PDDocument.load(file.getInputStream())) {
            for (int pageNumber = 0; pageNumber < document.getNumberOfPages(); ++pageNumber) {
                if (!PageExtract.isExtract(parts, pageNumber + 1))
                    continue;
                PDDocument singlePageDocument = new PDDocument();
                singlePageDocument.addPage((PDPage) document.getDocumentCatalog().getPages().get(pageNumber));
                filename = file.getName() + "_" + (pageNumber + 1) + ".pdf";
                File outputFile = new File(tempDirectory, filename);
                singlePageDocument.save(outputFile);

                ZipEntry zipEntry = new ZipEntry(filename);
                zipOutputStream.putNextEntry(zipEntry);
                FileUtils.copyFile(outputFile, zipOutputStream);
                zipOutputStream.closeEntry();

                singlePageDocument.close();

                FileUtils.deleteQuietly(outputFile);
            }
        }

        fileRepository.save(fileEntity);
        return FileUtils.readFileToByteArray(outputZipFile);
    }

    // Обуздать причину большого веса
    public byte[] compressionPdf(MultipartFile file, float quality) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream());
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                for (COSName xObjectName : resources.getXObjectNames()) {
                    PDXObject xObject = resources.getXObject(xObjectName);
                    if (xObject instanceof PDImageXObject image) {
                        BufferedImage bufferedImage = image.getImage();
                        BufferedImage resizedImage = resizeImage(bufferedImage, quality);

                        PDImageXObject pdImage = LosslessFactory.createFromImage(document, resizedImage);
                        resources.put(xObjectName, pdImage);
                    }
                }
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage, float quality) {
        int newWidth = (int) (originalImage.getWidth() * quality);
        int newHeight = (int) (originalImage.getHeight() * quality);
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, originalImage.getType());
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return resizedImage;
    }
}
