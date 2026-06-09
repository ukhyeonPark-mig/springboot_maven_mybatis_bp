package com.example.bp.web.admin;

import java.nio.file.Files;
import java.nio.file.Path;

import com.example.bp.service.BackupService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/** 백업 관리 (FR-11.1): 생성 / 목록 / 다운로드 / 삭제 (관리자 전용). */
@Controller
public class AdminBackupController {

    private static final String CARD = "admin/development/backup :: card";

    private final BackupService backupService;

    public AdminBackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @GetMapping("/admin/development/backup")
    public String backup(Model model) {
        model.addAttribute("backups", backupService.list());
        return "admin/development/backup";
    }

    @PostMapping("/admin/development/backup/create")
    public String create(Model model) {
        try {
            String name = backupService.create();
            model.addAttribute("success", "백업이 생성되었습니다: " + name);
        } catch (Exception e) {
            model.addAttribute("error", "백업 생성에 실패했습니다. " + e.getMessage());
        }
        model.addAttribute("backups", backupService.list());
        return CARD;
    }

    @PostMapping("/admin/development/backup/delete/{name}")
    public String delete(@PathVariable String name, Model model) {
        try {
            backupService.delete(name);
            model.addAttribute("success", "백업이 삭제되었습니다.");
        } catch (Exception e) {
            model.addAttribute("error", "백업 삭제에 실패했습니다.");
        }
        model.addAttribute("backups", backupService.list());
        return CARD;
    }

    @GetMapping("/admin/development/backup/download/{name}")
    @ResponseBody
    public ResponseEntity<Resource> download(@PathVariable String name) {
        Path path = backupService.resolve(name);
        if (!Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .body(new FileSystemResource(path));
    }
}
