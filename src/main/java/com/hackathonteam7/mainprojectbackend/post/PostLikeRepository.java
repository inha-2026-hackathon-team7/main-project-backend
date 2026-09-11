package com.hackathonteam7.mainprojectbackend.post;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    long countByPostId(Long postId);

    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);
}
