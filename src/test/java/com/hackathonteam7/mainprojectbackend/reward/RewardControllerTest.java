package com.hackathonteam7.mainprojectbackend.reward;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathonteam7.mainprojectbackend.course.Course;
import com.hackathonteam7.mainprojectbackend.course.CourseRepository;
import com.hackathonteam7.mainprojectbackend.course.CourseStatus;
import com.hackathonteam7.mainprojectbackend.course.CourseType;
import com.hackathonteam7.mainprojectbackend.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class RewardControllerTest extends IntegrationTestSupport {

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void update_stockReplacesRatherThanIncrements() throws Exception {
        Reward reward = rewardRepository.save(Reward.builder()
                .organization(organization).name("커피 쿠폰").kind(RewardKind.COUPON).stock(5).build());

        mockMvc.perform(put("/admin/rewards/{id}", reward.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stock": 40}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(40));
    }

    @Test
    void delete_rewardLinkedToCourse_returns409WithCourseDetails() throws Exception {
        Reward reward = rewardRepository.save(Reward.builder()
                .organization(organization).name("포인트").kind(RewardKind.POINT).stock(100).build());
        courseRepository.save(Course.builder()
                .organization(organization)
                .reward(reward)
                .name("연동 코스")
                .type(CourseType.OFFICIAL)
                .status(CourseStatus.DRAFT)
                .isOrdered(true)
                .build());

        mockMvc.perform(delete("/admin/rewards/{id}", reward.getId()).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REWARD_IN_USE"))
                .andExpect(jsonPath("$.details[0].name").value("연동 코스"));
    }
}
