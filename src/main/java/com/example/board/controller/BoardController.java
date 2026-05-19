package com.example.board.controller;

import com.example.board.dto.BoardDto;
import com.example.board.dto.BoardSearchDto;
import com.example.board.service.BoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public String list(@ModelAttribute BoardSearchDto searchDto, Model model) {
        List<BoardDto> boardList = boardService.getBoardList(searchDto);
        int totalCount = boardService.getBoardCount(searchDto);
        int totalPages = (int) Math.ceil((double) totalCount / searchDto.getSize());

        model.addAttribute("boardList", boardList);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("search", searchDto);
        return "board/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        BoardDto board = boardService.getBoardById(id);
        if (board == null) {
            return "redirect:/board";
        }
        model.addAttribute("board", board);
        return "board/detail";
    }

    @GetMapping("/write")
    public String writeForm(Model model) {
        model.addAttribute("board", new BoardDto());
        return "board/write";
    }

    @PostMapping("/write")
    public String write(@ModelAttribute BoardDto boardDto, RedirectAttributes redirectAttributes) {
        boardService.createBoard(boardDto);
        redirectAttributes.addFlashAttribute("message", "게시글이 등록되었습니다.");
        return "redirect:/board";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        BoardDto board = boardService.getBoardForEdit(id);
        if (board == null) {
            return "redirect:/board";
        }
        model.addAttribute("board", board);
        return "board/edit";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute BoardDto boardDto,
                       RedirectAttributes redirectAttributes) {
        boardDto.setId(id);
        boardService.updateBoard(boardDto);
        redirectAttributes.addFlashAttribute("message", "게시글이 수정되었습니다.");
        return "redirect:/board/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boardService.deleteBoard(id);
        redirectAttributes.addFlashAttribute("message", "게시글이 삭제되었습니다.");
        return "redirect:/board";
    }
}
