package vn.edu.iuh.fit.olachatbackend.dtos.responses;

import lombok.Builder;
import lombok.Data;
import vn.edu.iuh.fit.olachatbackend.entities.Comment;
import vn.edu.iuh.fit.olachatbackend.entities.Media;
import vn.edu.iuh.fit.olachatbackend.entities.User;
import vn.edu.iuh.fit.olachatbackend.enums.Privacy;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PostResponse {
    private Long postId;
    private PostUserResponse createdBy;
    private String content;
    private List<MediaPostResponse> attachments;
    private Privacy privacy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PostUserResponse> likedUsers;
    private List<CommentHierarchyResponse> comments;
    private Long originalPostId;
    private PostResponse originalPost;
}