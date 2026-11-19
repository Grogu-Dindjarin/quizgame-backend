package com.bramengel.quizgame.service;

import com.bramengel.quizgame.dto.AnswerResponse;
import com.bramengel.quizgame.dto.QuestionRequest;
import com.bramengel.quizgame.dto.QuestionResponse;
import com.bramengel.quizgame.exception.BadRequestException;
import com.bramengel.quizgame.exception.RecordNotFoundException;
import com.bramengel.quizgame.model.Question;
import com.bramengel.quizgame.model.Subcategory;
import com.bramengel.quizgame.repository.QuestionRepository;
import com.bramengel.quizgame.repository.SubcategoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final SubcategoryRepository subcategoryRepository;

    @Value("${app.questions.upload-dir:uploads/questions}")
    private String questionUploadDir;

    @Value("${app.questions.serve-path:/uploads/questions}")
    private String questionServePath;

    public QuestionService(QuestionRepository questionRepository, SubcategoryRepository subcategoryRepository) {
        this.questionRepository = questionRepository;
        this.subcategoryRepository = subcategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> findAll() {
        return questionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuestionResponse findById(Long id) {
        return toResponse(findQuestionOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> findBySubcategoryId(Long subcategoryId) {
        findSubcategoryOrThrow(subcategoryId);
        return questionRepository.findBySubcategoryId(subcategoryId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public QuestionResponse create(QuestionRequest req) {
        Subcategory subcategory = findSubcategoryOrThrow(req.getSubcategoryId());
        Question question = new Question(req.getText(), subcategory);
        question.setImagePath(req.getImagePath());
        return toResponse(questionRepository.save(question));
    }

    @Transactional
    public QuestionResponse update(Long id, QuestionRequest req) {
        Question question = findQuestionOrThrow(id);
        Subcategory subcategory = findSubcategoryOrThrow(req.getSubcategoryId());
        question.setText(req.getText());
        question.setImagePath(req.getImagePath());
        question.setSubcategory(subcategory);
        return toResponse(questionRepository.save(question));
    }

    @Transactional
    public void delete(Long id) {
        findQuestionOrThrow(id);
        questionRepository.deleteById(id);
    }

    @Transactional
    public QuestionResponse uploadImage(Long id, MultipartFile file) {
        Question question = findQuestionOrThrow(id);

        if (file.isEmpty()) {
            throw new BadRequestException("Bestand mag niet leeg zijn");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Alleen afbeeldingen zijn toegestaan");
        }

        try {
            Path uploadPath = Paths.get(questionUploadDir);
            Files.createDirectories(uploadPath);

            String originalName = Paths.get(String.valueOf(file.getOriginalFilename())).getFileName().toString();
            String filename = question.getId() + "_" + originalName;
            Files.copy(file.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);

            question.setImagePath(questionServePath + "/" + filename);
        } catch (IOException e) {
            throw new BadRequestException("Afbeelding opslaan mislukt: " + e.getMessage());
        }

        return toResponse(questionRepository.save(question));
    }

    private Subcategory findSubcategoryOrThrow(Long id) {
        return subcategoryRepository.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("Subcategorie niet gevonden met id: " + id));
    }

    private Question findQuestionOrThrow(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("Vraag niet gevonden met id: " + id));
    }

    private QuestionResponse toResponse(Question q) {
        List<AnswerResponse> answers = q.getAnswers().stream()
                .map(a -> new AnswerResponse(a.getId(), a.getText(), a.isCorrect(), q.getId()))
                .toList();
        return new QuestionResponse(
                q.getId(),
                q.getText(),
                q.getImagePath(),
                q.getSubcategory().getId(),
                q.getSubcategory().getName(),
                answers
        );
    }
}
