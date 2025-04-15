package com.goorm.clonestagram.util;

import com.goorm.clonestagram.comment.repository.CommentRepository;
import com.goorm.clonestagram.feed.domain.Feeds;
import com.goorm.clonestagram.feed.repository.FeedRepository;
import com.goorm.clonestagram.feed.service.FeedService;
import com.goorm.clonestagram.follow.domain.Follows;
import com.goorm.clonestagram.follow.dto.FollowDto;
import com.goorm.clonestagram.follow.mapper.FollowMapper;
import com.goorm.clonestagram.follow.repository.FollowRepository;
import com.goorm.clonestagram.follow.service.FollowService;
import com.goorm.clonestagram.post.ContentType;
import com.goorm.clonestagram.post.domain.Posts;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.post.service.PostService;

import com.goorm.clonestagram.user.domain.repository.UserExternalReadRepository;
import com.goorm.clonestagram.user.domain.repository.UserExternalWriteRepository;
import com.goorm.clonestagram.user.infrastructure.entity.UserEntity;
import com.goorm.clonestagram.user.infrastructure.repository.JpaUserExternalWriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class IntegrationTestHelper {

	private final JpaUserExternalWriteRepository userRepository;
	private final PostsRepository postsRepository;
	private final CommentRepository commentRepository;
	private final FollowRepository followRepository;
	private final FeedRepository feedRepository;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;
	private final PostService postService;
	private final FollowService followService;
	private final FeedService feedService;

	@Autowired
	public IntegrationTestHelper(JpaUserExternalWriteRepository userRepository,
		PostsRepository postsRepository,
		CommentRepository commentRepository,
		FollowRepository followRepository,
		FeedRepository feedRepository,
		BCryptPasswordEncoder bCryptPasswordEncoder,
		PostService postService,
		FollowService followService,
		FeedService feedService) {
		this.userRepository = userRepository;
		this.postsRepository = postsRepository;
		this.commentRepository = commentRepository;
		this.followRepository = followRepository;
		this.feedRepository = feedRepository;
		this.bCryptPasswordEncoder = bCryptPasswordEncoder;
		this.postService = postService;
		this.followService = followService;
		this.feedService = feedService;

	}

	/**
	 * 임의의 유저를 생성합니다.
	 */
	public UserEntity createUser(String basename) {

		String trimmedBase = basename.length() > 12 ? basename.substring(0, 12) : basename;
		String suffix = UUID.randomUUID().toString().substring(0, 5);

		String uniquename = trimmedBase + "_" + suffix;

		UserEntity user = UserEntity.builder()
			.name(uniquename)
			.password(bCryptPasswordEncoder.encode("password!@123"))
			.email(uniquename + "@example.com")
			.build();
		System.out.println("유저 생성 완료: " + user);
		return userRepository.save(user);
	}


	/**
	 * 유저와 연관된 모든 post, comment 를 삭제한 후 유저를 삭제합니다.
	 */
	@Transactional
	public void deleteUserAndDependencies(UserEntity user) {
		Long userId = user.getId();

		List<Posts> posts = postsRepository.findAllByUserIdAndDeletedIsFalse(userId);
		List<Long> postIds = posts.stream()
			.map(Posts::getId)
			.collect(Collectors.toList());
		if (!postIds.isEmpty()) {
			feedRepository.deleteAllByPostIdIn(postIds);
			commentRepository.deleteAllByPosts_IdIn(postIds);
		}
		postsRepository.deleteAllByUserId(userId);
		followRepository.deleteAllByFollowerId(userId);
		followRepository.deleteAllByFollowedId(userId);
		feedRepository.deleteAllByUserId(userId);
		userRepository.deleteById(userId);
	}

	@Transactional
	public void deleteOnlyUser(UserEntity user) {
		userRepository.deleteById(user.getId());
	}

	/**
	 * 테스트용 게시글을 생성합니다.
	 */
	public Posts createPost(UserEntity user) {
		Posts post = Posts.builder()
			.user(user)
			.content("테스트 게시물")
			.mediaName("media.jpg")
			.contentType(ContentType.IMAGE)
			.build();

		Posts saved = postService.save(post);
		feedService.createFeedForFollowers(saved); // ✅ 피드 생성
		feedRepository.save(Feeds.builder()
			.user(user)
			.post(saved)
			.build());
		return saved;
	}

	public void follow(UserEntity from, UserEntity to) {
		followService.toggleFollow(from.getId(), to.getId());
	}

	/**
	 * 이미지 업로드용 요청 객체를 생성합니다.
	 */
	public Posts createImagePost(UserEntity user, String fileUrl, String content, List<String> tags) {
		Posts post = Posts.builder()
			.user(user)
			.content(content != null ? content : "기본 이미지 내용")
			.mediaName(fileUrl != null ? fileUrl : "https://default-image-url.com/test.jpg")
			.contentType(ContentType.IMAGE)
			.build();
		return postService.save(post);
	}

	/**
	 * 비디오 업로드용 요청 객체를 생성합니다.
	 */
	public Posts createVideoPost(UserEntity user, String fileUrl, String content, List<String> tags) {
		Posts post = Posts.builder()
			.user(user)
			.content(content != null ? content : "기본 비디오 내용")
			.mediaName(fileUrl != null ? fileUrl : "https://default-video-url.com/video.mp4")
			.contentType(ContentType.VIDEO)
			.build();
		return postService.save(post);
	}

	/**
	 * 간단한 해시태그 테스트용 리스트를 반환합니다.
	 */
	public List<String> getSampleTags() {
		return List.of("test", "sample");
	}

	@Transactional
	public void breakPostUserReference(Long userId) {
		List<Feeds> feeds = feedRepository.findByUserId(userId);
		if (!feeds.isEmpty()) {
			Feeds feed = feeds.get(0);
			Posts post = feed.getPost();
			post.setUser(null); // 여기서 session 살아있음
			feed.setPost(post);
			feedRepository.save(feed);
		}
	}

	public List<FollowDto> getFollowings(Long userId) {
		List<Follows> follows = followRepository.findAllByFollowerId(userId);
		return follows.stream()
			.map(FollowMapper::toSimpleDto)
			.toList();
	}

}
