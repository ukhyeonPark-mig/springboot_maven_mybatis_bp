package com.example.bp.mapper;

import com.example.bp.domain.Setting;
import org.apache.ibatis.annotations.Mapper;

/** {@code settings} 싱글톤을 위한 MyBatis mapper. */
@Mapper
public interface SettingMapper {

    /** 첫 번째(유일한) settings 행, 아직 없으면 {@code null}. */
    Setting findFirst();

    int insert(Setting setting);

    int update(Setting setting);
}
