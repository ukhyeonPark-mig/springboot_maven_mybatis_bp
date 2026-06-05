package com.example.bp.service;

import com.example.bp.domain.Setting;
import com.example.bp.mapper.SettingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Access to the {@code settings} singleton with {@code firstOrNew} semantics
 * (PRD §5.2): a missing row is created on first access.
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
}
