package com.example.board.mapper;

import com.example.board.dto.BoardDto;
import com.example.board.dto.BoardSearchDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BoardMapper {

    List<BoardDto> selectBoardList(BoardSearchDto searchDto);

    int selectBoardCount(BoardSearchDto searchDto);

    BoardDto selectBoardById(Long id);

    void insertBoard(BoardDto boardDto);

    void updateBoard(BoardDto boardDto);

    void deleteBoard(Long id);

    void increaseViewCount(Long id);
}
