package com.example.bp.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 데이터베이스 백업 (FR-11.1): {@code mysqldump} → 로컬 백업 디렉터리에 zip으로 압축.
 * {@code mysqldump}가 PATH에 있어야 하며, 없으면 create()가 명확한 오류를 노출한다.
 * 보관 정리는 스케줄링된다 (SchedulingConfig 참고).
 */
@Service
public class BackupService {

    public record BackupFile(String name, long sizeBytes) {
    }

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path backupDir;
    private final String host;
    private final String port;
    private final String database;
    private final String username;
    private final String password;

    public BackupService(@Value("${app.backup.dir:storage/backups}") String backupDir,
                         @Value("${DB_HOST:localhost}") String host,
                         @Value("${DB_PORT:3306}") String port,
                         @Value("${DB_DATABASE:bp}") String database,
                         @Value("${spring.datasource.username:root}") String username,
                         @Value("${DB_PASSWORD:}") String password) {
        this.backupDir = Paths.get(backupDir).toAbsolutePath().normalize();
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public String create() throws IOException, InterruptedException {
        Files.createDirectories(backupDir);
        String stamp = LocalDateTime.now().format(STAMP);
        Path sql = backupDir.resolve("backup_" + stamp + ".sql");
        Path zip = backupDir.resolve("backup_" + stamp + ".zip");

        ProcessBuilder pb = new ProcessBuilder("mysqldump", "-h", host, "-P", port, "-u", username, database);
        pb.environment().put("MYSQL_PWD", password);
        pb.redirectOutput(sql.toFile());
        pb.redirectErrorStream(false);
        Process process = pb.start();
        int exit = process.waitFor();
        if (exit != 0) {
            Files.deleteIfExists(sql);
            throw new IOException("mysqldump exited with code " + exit + " (mysqldump이 PATH에 있는지 확인하세요).");
        }
        zip(sql, zip);
        Files.deleteIfExists(sql);
        return zip.getFileName().toString();
    }

    public List<BackupFile> list() {
        List<BackupFile> files = new ArrayList<>();
        if (!Files.isDirectory(backupDir)) {
            return files;
        }
        try (var stream = Files.list(backupDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".zip"))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .forEach(p -> files.add(new BackupFile(p.getFileName().toString(), sizeOf(p))));
        } catch (IOException ignored) {
        }
        return files;
    }

    public Path resolve(String name) {
        Path path = backupDir.resolve(name).normalize();
        if (!path.startsWith(backupDir)) {
            throw new IllegalArgumentException("Invalid backup name");
        }
        return path;
    }

    public void delete(String name) throws IOException {
        Files.deleteIfExists(resolve(name));
    }

    /** 보관 기간보다 오래된 백업을 삭제한다 (스케줄링됨). */
    public int cleanup(int keepDays) {
        if (!Files.isDirectory(backupDir)) {
            return 0;
        }
        long cutoff = System.currentTimeMillis() - keepDays * 24L * 60 * 60 * 1000;
        int[] removed = {0};
        try (var stream = Files.list(backupDir)) {
            stream.filter(p -> p.getFileName().toString().endsWith(".zip")).forEach(p -> {
                try {
                    if (Files.getLastModifiedTime(p).toMillis() < cutoff) {
                        Files.deleteIfExists(p);
                        removed[0]++;
                    }
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
        return removed[0];
    }

    private void zip(Path source, Path target) throws IOException {
        try (OutputStream os = Files.newOutputStream(target); ZipOutputStream zos = new ZipOutputStream(os)) {
            zos.putNextEntry(new ZipEntry(source.getFileName().toString()));
            Files.copy(source, zos);
            zos.closeEntry();
        }
    }

    private long sizeOf(Path p) {
        try {
            return Files.size(p);
        } catch (IOException e) {
            return 0;
        }
    }
}
