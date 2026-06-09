package com.example.bp.service;

import com.example.bp.domain.Setting;
import com.example.bp.mapper.SettingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code firstOrNew} 의미론으로 {@code settings} 싱글톤에 접근한다
 * (PRD §5.2): 행이 없으면 최초 접근 시 생성된다.
 */
@Service
public class SettingService {

    private final SettingMapper settingMapper;

    public SettingService(SettingMapper settingMapper) {
        this.settingMapper = settingMapper;
    }

    @Transactional
    public Setting get() {
        Setting setting = settingMapper.findFirst();
        if (setting == null) {
            setting = new Setting();
            settingMapper.insert(setting);
        }
        return setting;
    }

    @Transactional
    public void save(Setting setting) {
        Setting existing = settingMapper.findFirst();
        if (existing == null) {
            settingMapper.insert(setting);
        } else {
            setting.setId(existing.getId());
            settingMapper.update(setting);
        }
    }

    /** 다른 싱글톤 컬럼을 보존하는 필드 범위 갱신 (FR-10). */
    @Transactional
    public void updateInformation(String footer, String version) {
        Setting setting = get();
        setting.setFooter(footer);
        setting.setVersion(version);
        settingMapper.update(setting);
    }

    @Transactional
    public void updatePrivacy(String html) {
        Setting setting = get();
        setting.setPrivacy(html);
        settingMapper.update(setting);
    }

    @Transactional
    public void updateTerms(String html) {
        Setting setting = get();
        setting.setTerms(html);
        settingMapper.update(setting);
    }
}
