package org.converter.controller;

import lombok.extern.slf4j.Slf4j;
import org.converter.service.FileService;
import org.converter.utils.FileExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/pdf")
@Slf4j
public class PdfController {
    private final FileService fileService;

    @Autowired
    public PdfController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Integer> getPageCount(@RequestParam("file") MultipartFile file) throws IOException {
        try {
            Integer size = fileService.getPageCount(file.getInputStream());
            return ResponseEntity.ok().body(size);
        } catch (Exception ex) {
            log.error("Ошибка при расчете количества страниц", ex);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(0);
        }
    }

    @PostMapping(value = "/separation", params = {"parts"})
    public ResponseEntity<byte[]> handleSeparationPdf(@RequestParam("file") MultipartFile file, @RequestParam("parts") String parts) {
        try {
            byte[] zipBytes = fileService.separationPdf(file, parts);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", file.getName() + ".zip");

            return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
        } catch (Exception ex) {
            log.error("Ошибка при разделении файлов", ex);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        }
    }

    @PostMapping("/convert-pdf-to-txt")
    public ResponseEntity<byte[]> handleConvertPdfToTxt(@RequestParam("file") MultipartFile file)  {
        try {
            byte[] textBytes = fileService.convertPdfToText(file);
            String filename = file.getName() + ".txt";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(textBytes, headers, HttpStatus.OK);
        } catch (IOException ex) {
            log.error("Ошибка при конвертации pdf в txt ", ex);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        }
    }

    @PostMapping("/merge")
    public ResponseEntity<byte[]> handleMergePdf(@RequestBody List<MultipartFile> files) {
        try {
            byte[] pdfBytes = fileService.mergePdf(files);
            String filename = files.get(0).getName() + "_merged.pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (IOException ex) {
            log.error("Ошибка при объединении pdf файлов", ex);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        }
    }

    @PostMapping(value = "/compression", params = {"quality"})
    public ResponseEntity<byte[]> handleCompressionPdf(@RequestParam("file") MultipartFile file, @RequestParam("quality") float quality) {
        try {
            byte[] pdfBytes = fileService.compressionPdf(file, quality);
            String filename = file.getName() + "_compressed.pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (IOException ex) {
            log.error("Ошибка при сжатии pdf файла", ex);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        }
    }
}
