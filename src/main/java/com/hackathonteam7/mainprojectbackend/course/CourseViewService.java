package com.hackathonteam7.mainprojectbackend.course;

import com.hackathonteam7.mainprojectbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코스 조회수를 (course_id, user_id) 당 한 번만 올리기 위한 최초-조회 판별.
 * REQUIRES_NEW 로 별도 트랜잭션에서 실행한다 — 동시 요청으로 uq_course_view 위반이 나도
 * 그 실패가 호출자(코스 상세 조회)의 트랜잭션을 rollback-only 로 오염시키지 않게 하기 위해서다.
 */
@Service
@RequiredArgsConstructor
public class CourseViewService {

    private final CourseViewRepository courseViewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFirstView(Long courseId, Long userId) {
        if (courseViewRepository.existsByCourseIdAndUserId(courseId, userId)) {
            return false;
        }
        try {
            courseViewRepository.saveAndFlush(
                    CourseView.builder()
                            .course(courseRepository.getReferenceById(courseId))
                            .user(userRepository.getReferenceById(userId))
                            .build()
            );
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }
}
