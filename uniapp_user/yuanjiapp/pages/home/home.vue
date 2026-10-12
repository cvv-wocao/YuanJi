<template>
  <view class="mobile-container">
    <!-- 顶部标题栏 -->
    <view class="app-header glass-card" :style="{ paddingTop: statusBarHeight + 'px' }">
      <text class="header-title text-gradient">远疾 · 发现</text>
    </view>

    <!-- 发布区域 -->
    <view class="publish-container reveal-anim">
      <view class="publish-card glass-card" @click="showPublishModal = true">
        <view class="publish-head">
          <view class="avatar-small-refined">{{ userName.charAt(0) }}</view>
          <view class="publish-entry">
            <text class="publish-placeholder">分享你的运动瞬间...</text>
            <text class="publish-hint">记录今天的训练、生活或一点心情</text>
          </view>
          <button class="publish-btn-refined" @click.stop="showPublishModal = true">发布</button>
        </view>
        <view class="publish-actions">
          <view class="action-item action-item-image" @click.stop="showPublishModal = true">
            <text class="action-glyph">图</text>
            <text>图片</text>
          </view>
          <view class="action-item action-item-video" @click.stop="showPublishModal = true">
            <text class="action-glyph">视</text>
            <text>视频</text>
          </view>
          <view class="action-item action-item-mood" @click.stop="showPublishModal = true">
            <text class="action-glyph">心</text>
            <text>心情</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 帖子滚动区域 -->
    <scroll-view class="posts-scroll" 
      scroll-y 
      :style="{ height: scrollHeight }"
      @scrolltolower="loadMorePosts"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >

      <view class="post-list" v-if="posts.length > 0">
        <view v-for="post in posts" :key="post.id" class="post-card" @click="goToPostDetail(post.id)">
          <!-- 帖子头部 -->
          <view class="post-header">
            <view class="post-avatar">
              <image v-if="getPostAvatar(post)" :src="getPostAvatar(post)" mode="aspectFill"></image>
              <view v-else class="avatar-default">{{ getPostUserInitial(post) }}</view>
            </view>
            <view class="post-info">
              <text class="post-username">{{ getPostUserName(post) }}</text>
              <text class="post-time">{{ formatTime(post.createTime) }}</text>
            </view>
          </view>

          <!-- 帖子内容 -->
          <view class="post-content">
            <text class="post-text">{{ post.content }}</text>
            
            <!-- 封面图片 -->
            <view v-if="post.cover" class="post-cover">
              <image :src="post.cover" mode="widthFix" class="post-media-image"></image>
              <view v-if="post.videos && post.videos.length > 0" class="video-badge">
                <text class="video-icon">▶</text>
                <text class="video-count">{{ post.videos.length }}</text>
              </view>
            </view>
            
            <!-- 图片列表 -->
            <view v-else-if="post.images && post.images.length" class="post-images">
              <view class="image-grid">
                <image :src="post.images[0]" mode="widthFix" class="post-media-image" />
                <view v-if="post.images.length > 1" class="image-count">+{{ post.images.length - 1 }}</view>
              </view>
            </view>
            
            <!-- 视频列表 -->
            <view v-else-if="post.videos && post.videos.length" class="post-video">
              <image
                :src="post.videoCover || getVideoCover(post.videos[0])"
                mode="widthFix"
                class="post-media-image"
              />
              <view class="play-icon">▶</view>
            </view>
          </view>
        </view>
      </view>

      <!-- 空状态 -->
      <view v-else class="empty-state reveal-anim">
        <view class="empty-icon-wrapper">
          <text class="empty-glyph">发</text>
        </view>
        <text class="empty-text">还没有动态，来发布第一条吧</text>
      </view>

      <!-- 加载更多 -->
      <view v-if="posts.length > 0 && hasMore" class="load-more">
        <text class="loading-text">{{ loading ? '加载中...' : '上拉加载更多' }}</text>
      </view>
      
      <!-- 底部留白 -->
      <view class="bottom-space"></view>
    </scroll-view>

    <!-- 发布帖子弹窗 -->
    <view v-if="showPublishModal" class="modal-overlay" @click="showPublishModal = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">发布帖子</text>
          <button class="close-btn" @click="showPublishModal = false">✕</button>
        </view>
        <view class="modal-body">
          <textarea
            v-model="newPostContent"
            placeholder="分享你的新鲜事..."
            :maxlength="500"
            class="post-textarea"
          />
          <text class="char-count">{{ newPostContent.length }}/500</text>

          <!-- 图片预览 -->
          <view v-if="selectedImages.length > 0" class="image-preview">
            <view v-for="(img, index) in selectedImages" :key="index" class="preview-item">
              <image :src="img" mode="aspectFill"></image>
              <button class="remove-btn" @click.stop="removeImage(index)">✕</button>
            </view>
          </view>

          <!-- 视频预览 -->
          <view v-if="selectedVideos.length > 0" class="video-preview">
            <view v-for="(video, index) in selectedVideos" :key="index" class="preview-item video-item">
              <video :src="video" :controls="false" :show-center-play-btn="true" :show-play-btn="true" object-fit="cover"></video>
              <button class="remove-btn" @click.stop="removeVideo(index)">✕</button>
            </view>
          </view>

          <!-- 媒体选择 -->
          <view class="media-actions">
            <button class="media-btn-refined image-btn" @click="chooseImages">
              <text class="btn-glyph">图</text>
              <text>图片 ({{ selectedImages.length }}/9)</text>
            </button>
            <button class="media-btn-refined video-btn" @click="chooseVideos">
              <text class="btn-glyph">视</text>
              <text>视频 ({{ selectedVideos.length }}/3)</text>
            </button>
          </view>
        </view>
        <view class="modal-footer">
          <button class="cancel-btn" @click="showPublishModal = false">取消</button>
          <button
            class="submit-btn"
            :disabled="(!newPostContent.trim() && selectedImages.length === 0 && selectedVideos.length === 0) || publishing"
            @click="submitPost"
          >
            {{ publishing ? '发布中...' : '发布' }}
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { getPostList, getRecommendedPosts, createPost, uploadFile } from '@/api/post.js';
import { formatTime, showToast, showLoading, hideLoading, chooseImage } from '@/utils/index.js';

// 数据
const posts = ref([]);
const page = ref(1);
const size = 10;
const hasMore = ref(true);
const loading = ref(false);
const refreshing = ref(false);
const userName = ref('访客');
const scrollHeight = ref('0px');
const statusBarHeight = ref(20);
const isLoggedIn = ref(false); // 新增：登录状态

// 发布相关
const showPublishModal = ref(false);
const newPostContent = ref('');
const selectedImages = ref([]);
const selectedVideos = ref([]);
const selectedFiles = ref([]);
const publishing = ref(false);
let resizeHandler = null;

// 计算滚动区域高度
const initScrollHeight = () => {
  const systemInfo = uni.getSystemInfoSync();
  const windowHeight = systemInfo.windowHeight;
  statusBarHeight.value = systemInfo.statusBarHeight || 20;
  
  nextTick(() => {
    const query = uni.createSelectorQuery();
    
    query.select('.app-header').boundingClientRect();
    query.select('.publish-card').boundingClientRect();
    
    query.exec((res) => {
      const headerRect = res && res[0] ? res[0] : null;
      const publishRect = res && res[1] ? res[1] : null;
      const headerHeight = headerRect && headerRect.height ? headerRect.height : 60;
      const publishCardHeight = publishRect && publishRect.height ? publishRect.height : 110;
      
      const height = windowHeight - headerHeight - publishCardHeight;
      scrollHeight.value = `${height}px`;
      
      console.log('滚动区域高度计算:', {
        windowHeight,
        headerHeight,
        publishCardHeight,
        scrollHeight: height
      });
    });
  });
};

// 获取帖子列表（根据登录状态选择接口）
const fetchPosts = async (isLoadMore = false) => {
  if (loading.value) return;
  loading.value = true;

  try {
    let result;
    
    // 根据登录状态选择不同的接口
    if (isLoggedIn.value) {
      // 登录用户：先尝试获取推荐帖子
      try {
        result = await getRecommendedPosts(page.value, size);
        // 如果推荐接口返回成功，则直接使用
        if (result.success) {
          console.log('获取推荐帖子成功');
        } else {
          // 推荐接口返回失败，回退到普通接口
          console.log('推荐接口返回失败，回退到普通接口');
          result = await getPostList(page.value, size);
        }
      } catch (error) {
        // 推荐接口调用异常，回退到普通接口
        console.error('推荐接口调用异常，回退到普通接口:', error);
        result = await getPostList(page.value, size);
      }
    } else {
      // 未登录用户：获取普通帖子列表
      result = await getPostList(page.value, size);
    }

    if (result.success) {
      const responseData = result.data || {};
      let postList = [];
      
      // 处理不同的数据结构
      if (Array.isArray(responseData)) {
        postList = responseData;
      } else if (responseData.posts) {
        postList = responseData.posts;
      } else if (responseData.list) {
        postList = responseData.list;
      } else {
        postList = responseData;
      }

      if (isLoadMore) {
        posts.value.push(...postList);
      } else {
        posts.value = postList;
      }

      // 判断是否还有更多数据
      hasMore.value = postList.length === size;
      
      console.log(`获取${isLoggedIn.value ? '推荐' : '普通'}帖子列表:`, {
        page: page.value,
        size: size,
        count: postList.length,
        hasMore: hasMore.value
      });
    } else {
      showToast(result.message || '获取帖子失败');
    }
  } catch (error) {
    console.error('获取帖子失败:', error);
    showToast('获取帖子失败');
  } finally {
    loading.value = false;
    refreshing.value = false;
  }
};

// 加载更多
const loadMorePosts = () => {
  if (!hasMore.value || loading.value) return;
  page.value++;
  fetchPosts(true);
};

// 下拉刷新
const onRefresh = () => {
  refreshing.value = true;
  page.value = 1;
  fetchPosts(false);
};

// 选择图片
const chooseImages = async () => {
  try {
    const remainingSlots = 9 - selectedImages.value.length;
    if (remainingSlots <= 0) {
      showToast('最多只能选择9张图片');
      return;
    }
    
    const tempFilePaths = await chooseImage({
      count: remainingSlots,
      sizeType: ['compressed']
    });
    
    selectedImages.value.push(...tempFilePaths);
    selectedFiles.value.push(...tempFilePaths);
  } catch (error) {
    console.error('选择图片失败:', error);
  }
};

// 移除图片
const removeImage = (index) => {
  selectedImages.value.splice(index, 1);
  selectedFiles.value.splice(index, 1);
};

// 选择视频
const chooseVideos = async () => {
  try {
    const remainingSlots = 3 - selectedVideos.value.length;
    if (remainingSlots <= 0) {
      showToast('最多只能选择3个视频');
      return;
    }

    const res = await new Promise((resolve, reject) => {
      uni.chooseVideo({
        sourceType: ['album', 'camera'],
        maxDuration: 60,
        camera: 'back',
        success: (data) => {
          resolve(data);
        },
        fail: (err) => {
          reject(err);
        }
      });
    });

    if (res.tempFilePath) {
      selectedVideos.value.push(res.tempFilePath);
      selectedFiles.value.push(res.tempFilePath);
    }
  } catch (error) {
    console.error('选择视频失败:', error);
    showToast('选择视频失败');
  }
};

// 移除视频
const removeVideo = (index) => {
  selectedVideos.value.splice(index, 1);
  selectedFiles.value.splice(index, 1);
};

// 获取视频封面
const getVideoCover = (videoUrl) => {
  return 'https://via.placeholder.com/300x200?text=Video';
};

const getPostUser = (post) => {
  return post && post.user ? post.user : {};
};

const getPostAvatar = (post) => {
  const user = getPostUser(post);
  return user.avatar || '';
};

const getPostUserName = (post) => {
  const user = getPostUser(post);
  return user.name || '用户';
};

const getPostUserInitial = (post) => {
  const name = getPostUserName(post);
  return name ? name.charAt(0) : '?';
};

// 重置表单
const resetForm = () => {
  newPostContent.value = '';
  selectedImages.value = [];
  selectedVideos.value = [];
  selectedFiles.value = [];
  showPublishModal.value = false;
  page.value = 1;
  fetchPosts(false);
};

// 上传单个文件
const uploadSingleFile = async () => {
  let fileUrl;
  if (selectedImages.value.length > 0) {
    fileUrl = await uploadFile(selectedImages.value[0], 'image');
  } else if (selectedVideos.value.length > 0) {
    fileUrl = await uploadFile(selectedVideos.value[0], 'video');
  }

  const postData = {
    content: newPostContent.value.trim()
  };

  if (selectedImages.value.length > 0) {
    postData.images = [fileUrl];
  }

  if (selectedVideos.value.length > 0) {
    postData.videos = [fileUrl];
  }

  const result = await createPost(postData);
  return result;
};

// 上传多个文件
const uploadMultipleFiles = async () => {
  const imageUrls = [];
  const videoUrls = [];

  if (selectedImages.value.length > 0) {
    for (let i = 0; i < selectedImages.value.length; i++) {
      const url = await uploadFile(selectedImages.value[i], 'image');
      imageUrls.push(url);
    }
  }

  if (selectedVideos.value.length > 0) {
    for (let i = 0; i < selectedVideos.value.length; i++) {
      const url = await uploadFile(selectedVideos.value[i], 'video');
      videoUrls.push(url);
    }
  }

  const postData = {
    content: newPostContent.value.trim()
  };

  if (imageUrls.length > 0) {
    postData.images = imageUrls;
  }

  if (videoUrls.length > 0) {
    postData.videos = videoUrls;
  }

  const result = await createPost(postData);
  return result;
};

// 提交帖子
const submitPost = async () => {
  if (!newPostContent.value.trim() && selectedImages.value.length === 0 && selectedVideos.value.length === 0) {
    showToast('请输入内容或选择图片/视频');
    return;
  }

  publishing.value = true;
  showLoading('发布中...');

  try {
    const token = uni.getStorageSync('token');
    if (!token) {
      showToast('请先登录');
      publishing.value = false;
      hideLoading();
      return;
    }

    const hasImages = selectedImages.value.length > 0;
    const hasVideos = selectedVideos.value.length > 0;

    let result;
    if (!hasImages && !hasVideos) {
      result = await createPost({
        content: newPostContent.value.trim()
      });
    } else {
      const totalFiles = selectedImages.value.length + selectedVideos.value.length;
      if (totalFiles === 1) {
        result = await uploadSingleFile();
      } else {
        result = await uploadMultipleFiles();
      }
    }

    if (result.success) {
      showToast('发布成功！');
      resetForm();
    } else {
      showToast(result.message || '发布失败');
    }
  } catch (error) {
    console.error('发布失败:', error);
    showToast('发布失败');
  } finally {
    publishing.value = false;
    hideLoading();
  }
};

const goToPostDetail = (postId) => {
  if (!postId) return;
  
  uni.navigateTo({
    url: `/pages/post/post?id=${postId}`
  });
};

// 跳转到登录页
const goToLogin = () => {
  uni.navigateTo({
    url: '/pages/login/login'
  });
};

// 检查登录状态
const checkLoginStatus = () => {
  const token = uni.getStorageSync('token');
  isLoggedIn.value = !!token;
  
  const userInfo = uni.getStorageSync('userInfo');
  const storedName = userInfo && userInfo.name ? userInfo.name : '';
  const name = storedName || uni.getStorageSync('userName') || uni.getStorageSync('userNumber') || '访客';
  userName.value = name;
};

// 监听窗口大小变化
onMounted(() => {
  checkLoginStatus();
  
  setTimeout(() => {
    initScrollHeight();
  }, 100);
  
  resizeHandler = () => {
    setTimeout(() => {
      initScrollHeight();
    }, 100);
  };

  if (typeof uni.onWindowResize === 'function') {
    uni.onWindowResize(resizeHandler);
  }
  
  // 加载帖子数据
  fetchPosts();
});

onUnmounted(() => {
  if (typeof uni.offWindowResize === 'function' && resizeHandler) {
    uni.offWindowResize(resizeHandler);
  }
});
</script>

<style scoped>
.mobile-container {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f4f7fb;
  overflow: hidden;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 16rpx 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 0 0 40rpx 40rpx !important;
  border-top: none !important;
  flex-shrink: 0;
}

.header-title {
  font-size: 40rpx;
  font-weight: 800;
  letter-spacing: -0.5px;
}

.publish-container {
  padding: 24rpx 32rpx 0;
  flex-shrink: 0;
}

.publish-card {
  padding: 24rpx;
  border-radius: 32rpx;
}

.publish-head {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.avatar-small-refined {
  width: 80rpx;
  height: 80rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #10b981 0%, #3b82f6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 32rpx;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.2);
}

.publish-entry {
  flex: 1;
  min-width: 0;
  padding: 16rpx 22rpx;
  border-radius: 24rpx;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.86) 0%, rgba(244, 247, 251, 0.96) 100%);
  border: 1rpx solid rgba(226, 232, 240, 0.8);
}

.publish-placeholder {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #667085;
}

.publish-hint {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: #98a2b3;
}

.publish-btn-refined {
  background: linear-gradient(135deg, #4f46e5 0%, #14b8a6 100%);
  color: white;
  border-radius: 20rpx;
  padding: 0 32rpx;
  height: 72rpx;
  line-height: 72rpx;
  font-size: 26rpx;
  font-weight: 600;
  border: none;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.2);
  flex-shrink: 0;
}

.publish-btn-refined:active {
  transform: scale(0.95);
}

.publish-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 18rpx;
}

.action-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  color: #475569;
  font-size: 24rpx;
  font-weight: 600;
  padding: 16rpx 12rpx;
  border-radius: 18rpx;
  transition: all 0.2s ease;
}

.action-item-image {
  background: rgba(79, 70, 229, 0.08);
}

.action-item-video {
  background: rgba(59, 130, 246, 0.08);
}

.action-item-mood {
  background: rgba(16, 185, 129, 0.08);
}

.action-item:active {
  transform: scale(0.98);
}

.action-glyph {
  width: 36rpx;
  height: 36rpx;
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.92);
  color: #4f46e5;
  font-size: 20rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.posts-scroll {
  flex: 1;
  width: 100%;
}

.post-list {
  column-count: 2;
  column-gap: 20rpx;
  padding: 24rpx 32rpx;
}

.post-card {
  background: #ffffff;
  border-radius: 32rpx;
  padding: 16rpx;
  box-shadow: 0 12rpx 28rpx rgba(15, 23, 42, 0.06);
  border: 1rpx solid rgba(226, 232, 240, 0.8);
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  display: flex;
  flex-direction: column;
  width: 100%;
  margin-bottom: 20rpx;
  break-inside: avoid;
}

.post-card:active {
  transform: scale(0.97);
  box-shadow: 0 8rpx 18rpx rgba(15, 23, 42, 0.05);
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 16rpx;
}

.post-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 20rpx;
  overflow: hidden;
  background: linear-gradient(135deg, #4f46e5 0%, #14b8a6 100%);
  flex-shrink: 0;
}

.post-avatar image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-default {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24rpx;
  font-weight: 700;
}

.post-info {
  flex: 1;
  min-width: 0;
}

.post-username {
  font-size: 24rpx;
  font-weight: 700;
  color: #18243c;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.post-time {
  font-size: 20rpx;
  color: #94a3b8;
}

.post-text {
  font-size: 26rpx;
  line-height: 1.5;
  color: #475569;
  margin-bottom: 12rpx;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-cover,
.post-images,
.post-video {
  position: relative;
  width: 100%;
  border-radius: 20rpx;
  overflow: hidden;
  margin-top: 8rpx;
  background: #f4f7fb;
}

.post-media-image {
  width: 100%;
  display: block;
}

.image-grid {
  position: relative;
}

.image-count,
.video-badge {
  position: absolute;
  right: 10rpx;
  bottom: 10rpx;
  min-width: 44rpx;
  height: 44rpx;
  padding: 0 12rpx;
  border-radius: 999rpx;
  background: rgba(15, 23, 42, 0.75);
  color: #ffffff;
  font-size: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6rpx;
}

.play-icon {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  width: 68rpx;
  height: 68rpx;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.7);
  color: #ffffff;
  font-size: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100rpx 0;
}

.empty-icon-wrapper {
  width: 120rpx;
  height: 120rpx;
  background: rgba(99, 102, 241, 0.05);
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #4f46e5;
  margin-bottom: 32rpx;
}

.empty-text {
  font-size: 28rpx;
  color: #94a3b8;
}

.empty-glyph {
  font-size: 42rpx;
  font-weight: 800;
}

.load-more {
  padding: 24rpx 0 12rpx;
  text-align: center;
}

.loading-text {
  font-size: 24rpx;
  color: #94a3b8;
}

.bottom-space {
  height: 120rpx;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(12px);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.modal-content {
  width: 100%;
  max-width: 414px;
  margin: 0 auto;
  background: white;
  border-radius: 48rpx 48rpx 0 0;
  padding: 40rpx;
  box-sizing: border-box;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32rpx;
}

.modal-title {
  font-size: 36rpx;
  font-weight: 800;
  color: #18243c;
}

.close-btn {
  background: #f1f5f9;
  border: none;
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
  color: #475569;
}

.modal-body {
  max-height: 760rpx;
}

.post-textarea {
  width: 100%;
  height: 240rpx;
  background: #f8fafc;
  border-radius: 24rpx;
  padding: 24rpx;
  font-size: 30rpx;
  box-sizing: border-box;
}

.char-count {
  display: block;
  margin-top: 12rpx;
  text-align: right;
  font-size: 22rpx;
  color: #94a3b8;
}

.image-preview,
.video-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 24rpx;
}

.preview-item {
  position: relative;
  width: 160rpx;
  height: 160rpx;
  border-radius: 20rpx;
  overflow: hidden;
  background: #f1f5f9;
}

.preview-item image,
.preview-item video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 8rpx;
  right: 8rpx;
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: rgba(15, 23, 42, 0.72);
  color: #ffffff;
  font-size: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.media-actions {
  display: flex;
  gap: 20rpx;
  margin-top: 32rpx;
}

.media-btn-refined {
  flex: 1;
  height: 88rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-size: 26rpx;
  font-weight: 600;
  border: 1px solid #e2e8f0;
  background: white;
}

.media-btn-refined:active {
  background: #f8fafc;
  transform: scale(0.98);
}

.btn-glyph {
  width: 34rpx;
  height: 34rpx;
  border-radius: 10rpx;
  background: rgba(79, 70, 229, 0.1);
  color: #4f46e5;
  font-size: 20rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-footer {
  margin-top: 48rpx;
  display: flex;
  gap: 24rpx;
}

.modal-footer button {
  flex: 1;
  height: 96rpx;
  border-radius: 24rpx;
  font-size: 30rpx;
  font-weight: 700;
}

.cancel-btn {
  background: #f1f5f9;
  color: #475569;
}

.submit-btn {
  background: linear-gradient(135deg, #4f46e5 0%, #14b8a6 100%);
  color: white;
  box-shadow: 0 8px 16rpx rgba(99, 102, 241, 0.2);
}

.submit-btn:disabled {
  opacity: 0.5;
  box-shadow: none;
}
</style>

