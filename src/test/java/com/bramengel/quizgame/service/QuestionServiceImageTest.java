package com.bramengel.quizgame.service;

import com.bramengel.quizgame.dto.QuestionResponse;
import com.bramengel.quizgame.exception.BadRequestException;
import com.bramengel.quizgame.exception.RecordNotFoundException;
import com.bramengel.quizgame.model.Question;
import com.bramengel.quizgame.model.Subcategory;
import com.bramengel.quizgame.repository.QuestionRepository;
import com.bramengel.quizgame.repository.SubcategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceImageTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private SubcategoryRepository subcategoryRepository;

    @InjectMocks
    private QuestionService questionService;

    @TempDir
    Path tempDir;

    private Question question;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(questionService, "questionUploadDir", tempDir.toString());
        ReflectionTestUtils.setField(questionService, "questionServePath", "/uploads/questions");

        Subcategory subcategory = new Subcategory();
        subcategory.setId(3L);
        question = new Question("Welke vlag is dit?", subcategory);
        question.setId(7L);
    }

    @Test
    void uploadImage_withImage_storesFileAndSetsImagePath() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "vlag.png", "image/png", new byte[]{1, 2, 3});
        when(questionRepository.findById(7L)).thenReturn(Optional.of(question));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        QuestionResponse response = questionService.uploadImage(7L, file);

        // Assert
        assertEquals("/uploads/questions/7_vlag.png", response.getImagePath());
        assertTrue(Files.exists(tempDir.resolve("7_vlag.png")));
        verify(questionRepository).save(question);
    }

    @Test
    void uploadImage_withEmptyFile_throwsBadRequest() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "leeg.png", "image/png", new byte[0]);
        when(questionRepository.findById(7L)).thenReturn(Optional.of(question));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> questionService.uploadImage(7L, file));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void uploadImage_withNonImageFile_throwsBadRequest() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "tekst.txt", "text/plain", new byte[]{1});
        when(questionRepository.findById(7L)).thenReturn(Optional.of(question));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> questionService.uploadImage(7L, file));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void uploadImage_withUnknownQuestion_throwsRecordNotFound() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "vlag.png", "image/png", new byte[]{1});
        when(questionRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RecordNotFoundException.class, () -> questionService.uploadImage(99L, file));
    }
}
