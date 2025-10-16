package Wonbin.FinalProject.ai.diary.repository;

import Wonbin.FinalProject.ai.diary.domain.Diary;
import Wonbin.FinalProject.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    
    /**
     * 특정 사용자의 특정 날짜 일기 조회
     */
    Optional<Diary> findByUserAndDiaryDate(User user, LocalDate diaryDate);
    
    /**
     * 특정 사용자의 특정 날짜에 일기가 있는지 확인
     */
    boolean existsByUserAndDiaryDate(User user, LocalDate diaryDate);
    
    /**
     * 특정 사용자의 모든 일기 조회 (최신순)
     */
    List<Diary> findByUserOrderByDiaryDateDesc(User user);
    
    /**
     * 특정 사용자의 날짜 범위 일기 조회
     */
    List<Diary> findByUserAndDiaryDateBetweenOrderByDiaryDateDesc(
        User user, LocalDate startDate, LocalDate endDate
    );
}
