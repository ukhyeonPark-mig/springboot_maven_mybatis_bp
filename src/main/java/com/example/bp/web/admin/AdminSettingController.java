package com.example.bp.web.admin;

import com.example.bp.domain.Setting;
import com.example.bp.service.BrandingService;
import com.example.bp.service.ImageService;
import com.example.bp.service.SettingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * 관리자 설정 (FR-10): 버전/푸터 정보, 개인정보 처리방침 및 이용약관(HugeRTE 리치
 * 텍스트), 브랜딩(로고 변형 8종 + 멀티 사이즈 PNG + favicon 생성).
 */
@Controller
@RequestMapping("/admin/setting")
public class AdminSettingController {

    private static final int[] COLOR_PNG_SIZES = {512, 192, 180, 150, 32, 16};
    private static final int[] WHITE_PNG_SIZES = {512};

    private final SettingService settingService;
    private final BrandingService brandingService;
    private final ImageService imageService;

    public AdminSettingController(SettingService settingService, BrandingService brandingService,
                                  ImageService imageService) {
        this.settingService = settingService;
        this.brandingService = brandingService;
        this.imageService = imageService;
    }

    // ── 정보 (FR-10.1) ────────────────────────────────────────────────
    @GetMapping("/information")
    public String information(Model model) {
        informationModel(model);
        return "admin/setting/information";
    }

    @PostMapping("/information")
    public String saveInformation(@RequestParam(defaultValue = "") String footer,
                                  @RequestParam(defaultValue = "") String version, Model model) {
        settingService.updateInformation(footer, version);
        model.addAttribute("success", "정보가 업데이트되었습니다.");
        informationModel(model);
        return "admin/setting/information :: card";
    }

    // ── 개인정보 처리방침 (FR-10.2) ────────────────────────────────────────────────────
    @GetMapping("/privacy")
    public String privacy(Model model) {
        model.addAttribute("privacy", settingService.get().getPrivacy());
        return "admin/setting/privacy";
    }

    @PostMapping("/privacy")
    public String savePrivacy(@RequestParam(defaultValue = "") String privacy, Model model) {
        settingService.updatePrivacy(privacy);
        model.addAttribute("success", "개인정보 처리방침이 업데이트되었습니다.");
        return "fragments/message :: toasts";
    }

    // ── 이용약관 (FR-10.3) ──────────────────────────────────────────────────────
    @GetMapping("/terms")
    public String terms(Model model) {
        model.addAttribute("terms", settingService.get().getTerms());
        return "admin/setting/terms";
    }

    @PostMapping("/terms")
    public String saveTerms(@RequestParam(defaultValue = "") String terms, Model model) {
        settingService.updateTerms(terms);
        model.addAttribute("success", "이용약관이 업데이트되었습니다.");
        return "fragments/message :: toasts";
    }

    // ── 브랜딩 (FR-10.4) ───────────────────────────────────────────────────
    @GetMapping("/branding")
    public String branding() {
        return "admin/setting/branding";
    }

    @PostMapping("/branding")
    public String saveBranding(@RequestParam(required = false) MultipartFile logo_color_square_svg,
                              @RequestParam(required = false) MultipartFile logo_color_svg,
                              @RequestParam(required = false) MultipartFile logo_white_square_svg,
                              @RequestParam(required = false) MultipartFile logo_white_svg,
                              @RequestParam(required = false) MultipartFile logo_color_square_png,
                              @RequestParam(required = false) MultipartFile logo_color_png,
                              @RequestParam(required = false) MultipartFile logo_white_square_png,
                              @RequestParam(required = false) MultipartFile logo_white_png,
                              Model model) {
        try {
            saveRaw(logo_color_square_svg, "logo_color_square.svg");
            saveRaw(logo_color_svg, "logo_color.svg");
            saveRaw(logo_white_square_svg, "logo_white_square.svg");
            saveRaw(logo_white_svg, "logo_white.svg");

            if (isPresent(logo_color_square_png)) {
                saveSquarePng(logo_color_square_png, "logo_color", COLOR_PNG_SIZES);
                brandingService.write("favicon.ico", imageService.toPngSquare(logo_color_square_png.getBytes(), 32));
            }
            saveRaw(logo_color_png, "logo_color.png");

            if (isPresent(logo_white_square_png)) {
                saveSquarePng(logo_white_square_png, "logo_white", WHITE_PNG_SIZES);
            }
            saveRaw(logo_white_png, "logo_white.png");

            model.addAttribute("success", "브랜딩 자산이 저장되었습니다.");
        } catch (Exception e) {
            model.addAttribute("error", "브랜딩 자산을 저장하지 못했습니다. PNG/SVG 파일을 확인해 주세요.");
        }
        return "admin/setting/branding :: card";
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────
    private void informationModel(Model model) {
        Setting setting = settingService.get();
        model.addAttribute("footer", setting.getFooter());
        model.addAttribute("version", setting.getVersion());
    }

    private boolean isPresent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private void saveRaw(MultipartFile file, String filename) throws java.io.IOException {
        if (isPresent(file)) {
            brandingService.write(filename, file.getBytes());
        }
    }

    private void saveSquarePng(MultipartFile file, String prefix, int[] sizes) throws java.io.IOException {
        byte[] bytes = file.getBytes();
        for (int size : sizes) {
            brandingService.write(prefix + "_" + size + ".png", imageService.toPngSquare(bytes, size));
        }
    }
}
