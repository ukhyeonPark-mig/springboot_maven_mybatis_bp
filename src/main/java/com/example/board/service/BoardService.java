package com.example.board.service;

import com.example.board.dto.BoardDto;
import com.example.board.dto.BoardSearchDto;
import com.example.board.mapper.BoardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardMapper boardMapper;

    public List<BoardDto> getBoardList(BoardSearchDto searchDto) {
        return boardMapper.selectBoardList(searchDto);
    }

    public int getBoardCount(BoardSearchDto searchDto) {
        return boardMapper.selectBoardCount(searchDto);
    }

    @Transactional
    public BoardDto getBoardById(Long id) {
        boardMapper.increaseViewCount(id);
        return boardMapper.selectBoardById(id);
    }

    public BoardDto getBoardForEdit(Long id) {
        return boardMapper.selectBoardById(id);
    }

    @Transactional
    public void createBoard(BoardDto boardDto) {
        boardMapper.insertBoard(boardDto);
    }

    @Transactional
    public void updateBoard(BoardDto boardDto) {
        boardMapper.updateBoard(boardDto);
    }

    @Transactional
    public void deleteBoard(Long id) {
        boardMapper.deleteBoard(id);
    }
}
