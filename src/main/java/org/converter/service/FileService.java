package org.converter.service;

import org.converter.entity.FileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface FileService {
    List<FileEntity> getAllFiles();
    FileEntity saveFile(FileEntity file);
    FileEntity getFileById(Long id);
    void deleteFile(Long id);
    byte[] convertPdfToText(MultipartFile file) throws IOException;
    byte[] mergePdf(List<MultipartFile> pdfs) throws IOException;
    int getPageCount(InputStream inputStream) throws IOException;
    byte[] separationPdf(MultipartFile file, String parts) throws IOException;
    byte[] compressionPdf(MultipartFile file, float quality) throws IOException;
}
