package com.fastgpt.ai.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for handling files
 */
public class FileUtils {
    
    private static final Tika tika = new Tika();
    
    // Document types
    private static final String[] DOCUMENT_EXTENSIONS = {
        "pdf", "doc", "docx", "txt", "rtf", "csv", "xls", "xlsx", "ppt", "pptx"
    };
    
    // Image types
    private static final String[] IMAGE_EXTENSIONS = {
        "jpg", "jpeg", "png", "gif", "bmp", "svg", "webp"
    };
    
    // Audio types
    private static final String[] AUDIO_EXTENSIONS = {
        "mp3", "wav", "ogg", "m4a", "flac"
    };
    
    /**
     * Remove files by paths
     * 
     * @param paths Array of file paths
     */
    public static void removeFilesByPaths(String[] paths) {
        if (paths == null || paths.length == 0) {
            return;
        }
        
        for (String path : paths) {
            try {
                Files.deleteIfExists(Paths.get(path));
            } catch (IOException e) {
                // Log but continue with other files
                System.err.println("Failed to delete file: " + path + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * Extract text from a PDF file
     * 
     * @param pdfFile PDF file
     * @return Extracted text
     */
    public static String extractTextFromPdf(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    /**
     * Extract text from a PDF input stream
     * 
     * @param inputStream PDF input stream
     * @return Extracted text
     */
    public static String extractTextFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    /**
     * Check if a file is an image
     * 
     * @param contentType File MIME type
     * @return true if the file is an image
     */
    public static boolean isImageFile(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        if (contentType.startsWith("image/")) {
            return true;
        }
        
        for (String ext : IMAGE_EXTENSIONS) {
            if (contentType.contains(ext)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a file is a document
     * 
     * @param contentType File MIME type
     * @return true if the file is a document
     */
    public static boolean isDocumentFile(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        if (contentType.startsWith("text/") || 
            contentType.equals("application/pdf") ||
            contentType.contains("document") ||
            contentType.contains("spreadsheet") ||
            contentType.contains("presentation")) {
            return true;
        }
        
        for (String ext : DOCUMENT_EXTENSIONS) {
            if (contentType.contains(ext)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a file is an audio file
     * 
     * @param contentType File MIME type
     * @return true if the file is an audio file
     */
    public static boolean isAudioFile(String contentType) {
        if (contentType == null) {
            return false;
        }
        
        if (contentType.startsWith("audio/")) {
            return true;
        }
        
        for (String ext : AUDIO_EXTENSIONS) {
            if (contentType.contains(ext)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Calculate file size in human-readable format
     * 
     * @param size Size in bytes
     * @return Human-readable size string
     */
    public static String formatFileSize(long size) {
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int unitIndex = 0;
        double fileSize = size;
        
        while (fileSize > 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", fileSize, units[unitIndex]);
    }
    
    /**
     * Get file icon based on file name
     * 
     * @param fileName File name
     * @return Icon identifier
     */
    public static String getFileIcon(String fileName) {
        if (fileName == null) {
            return "unknown";
        }
        
        String extension = getFileExtension(fileName).toLowerCase();
        
        // Document types
        if (Arrays.asList("pdf").contains(extension)) {
            return "pdf";
        } else if (Arrays.asList("doc", "docx").contains(extension)) {
            return "word";
        } else if (Arrays.asList("xls", "xlsx").contains(extension)) {
            return "excel";
        } else if (Arrays.asList("ppt", "pptx").contains(extension)) {
            return "powerpoint";
        } else if (Arrays.asList("txt", "md", "rtf").contains(extension)) {
            return "text";
        }
        
        // Audio types
        if (Arrays.asList(AUDIO_EXTENSIONS).contains(extension)) {
            return "audio";
        }
        
        // Default
        return "file";
    }
    
    /**
     * Get file extension from file name
     * 
     * @param fileName File name
     * @return File extension
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty() || !fileName.contains(".")) {
            return "";
        }
        
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
    
    /**
     * Create a temporary file from a MultipartFile
     * 
     * @param file MultipartFile
     * @param prefix File prefix
     * @param suffix File suffix
     * @return Temporary file
     */
    public static File createTempFile(MultipartFile file, String prefix, String suffix) throws IOException {
        File tempFile = File.createTempFile(prefix, suffix);
        file.transferTo(tempFile);
        tempFile.deleteOnExit();
        return tempFile;
    }
} 