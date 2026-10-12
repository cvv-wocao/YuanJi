<template>
  <view class="post-detail-container">
    <!-- 顶部导航栏 -->
    <view class="nav-header" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="nav-back" @click="goBack">
        <text class="nav-icon">←</text>
      </view>
      <view class="nav-title-wrapper">
        <text class="nav-title">帖子详情</text>
      </view>
      <view v-if="isOwner" class="nav-more" @click="showMoreOptions = true">
        <text class="nav-icon">⋯</text>
      </view>
      <view v-else class="nav-placeholder"></view>
    </view>

    <!-- 主内容区域 -->
    <scroll-view
      class="content-scroll"
      scroll-y
      :style="{ height: scrollHeight }"
      @scroll="onScroll"
      :scroll-top="scrollTop"
    >
      <view v-if="loading" class="loading-state">
        <view class="loading-pulse"></view>
        <text class="loading-text">加载中...</text>
      </view>

      <template v-else-if="post">
        <!-- 作者信息卡片 -->
        <view class="author-card">
          <view class="author-main" @click="goToUserProfile(post.userId)">
            <view class="author-avatar">
              <image v-if="post.user?.avatar" :src="post.user.avatar" mode="aspectFill" />
              <view v-else class="avatar-fallback">{{ post.user?.name?.charAt(0) || '?' }}</view>
            </view>
            <view class="author-info">
              <text class="author-name">{{ post.user?.name || '用户' }}</text>
              <text class="author-meta">{{ formatTime(post.createTime) }}</text>
            </view>
          </view>
          <button
            v-if="!isOwner"
            class="follow-btn"
            :class="{ 'is-following': isFollowing }"
            @click="toggleFollow"
          >
            {{ isFollowing ? '已关注' : '关注' }}
          </button>
        </view>

        <!-- 帖子内容区 -->
        <view class="post-content">
          <text class="post-text">{{ post.content }}</text>

          <!-- 图片展示 -->
          <view v-if="post.images && post.images.length > 0" class="media-section">
            <!-- 单张图片 -->
            <view v-if="post.images.length === 1" class="single-image-wrapper">
              <image
                :src="post.images[0]"
                mode="widthFix"
                class="single-image"
                @click="previewImage(post.images[0], 0)"
              />
            </view>

            <!-- 两张图片 -->
            <view v-else-if="post.images.length === 2" class="double-image-grid">
              <image
                v-for="(img, index) in post.images"
                :key="index"
                :src="img"
                mode="aspectFill"
                class="grid-image"
                @click="previewImage(img, index)"
              />
            </view>

            <!-- 三张及以上图片 -->
            <view v-else class="multi-image-grid">
              <image
                v-for="(img, index) in post.images.slice(0, 9)"
                :key="index"
                :src="img"
                mode="aspectFill"
                class="grid-image"
                :class="{ 'large': index === 0 && post.images.length >= 3 }"
                @click="previewImage(img, index)"
              />
              <view v-if="post.images.length > 9" class="more-overlay" @click="previewAllImages">
                <text class="more-count">+{{ post.images.length - 9 }}</text>
              </view>
            </view>
          </view>

          <!-- 视频展示 -->
          <view v-if="post.videos && post.videos.length > 0" class="media-section">
            <view
              v-for="(video, index) in post.videos"
              :key="index"
              class="video-wrapper"
            >
              <video
                :src="video"
                :poster="post.cover || (post.images && post.images[0])"
                :controls="true"
                :show-center-play-btn="true"
                object-fit="contain"
                class="video-player"
              />
            </view>
          </view>
        </view>

        <!-- 互动数据条 -->
        <view class="engagement-bar">
          <view class="engagement-item" :class="{ active: post.isLiked }" @click="handleLike">
            <view class="engagement-icon">
              <text>{{ post.isLiked ? '❤️' : '🤍' }}</text>
            </view>
            <text class="engagement-count">{{ formatNumber(post.likeCount) }}</text>
          </view>
          <view class="engagement-item" @click="scrollToComments">
            <view class="engagement-icon">
              <text>💬</text>
            </view>
            <text class="engagement-count">{{ formatNumber(post.commentCount) }}</text>
          </view>
          <view class="engagement-item" @click="handleShare">
            <view class="engagement-icon">
              <text>↗️</text>
            </view>
            <text class="engagement-count">分享</text>
          </view>
          <view class="post-time-detail">
            <text>{{ formatAbsoluteDate(post.createTime) }}</text>
          </view>
        </view>

        <!-- 评论区 -->
        <view class="comments-section" id="commentsSection">
          <view class="section-header">
            <view class="section-title">
              <text class="title-text">评论</text>
              <text class="title-count">{{ comments.length }}</text>
            </view>
            <view class="sort-tabs">
              <text
                class="sort-tab"
                :class="{ active: commentSort === 'hot' }"
                @click="changeSort('hot')"
              >最热</text>
              <text
                class="sort-tab"
                :class="{ active: commentSort === 'new' }"
                @click="changeSort('new')"
              >最新</text>
            </view>
          </view>

          <!-- 评论列表 -->
          <view v-if="sortedComments.length > 0" class="comments-list">
            <view
              v-for="(comment, index) in sortedComments"
              :key="comment.id"
              class="comment-item"
              :style="{ animationDelay: index * 50 + 'ms' }"
            >
              <view class="comment-avatar" @click="goToUserProfile(comment.userId)">
                <image v-if="comment.user?.avatar" :src="comment.user.avatar" mode="aspectFill" />
                <view v-else class="avatar-fallback">{{ comment.user?.name?.charAt(0) || '?' }}</view>
              </view>
              <view class="comment-body">
                <view class="comment-header">
                  <text class="comment-author">{{ comment.user?.name || '用户' }}</text>
                  <text v-if="comment.userId === post.userId" class="author-badge">作者</text>
                </view>
                <text class="comment-content">{{ comment.content }}</text>
                <view class="comment-actions">
                  <text class="comment-time">{{ formatTime(comment.createTime) }}</text>
                  <view class="action-group">
                    <text
                      class="action-btn"
                      :class="{ active: comment.isLiked }"
                      @click="handleLikeComment(comment)"
                    >
                      {{ comment.isLiked ? '❤️' : '🤍' }} {{ comment.likeCount || 0 }}
                    </text>
                    <text class="action-btn reply" @click="replyTo(comment)">回复</text>
                    <text
                      v-if="comment.userId === currentUserId"
                      class="action-btn delete"
                      @click="deleteComment(comment.id)"
                    >删除</text>
                  </view>
                </view>

                <!-- 回复列表 -->
                <view v-if="comment.replies && comment.replies.length > 0" class="replies-wrapper">
                  <view
                    v-for="reply in comment.replies.slice(0, 2)"
                    :key="reply.id"
                    class="reply-item"
                  >
                    <text class="reply-author">{{ reply.user?.name || '用户' }}</text>
                    <text class="reply-content">{{ reply.content }}</text>
                  </view>
                  <view
                    v-if="comment.replyCount > 2"
                    class="view-more"
                    @click="expandReplies(comment)"
                  >
                    展开 {{ comment.replyCount - 2 }} 条回复
                  </view>
                </view>
              </view>
            </view>
          </view>

          <!-- 空状态 -->
          <view v-else class="empty-comments">
            <view class="empty-icon">
              <text>💭</text>
            </view>
            <text class="empty-title">还没有评论</text>
            <text class="empty-desc">来发表第一条评论吧</text>
          </view>

          <!-- 底部占位 -->
          <view class="bottom-spacer"></view>
        </view>
      </template>

      <!-- 错误状态 -->
      <view v-else class="error-state">
        <view class="error-icon">⚠️</view>
        <text class="error-title">帖子不存在</text>
        <text class="error-desc">该帖子可能已被删除或不存在</text>
        <button class="back-btn" @click="goBack">返回</button>
      </view>
    </scroll-view>

    <!-- 底部评论输入栏 -->
    <view class="comment-input-wrapper" :class="{ 'keyboard-open': keyboardVisible }">
      <view class="input-bar">
        <view class="input-box">
          <input
            v-model="commentContent"
            :placeholder="replyToUser ? `回复 ${replyToUser}:` : '写评论...'"
            confirm-type="send"
            @confirm="submitComment"
            @focus="onInputFocus"
            @blur="onInputBlur"
            class="comment-input"
          />
          <text v-if="replyToUser" class="cancel-reply" @click="cancelReply">取消</text>
        </view>
        <button
          class="send-btn"
          :disabled="!commentContent.trim() || submitting"
          @click="submitComment"
        >
          <text v-if="!submitting">发送</text>
          <view v-else class="send-loading"></view>
        </button>
      </view>
    </view>

    <!-- 更多操作弹窗 -->
    <view v-if="showMoreOptions" class="action-sheet-overlay" @click="showMoreOptions = false">
      <view class="action-sheet" @click.stop>
        <view class="sheet-header">
          <view class="sheet-indicator"></view>
        </view>
        <button class="sheet-item" @click="editPost">
          <text class="item-icon">✏️</text>
          <text class="item-text">编辑帖子</text>
        </button>
        <button class="sheet-item danger" @click="confirmDeletePost">
          <text class="item-icon">🗑️</text>
          <text class="item-text">删除帖子</text>
        </button>
        <button class="sheet-item cancel" @click="showMoreOptions = false">
          <text class="item-text">取消</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script>
import {
  getPostById,
  getPostComments,
  likePost,
  unlikePost,
  likeComment,
  unlikeComment,
  createComment,
  deleteComment as deleteCommentApi,
  deletePost
} from '@/api/post.js';
import { followUser, unfollowUser, checkFollowing } from '@/api/follow.js';

export default {
  data() {
    return {
      loading: true,
      post: null,
      comments: [],
      commentSort: 'hot',
      currentUserId: null,
      statusBarHeight: 20,
      scrollHeight: '0px',

      // 评论输入
      commentContent: '',
      replyToUser: '',
      replyToId: null,
      submitting: false,

      // 更多操作
      showMoreOptions: false,
      isFollowing: false,

      // 滚动相关
      scrollTop: 0,
      keyboardVisible: false,

      postId: null
    };
  },

  computed: {
    isOwner() {
      return this.post && this.currentUserId && this.post.userId === this.currentUserId;
    },

    sortedComments() {
      let list = [...this.comments];
      if (this.commentSort === 'new') {
        list.sort((a, b) => new Date(b.createTime) - new Date(a.createTime));
      } else {
        list.sort((a, b) => (b.likeCount + (b.replyCount || 0)) - (a.likeCount + (a.replyCount || 0)));
      }
      return list;
    }
  },

  onLoad(options) {
    this.postId = options.id;
    this.currentUserId = uni.getStorageSync('userId') ? parseInt(uni.getStorageSync('userId')) : null;
    this.initSystemInfo();
    this.fetchData();
  },

  onReady() {
    uni.onKeyboardHeightChange(res => {
      this.keyboardVisible = res.height > 0;
      if (res.height > 0) {
        setTimeout(() => this.scrollToBottom(), 100);
      }
    });
  },

  onUnload() {
    uni.offKeyboardHeightChange();
  },

  methods: {
    initSystemInfo() {
      const systemInfo = uni.getSystemInfoSync();
      this.statusBarHeight = systemInfo.statusBarHeight || 20;
      const windowHeight = systemInfo.windowHeight;
      // 减去导航栏和输入栏高度
      this.scrollHeight = (windowHeight - 100 - this.statusBarHeight) + 'px';
    },

    async fetchData() {
      try {
        await Promise.all([this.fetchPostDetail(), this.fetchComments()]);
        this.loading = false;
      } catch (error) {
        console.error('获取数据失败:', error);
        this.loading = false;
      }
    },

    async fetchPostDetail() {
      try {
        const result = await getPostById(this.postId);
        if (result.success) {
          this.post = result.data;
          await this.checkFollowStatus();
        }
      } catch (error) {
        console.error('获取帖子详情失败:', error);
      }
    },

    async fetchComments() {
      try {
        const result = await getPostComments(this.postId);
        if (result.success) {
          this.comments = result.data || [];
        }
      } catch (error) {
        console.error('获取评论失败:', error);
      }
    },

    async handleLike() {
      if (!this.post) return;
      try {
        const result = this.post.isLiked
          ? await unlikePost(this.post.id)
          : await likePost(this.post.id);

        if (result.success) {
          this.post.isLiked = !this.post.isLiked;
          this.post.likeCount = (this.post.likeCount || 0) + (this.post.isLiked ? 1 : -1);
        }
      } catch (error) {
        console.error('点赞失败:', error);
        uni.showToast({ title: '操作失败', icon: 'none' });
      }
    },

    async submitComment() {
      if (!this.commentContent.trim() || this.submitting) return;

      this.submitting = true;
      try {
        const result = await createComment(
          this.postId,
          this.commentContent.trim(),
          this.replyToId
        );

        if (result.success) {
          this.commentContent = '';
          this.replyToUser = '';
          this.replyToId = null;
          await this.fetchComments();
          if (this.post) {
            this.post.commentCount = (this.post.commentCount || 0) + 1;
          }

          setTimeout(() => this.scrollToBottom(), 300);
          uni.showToast({ title: '评论成功', icon: 'success' });
        } else {
          uni.showToast({ title: result.message || '评论失败', icon: 'none' });
        }
      } catch (error) {
        console.error('评论失败:', error);
        uni.showToast({ title: '评论失败，请重试', icon: 'none' });
      } finally {
        this.submitting = false;
      }
    },

    scrollToComments() {
      this.scrollTop = 99999;
      setTimeout(() => this.scrollTop = 0, 500);
    },

    scrollToBottom() {
      this.scrollTop = 99999;
      setTimeout(() => this.scrollTop = 0, 500);
    },

    onScroll() {},

    onInputFocus() {
      setTimeout(() => this.scrollToBottom(), 100);
    },

    onInputBlur() {
      this.keyboardVisible = false;
    },

    changeSort(type) {
      this.commentSort = type;
    },

    replyTo(comment) {
      this.replyToUser = comment.user?.name || '用户';
      this.replyToId = comment.id;
    },

    cancelReply() {
      this.replyToUser = '';
      this.replyToId = null;
    },

    expandReplies(comment) {
      console.log('展开更多回复', comment);
    },

    async deleteComment(commentId) {
      uni.showModal({
        title: '删除评论',
        content: '确定要删除这条评论吗？',
        confirmColor: '#ef4444',
        success: async (res) => {
          if (res.confirm) {
            try {
              const result = await deleteCommentApi(commentId);
              if (result.success) {
                this.comments = this.comments.filter(c => c.id !== commentId);
                if (this.post) {
                  this.post.commentCount = Math.max(0, (this.post.commentCount || 0) - 1);
                }
                uni.showToast({ title: '删除成功', icon: 'success' });
              } else {
                uni.showToast({ title: result.message || '删除失败', icon: 'none' });
              }
            } catch (error) {
              console.error('删除评论失败:', error);
              uni.showToast({ title: '删除失败', icon: 'none' });
            }
          }
        }
      });
    },

    async handleLikeComment(comment) {
      try {
        const result = comment.isLiked
          ? await unlikeComment(comment.id)
          : await likeComment(comment.id);

        if (result.success) {
          comment.isLiked = !comment.isLiked;
          comment.likeCount = (comment.likeCount || 0) + (comment.isLiked ? 1 : -1);
        }
      } catch (error) {
        console.error('评论点赞失败:', error);
        uni.showToast({ title: '操作失败', icon: 'none' });
      }
    },

    editPost() {
      this.showMoreOptions = false;
      uni.navigateTo({
        url: `/pages/post/edit?id=${this.postId}`
      });
    },

    async confirmDeletePost() {
      this.showMoreOptions = false;
      uni.showModal({
        title: '删除帖子',
        content: '确定要删除这条帖子吗？此操作不可恢复。',
        confirmColor: '#ef4444',
        success: async (res) => {
          if (res.confirm) {
            try {
              const result = await deletePost(this.postId);
              if (result.success) {
                uni.showToast({ title: '删除成功', icon: 'success' });
                setTimeout(() => uni.navigateBack(), 1500);
              } else {
                uni.showToast({ title: result.message || '删除失败', icon: 'none' });
              }
            } catch (error) {
              console.error('删除帖子失败:', error);
              uni.showToast({ title: '删除失败', icon: 'none' });
            }
          }
        }
      });
    },

    handleShare() {
      uni.showShareMenu({ withShareTicket: true });
    },

    async toggleFollow() {
      if (!this.post || !this.post.userId) {
        uni.showToast({ title: '无法获取用户信息', icon: 'none' });
        return;
      }

      const targetUserId = this.post.userId;

      try {
        if (this.isFollowing) {
          const result = await unfollowUser(targetUserId);
          if (result.success) {
            this.isFollowing = false;
            uni.showToast({ title: '已取消关注', icon: 'success' });
          }
        } else {
          const result = await followUser(targetUserId);
          if (result.success) {
            this.isFollowing = true;
            uni.showToast({ title: '关注成功', icon: 'success' });
          }
        }
      } catch (error) {
        console.error('关注操作失败:', error);
        uni.showToast({ title: '操作失败', icon: 'none' });
        await this.checkFollowStatus();
      }
    },

    async checkFollowStatus() {
      if (!this.post || !this.post.userId) return;
      try {
        const result = await checkFollowing(this.post.userId);
        if (result.success) {
          this.isFollowing = result.data === true;
        }
      } catch (error) {
        console.error('检查关注状态失败:', error);
      }
    },

    previewImage(url, index) {
      const urls = this.post.images || [];
      uni.previewImage({ urls, current: index });
    },

    previewAllImages() {
      const urls = this.post.images || [];
      uni.previewImage({ urls, current: 0 });
    },

    goToUserProfile(userId) {
      uni.navigateTo({ url: `/pages/user/user?id=${userId}` });
    },

    goBack() {
      uni.navigateBack();
    },

    formatTime(timeStr) {
      if (!timeStr) return '';
      const date = new Date(timeStr);
      const now = new Date();
      const diff = now - date;

      const minutes = Math.floor(diff / 60000);
      const hours = Math.floor(diff / 3600000);
      const days = Math.floor(diff / 86400000);

      if (minutes < 1) return '刚刚';
      if (minutes < 60) return `${minutes}分钟前`;
      if (hours < 24) return `${hours}小时前`;
      if (days < 7) return `${days}天前`;
      return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' });
    },

    formatAbsoluteDate(timeStr) {
      if (!timeStr) return '';
      const date = new Date(timeStr);
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    },

    formatNumber(num) {
      if (!num) return '0';
      if (num >= 10000) {
        return (num / 10000).toFixed(1) + 'w';
      }
      if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'k';
      }
      return num.toString();
    }
  }
};
</script>

<style scoped>
.post-detail-container {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(120, 149, 255, 0.18) 0, rgba(120, 149, 255, 0) 28%),
    radial-gradient(circle at top right, rgba(69, 196, 176, 0.16) 0, rgba(69, 196, 176, 0) 26%),
    linear-gradient(180deg, #f4f7fd 0%, #edf3fb 44%, #f7f9fc 100%);
  display: flex;
  flex-direction: column;
}

/* 导航栏 */
.nav-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(244, 247, 253, 0.78);
  backdrop-filter: blur(26px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16rpx 24rpx 14rpx;
  border-bottom: 1rpx solid rgba(135, 155, 185, 0.12);
}

.nav-back,
.nav-more,
.nav-placeholder {
  width: 72rpx;
  height: 72rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-back {
  background: rgba(255, 255, 255, 0.82);
  border-radius: 24rpx;
  box-shadow: 0 10rpx 22rpx rgba(58, 83, 120, 0.08);
  transition: all 0.2s ease;
}

.nav-back:active {
  background: rgba(255, 255, 255, 0.95);
  transform: scale(0.95);
}

.nav-icon {
  font-size: 36rpx;
  color: #22314d;
  font-weight: 700;
}

.nav-title-wrapper {
  flex: 1;
  text-align: center;
}

.nav-title {
  font-size: 32rpx;
  font-weight: 800;
  color: #16233a;
  letter-spacing: 1rpx;
}

/* 内容滚动区 */
.content-scroll {
  flex: 1;
  padding-bottom: 12rpx;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120rpx 40rpx;
}

.loading-pulse {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #4f46e5 0%, #14b8a6 100%);
  animation: pulse 1.5s ease-in-out infinite;
  margin-bottom: 24rpx;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.1); opacity: 0.7; }
}

.loading-text {
  font-size: 28rpx;
  color: #94a3b8;
}

/* 作者卡片 */
.author-card {
  background: rgba(255, 255, 255, 0.92);
  margin: 28rpx 24rpx 18rpx;
  padding: 24rpx 24rpx 24rpx 22rpx;
  border-radius: 30rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1rpx solid rgba(255, 255, 255, 0.72);
  box-shadow: 0 18rpx 34rpx rgba(73, 95, 128, 0.08);
}

.author-main {
  display: flex;
  align-items: center;
  flex: 1;
}

.author-avatar {
  width: 92rpx;
  height: 92rpx;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #4f46e5 0%, #14b8a6 100%);
  margin-right: 22rpx;
  border: 4rpx solid rgba(255, 255, 255, 0.95);
  box-shadow: 0 10rpx 18rpx rgba(79, 70, 229, 0.18);
  flex-shrink: 0;
}

.author-avatar image {
  width: 100%;
  height: 100%;
}

.avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 36rpx;
  font-weight: 600;
}

.author-info {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 32rpx;
  font-weight: 800;
  color: #152238;
  margin-bottom: 8rpx;
}

.author-meta {
  font-size: 24rpx;
  color: #7f8ca2;
}

.follow-btn {
  min-width: 132rpx;
  height: 86rpx;
  padding: 0 28rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #4f46e5 0%, #14b8a6 100%);
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 700;
  border: none;
  box-shadow: 0 14rpx 28rpx rgba(54, 114, 204, 0.24);
  transition: all 0.2s ease;
}

.follow-btn.is-following {
  background: linear-gradient(180deg, #f7f9fc 0%, #eef3f8 100%);
  color: #66778f;
  box-shadow: none;
}

.follow-btn:active {
  transform: scale(0.95);
  opacity: 0.9;
}

/* 帖子内容 */
.post-content {
  background: rgba(255, 255, 255, 0.94);
  margin: 0 24rpx 18rpx;
  padding: 34rpx 32rpx;
  border-radius: 30rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 18rpx 34rpx rgba(73, 95, 128, 0.08);
}

.post-text {
  font-size: 35rpx;
  line-height: 1.8;
  color: #18243c;
  font-weight: 600;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 媒体区域 */
.media-section {
  margin-top: 28rpx;
}

.single-image-wrapper {
  border-radius: 24rpx;
  overflow: hidden;
  background: #eff4fb;
  box-shadow: 0 12rpx 24rpx rgba(47, 72, 105, 0.08);
}

.single-image {
  width: 100%;
  display: block;
  border-radius: 24rpx;
}

.double-image-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
  border-radius: 24rpx;
  overflow: hidden;
}

.multi-image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  border-radius: 24rpx;
  overflow: hidden;
  position: relative;
}

.grid-image {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 18rpx;
  box-shadow: 0 10rpx 18rpx rgba(47, 72, 105, 0.08);
}

.grid-image.large {
  grid-column: span 2;
  grid-row: span 2;
  aspect-ratio: auto;
}

.more-overlay {
  position: absolute;
  right: 0;
  bottom: 0;
  width: calc(33.33% - 9rpx);
  aspect-ratio: 1;
  background: linear-gradient(180deg, rgba(16, 23, 42, 0.28) 0%, rgba(16, 23, 42, 0.72) 100%);
  border-radius: 18rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.more-count {
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 600;
}

.video-wrapper {
  border-radius: 24rpx;
  overflow: hidden;
  background: #000;
  margin-bottom: 16rpx;
  box-shadow: 0 12rpx 24rpx rgba(24, 36, 60, 0.14);
}

.video-player {
  width: 100%;
  height: 400rpx;
}

/* 互动数据条 */
.engagement-bar {
  background: rgba(255, 255, 255, 0.94);
  margin: 0 24rpx 18rpx;
  padding: 20rpx 24rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
  flex-wrap: wrap;
  border: 1rpx solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 18rpx 34rpx rgba(73, 95, 128, 0.08);
}

.engagement-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-height: 68rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #f5f7fb;
  transition: all 0.2s ease;
}

.engagement-item:active {
  transform: scale(0.95);
  opacity: 0.8;
}

.engagement-item.active {
  background: rgba(255, 107, 129, 0.1);
}

.engagement-item.active .engagement-count {
  color: #ef4444;
}

.engagement-icon {
  font-size: 34rpx;
}

.engagement-count {
  font-size: 26rpx;
  color: #425169;
  font-weight: 700;
}

.post-time-detail {
  margin-left: auto;
  min-height: 68rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: linear-gradient(180deg, #f7f9fc 0%, #eef3f8 100%);
  display: flex;
  align-items: center;
  font-size: 23rpx;
  color: #7f8ca2;
}

/* 评论区 */
.comments-section {
  background: rgba(255, 255, 255, 0.96);
  margin: 0 24rpx 0;
  border-radius: 30rpx 30rpx 0 0;
  padding: 32rpx;
  min-height: 440rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.82);
  box-shadow: 0 18rpx 34rpx rgba(73, 95, 128, 0.08);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28rpx;
  padding-bottom: 22rpx;
  border-bottom: 1rpx solid #eef3f8;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.title-text {
  font-size: 32rpx;
  font-weight: 800;
  color: #18243c;
}

.title-count {
  font-size: 24rpx;
  color: #7b89a0;
  background: #eef3fa;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
}

.sort-tabs {
  display: flex;
  gap: 12rpx;
}

.sort-tab {
  font-size: 24rpx;
  color: #8694a8;
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  background: #f6f8fb;
  transition: all 0.2s ease;
}

.sort-tab.active {
  color: #315ea8;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.12) 0%, rgba(20, 184, 166, 0.14) 100%);
  font-weight: 700;
}

/* 评论列表 */
.comments-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.comment-item {
  display: flex;
  gap: 18rpx;
  padding: 22rpx;
  border-radius: 24rpx;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcfe 100%);
  border: 1rpx solid #edf2f8;
  box-shadow: 0 12rpx 24rpx rgba(72, 92, 122, 0.05);
  animation: slideIn 0.4s ease forwards;
  opacity: 0;
  transform: translateY(20rpx);
}

@keyframes slideIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.comment-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #4f46e5 0%, #14b8a6 100%);
  border: 3rpx solid rgba(255, 255, 255, 0.96);
  box-shadow: 0 10rpx 18rpx rgba(79, 70, 229, 0.16);
  flex-shrink: 0;
}

.comment-avatar image {
  width: 100%;
  height: 100%;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 10rpx;
}

.comment-author {
  font-size: 28rpx;
  font-weight: 700;
  color: #16233a;
}

.author-badge {
  font-size: 20rpx;
  color: #315ea8;
  background: rgba(79, 70, 229, 0.1);
  padding: 5rpx 12rpx;
  border-radius: 999rpx;
  font-weight: 700;
}

.comment-content {
  font-size: 28rpx;
  line-height: 1.6;
  color: #415067;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
}

.comment-time {
  font-size: 24rpx;
  color: #8e99ac;
}

.action-group {
  display: flex;
  gap: 10rpx;
  flex-wrap: wrap;
}

.action-btn {
  font-size: 22rpx;
  color: #66778f;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #f5f7fb;
  transition: all 0.2s ease;
}

.action-btn:active {
  background: #ecf1f7;
}

.action-btn.active {
  color: #ef4444;
}

.action-btn.reply {
  color: #4f46e5;
}

.action-btn.delete {
  color: #ef4444;
}

/* 回复列表 */
.replies-wrapper {
  background: linear-gradient(180deg, #f7f9fc 0%, #f1f5fa 100%);
  border-radius: 18rpx;
  padding: 18rpx;
  margin-top: 18rpx;
}

.reply-item {
  font-size: 26rpx;
  line-height: 1.5;
  margin-bottom: 12rpx;
}

.reply-item:last-child {
  margin-bottom: 0;
}

.reply-author {
  color: #4f46e5;
  font-weight: 600;
  margin-right: 8rpx;
}

.reply-content {
  color: #475569;
}

.view-more {
  font-size: 24rpx;
  color: #315ea8;
  font-weight: 700;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1rpx solid #e2e8f0;
}

/* 空评论状态 */
.empty-comments {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 90rpx 40rpx;
}

.empty-icon {
  width: 132rpx;
  height: 132rpx;
  background: linear-gradient(135deg, rgba(79, 70, 229, 0.12) 0%, rgba(20, 184, 166, 0.12) 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  margin-bottom: 24rpx;
  box-shadow: inset 0 0 0 1rpx rgba(255, 255, 255, 0.8);
}

.empty-title {
  font-size: 32rpx;
  font-weight: 800;
  color: #18243c;
  margin-bottom: 8rpx;
}

.empty-desc {
  font-size: 26rpx;
  color: #8c98ab;
}

.bottom-spacer {
  height: 160rpx;
}

/* 错误状态 */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 160rpx 40rpx;
}

.error-icon {
  font-size: 80rpx;
  margin-bottom: 24rpx;
}

.error-title {
  font-size: 36rpx;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 12rpx;
}

.error-desc {
  font-size: 28rpx;
  color: #94a3b8;
  margin-bottom: 48rpx;
}

.back-btn {
  padding: 24rpx 64rpx;
  background: linear-gradient(135deg, #4f46e5 0%, #14b8a6 100%);
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  border-radius: 40rpx;
  border: none;
}

/* 评论输入栏 */
.comment-input-wrapper {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(248, 250, 253, 0.86);
  border-top: 1rpx solid rgba(214, 224, 236, 0.7);
  backdrop-filter: blur(26rpx);
  padding: 16rpx 20rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  z-index: 1000;
}

.input-bar {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.input-box {
  flex: 1;
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.94);
  border-radius: 999rpx;
  padding: 0 24rpx;
  height: 84rpx;
  border: 1rpx solid rgba(219, 229, 238, 0.9);
  box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.9);
}

.comment-input {
  flex: 1;
  height: 100%;
  font-size: 28rpx;
  color: #1d2a42;
  background: transparent;
  border: none;
}

.cancel-reply {
  font-size: 24rpx;
  color: #315ea8;
  padding: 8rpx 14rpx;
  white-space: nowrap;
}

.send-btn {
  width: 124rpx;
  height: 84rpx;
  background: linear-gradient(135deg, #4f46e5 0%, #14b8a6 100%);
  border-radius: 999rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  box-shadow: 0 14rpx 26rpx rgba(49, 94, 168, 0.24);
  transition: all 0.2s ease;
}

.send-btn text {
  color: #ffffff;
  font-size: 27rpx;
  font-weight: 700;
}

.send-btn:disabled {
  opacity: 0.5;
}

.send-loading {
  width: 32rpx;
  height: 32rpx;
  border: 3rpx solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 操作菜单 */
.action-sheet-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8rpx);
  z-index: 2000;
  display: flex;
  align-items: flex-end;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.action-sheet {
  width: 100%;
  background: #ffffff;
  border-radius: 40rpx 40rpx 0 0;
  padding-bottom: env(safe-area-inset-bottom);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.sheet-header {
  display: flex;
  justify-content: center;
  padding: 20rpx 0;
}

.sheet-indicator {
  width: 80rpx;
  height: 8rpx;
  background: #e2e8f0;
  border-radius: 4rpx;
}

.sheet-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  width: 100%;
  padding: 32rpx 40rpx;
  background: none;
  border: none;
  border-bottom: 1rpx solid #f1f5f9;
}

.sheet-item:last-child {
  border-bottom: none;
}

.sheet-item.cancel {
  justify-content: center;
  margin-top: 16rpx;
  background: #f8fafc;
}

.sheet-item.cancel .item-text {
  color: #64748b;
  font-weight: 600;
}

.sheet-item.danger .item-text {
  color: #ef4444;
}

.item-icon {
  font-size: 40rpx;
}

.item-text {
  font-size: 32rpx;
  color: #1a1a1a;
  font-weight: 500;
}
</style>
