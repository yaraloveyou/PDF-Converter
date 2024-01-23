package org.converter.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.converter.config.RedisLock;
import org.converter.rabbit.Sender;
import org.converter.service.FileService;
import org.converter.utils.FileExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/pdf")
@Slf4j
public class PdfController {

    RedisLock redisLock;
    Sender sender;
    private static final String GENERATE_TASKS_KEY = "converter:controller:pdfcontroller";
    private final FileService fileService;

    @Autowired
    public PdfController(FileService fileService, RedisLock redisLock) {
        this.fileService = fileService;
        this.redisLock = redisLock;
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
            String fileName = file.getName() + ".txt";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", fileName);

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
            if (redisLock.acquireLock(200, GENERATE_TASKS_KEY)) {
                log.info(Strings.repeat("-", 50));
                log.info("Service start");
                byte[] pdfBytes = fileService.mergePdf(files);
                String fileName = files.get(0).getName() + "_merged.pdf";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("attachment", fileName);

                log.info("Service end");
                log.info(Strings.repeat("-", 50));
                redisLock.releaseLock(GENERATE_TASKS_KEY);
                return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            } else {
                log.warn("Не удалось получить блокировку для задачи: {}", GENERATE_TASKS_KEY);
                return ResponseEntity.status(HttpStatus.LOCKED).body(new byte[0]);
            }
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
            String fileName = file.getName() + "_compressed.pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", fileName);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (IOException ex) {
            log.error("Ошибка при сжатии pdf файла", ex);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new byte[0]);
        }
    }
}
