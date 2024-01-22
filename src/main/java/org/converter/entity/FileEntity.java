package org.converter.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
public class FileEntity {

    public FileEntity(String filename, String format, String actionTaken, byte[] fileContent,
                      Long fileSize, LocalDateTime createdAt) {
        this.filename = filename;
        this.format = format;
        this.actionTaken = actionTaken;
        this.fileContent = fileContent;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
        this.updateAt = createdAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String format;
    private String actionTaken;
    private byte[] fileContent;
    private Long fileSize;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
}
