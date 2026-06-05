package com.example.bp.mapper;

import com.example.bp.domain.Setting;
import org.apache.ibatis.annotations.Mapper;

/** MyBatis mapper for the {@code settings} singleton. */
@Mapper
public interface SettingMapper {

    /** First (only) settings row, or {@code null} if none yet. */
    Setting findFirst();

    int insert(Setting setting);

    int update(Setting setting);
}
