package Wonbin.FinalProject.ai.diary.service;

import Wonbin.FinalProject.ai.diary.domain.Diary;
import Wonbin.FinalProject.ai.diary.domain.Mood;
import Wonbin.FinalProject.ai.diary.dto.DiaryRequest;
import Wonbin.FinalProject.ai.diary.dto.DiaryResponse;
import Wonbin.FinalProject.ai.diary.repository.DiaryRepository;
import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final UserRepository userRepository;

    /**
     * 일기 작성 (같은 날짜에 이미 일기가 있으면 에러)
     */
    @Transactional
    public DiaryResponse saveDiary(Long userId, DiaryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        LocalDate diaryDate = LocalDate.parse(request.getDate());
        Mood mood = Mood.fromKey(request.getMood());

        // 같은 날짜에 일기가 이미 있는지 확인
        boolean exists = diaryRepository.existsByUserAndDiaryDate(user, diaryDate);
        
        if (exists) {
            // 이미 일기가 있으면 에러 반환
            throw new IllegalArgumentException("이미 오늘 일기를 작성하셨습니다. 하루에 하나의 일기만 작성할 수 있습니다.");
        }

        // 새로운 일기 생성
        Diary diary = Diary.builder()
                .user(user)
                .diaryDate(diaryDate)
                .mood(mood)
                .content(request.getContent())
                .build();
        
        diary = diaryRepository.save(diary);
        log.info("새 일기 저장 완료 - userId: {}, date: {}", userId, diaryDate);

        return DiaryResponse.from(diary);
    }
    /**
     * 특정 날짜의 일기 조회
     */
    public DiaryResponse getDiary(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findByUserAndDiaryDate(user, date)
                .orElseThrow(() -> new IllegalArgumentException("해당 날짜의 일기를 찾을 수 없습니다."));

        return DiaryResponse.from(diary);
    }

    /**
     * 특정 사용자의 모든 일기 조회 (최신순)
     */
    public List<DiaryResponse> getAllDiaries(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Diary> diaries = diaryRepository.findByUserOrderByDiaryDateDesc(user);
        
        return diaries.stream()
                .map(DiaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 기간의 일기 조회
     */
    public List<DiaryResponse> getDiariesByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Diary> diaries = diaryRepository.findByUserAndDiaryDateBetweenOrderByDiaryDateDesc(
                user, startDate, endDate
        );

        return diaries.stream()
                .map(DiaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 일기 삭제
     */
    @Transactional
    public void deleteDiary(Long userId, Long diaryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));

        // 본인의 일기인지 확인
        if (!diary.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 일기만 삭제할 수 있습니다.");
        }

        diaryRepository.delete(diary);
        log.info("일기 삭제 완료 - userId: {}, diaryId: {}", userId, diaryId);
    }

//    /**
//     * 일기 요약 저장
//     */
//    @Transactional
//    public DiaryResponse saveSummary(Long userId, Long diaryId, String summaryText) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
//
//        Diary diary = diaryRepository.findById(diaryId)
//                .orElseThrow(() -> new IllegalArgumentException("일기를 찾을 수 없습니다."));
//
//        // 본인의 일기인지 확인
//        if (!diary.getUser().getId().equals(userId)) {
//            throw new IllegalArgumentException("본인의 일기만 수정할 수 있습니다.");
//        }
//
//        // 요약 텍스트 저장
//        diary.updateSummary(summaryText);
//        log.info("일기 요약 저장 완료 - userId: {}, diaryId: {}", userId, diaryId);
//
//        return DiaryResponse.from(diary);
//    }
}
