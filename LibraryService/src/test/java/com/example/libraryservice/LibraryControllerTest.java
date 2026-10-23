package com.example.libraryservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LibraryController.class)
@Import(ApiExceptionHandler.class)
class LibraryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookGrpcClient bookGrpcClient;

    @Test
    void returnsBookAsJson() throws Exception {
        when(bookGrpcClient.getBook(1))
                .thenReturn(new BookDto(1, "Clean Code", "Robert C. Martin", "9780132350884", true));

        mockMvc.perform(get("/api/library/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void rejectsNonPositiveId() throws Exception {
        mockMvc.perform(get("/api/library/books/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid book ID"));
    }

    @Test
    void listsBooks() throws Exception {
        when(bookGrpcClient.listBooks()).thenReturn(List.of(
                new BookDto(1, "Clean Code", "Robert C. Martin", "9780132350884", true)));

        mockMvc.perform(get("/api/library/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }
}
