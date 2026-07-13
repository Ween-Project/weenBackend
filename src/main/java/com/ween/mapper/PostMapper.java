package com.ween.mapper;

import com.ween.dto.response.PostAuthorResponse;
import com.ween.dto.response.PostCommentResponse;
import com.ween.dto.response.PostResponse;
import com.ween.entity.Post;
import com.ween.entity.PostComment;
import com.ween.entity.Organization;
import com.ween.entity.User;
import com.ween.repository.PostCommentRepository;
import com.ween.repository.PostLikeRepository;
import com.ween.repository.PostRepostRepository;
import com.ween.repository.PostSaveRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import com.ween.dto.projection.PostWithStatsProjection;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "id", source = "projection.post.id")
    @Mapping(target = "author", expression = "java(toAuthorResponse(projection.getPost()))")
    @Mapping(target = "content", source = "projection.post.content")
    @Mapping(target = "mediaUrl", source = "projection.post.mediaUrl")
    @Mapping(target = "likeCount", source = "projection.likeCount")
    @Mapping(target = "commentCount", source = "projection.commentCount")
    @Mapping(target = "saveCount", source = "projection.saveCount")
    @Mapping(target = "repostCount", source = "projection.repostCount")
    @Mapping(target = "likedByMe", source = "projection.likedByMe")
    @Mapping(target = "savedByMe", source = "projection.savedByMe")
    @Mapping(target = "repostedByMe", source = "projection.repostedByMe")
    @Mapping(target = "createdAt", source = "projection.post.createdAt")
    @Mapping(target = "updatedAt", source = "projection.post.updatedAt")
    PostResponse toPostResponse(PostWithStatsProjection projection);

    @Mapping(target = "author", expression = "java(toAuthorResponse(post))")
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "commentCount", ignore = true)
    @Mapping(target = "saveCount", ignore = true)
    @Mapping(target = "repostCount", ignore = true)
    @Mapping(target = "likedByMe", ignore = true)
    @Mapping(target = "savedByMe", ignore = true)
    @Mapping(target = "repostedByMe", ignore = true)
    PostResponse toAdminPostResponse(Post post);

    @Mapping(target = "id", source = "comment.id")
    @Mapping(target = "postId", source = "comment.post.id")
    @Mapping(target = "author", source = "comment.author")
    @Mapping(target = "content", source = "comment.content")
    @Mapping(target = "createdAt", source = "comment.createdAt")
    @Mapping(target = "updatedAt", source = "comment.updatedAt")
    PostCommentResponse toCommentResponse(PostComment comment);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "profilePhotoUrl", source = "user.profilePhotoUrl")
    @Mapping(target = "accountType", constant = "USER")
    PostAuthorResponse toAuthorResponse(User user);

    @Mapping(target = "id", source = "organization.id")
    @Mapping(target = "username", source = "organization.username")
    @Mapping(target = "fullName", source = "organization.organizationName")
    @Mapping(target = "profilePhotoUrl", source = "organization.logoUrl")
    @Mapping(target = "accountType", constant = "ORGANIZATION")
    PostAuthorResponse toAuthorResponse(Organization organization);

    default PostAuthorResponse toAuthorResponse(Post post) {
        if (post.getOrganizationAuthor() != null) {
            return toAuthorResponse(post.getOrganizationAuthor());
        }
        return toAuthorResponse(post.getUserAuthor());
    }
}
