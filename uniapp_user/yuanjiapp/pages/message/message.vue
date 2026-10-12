<template>
  <view class="message-page">
    <view class="page-glow glow-left"></view>
    <view class="page-glow glow-right"></view>

    <view class="nav-header" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view>
        <text class="nav-eyebrow">INBOX</text>
        <text class="nav-title">消息中心</text>
      </view>
      <view class="action-btn" @click="refreshList">
        <text class="action-icon">↻</text>
      </view>
    </view>

    <view class="search-bar">
      <view class="search-input-wrapper">
        <text class="search-icon">搜</text>
        <input
          class="search-input"
          v-model="searchKeyword"
          placeholder="搜索联系人或消息内容"
          confirm-type="search"
        />
      </view>
    </view>

    <scroll-view
      class="conversation-list"
      scroll-y
      :style="{ height: listHeight }"
      :refresher-enabled="true"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
    >
      <view v-if="filteredConversations.length === 0 && !loading" class="empty-state">
        <view class="empty-icon-shell">
          <text class="empty-icon">信</text>
        </view>
        <text class="empty-title">暂时还没有消息</text>
        <text class="empty-desc">去互动、关注或发起私信后，这里会出现新的会话。</text>
      </view>

      <view
        v-for="(conversation, index) in filteredConversations"
        :key="conversation.id"
        class="conversation-card"
        :style="{ animationDelay: index * 40 + 'ms' }"
        @click="openConversation(conversation)"
      >
        <view class="avatar-wrapper" @click.stop="openUserProfile(getOtherUser(conversation)?.id)">
          <view class="avatar">
            <image
              v-if="getOtherUser(conversation)?.avatar"
              :src="getOtherUser(conversation).avatar"
              mode="aspectFill"
            />
            <view v-else class="avatar-fallback">{{ getUserName(getOtherUser(conversation)).charAt(0) }}</view>
          </view>
          <view v-if="isOnline(getOtherUser(conversation)?.id)" class="online-indicator"></view>
        </view>

        <view class="conversation-content">
          <view class="content-header">
            <view class="name-row">
              <text class="user-name">{{ getUserName(getOtherUser(conversation)) }}</text>
              <text v-if="isMutualFollowMap[getOtherUser(conversation)?.id]" class="relation-badge">互相关注</text>
            </view>
            <text class="message-time">{{ formatTime(conversation.createTime) }}</text>
          </view>

          <view class="content-body">
            <text class="message-preview" :class="{ unread: getUnreadCount(conversation) > 0 }">
              {{ getLastMessageText(conversation) }}
            </text>
            <view v-if="getUnreadCount(conversation) > 0" class="unread-badge">
              {{ getUnreadCount(conversation) > 99 ? '99+' : getUnreadCount(conversation) }}
            </view>
            <view v-else-if="conversation.isPinned" class="pin-indicator">置顶</view>
          </view>
        </view>
      </view>

      <view class="bottom-spacer"></view>
    </scroll-view>

    <view v-if="loading && conversations.length === 0" class="loading-overlay">
      <view class="loading-orbit"></view>
      <text class="loading-text">消息加载中...</text>
    </view>
  </view>
</template>

<script>
import { getConversations } from '@/api/message.js';
import { isMutualFollow } from '@/api/follow.js';

export default {
  data() {
    return {
      conversations: [],
      loading: false,
      refreshing: false,
      isMutualFollowMap: {},
      searchKeyword: '',
      statusBarHeight: 20,
      listHeight: '0px',
      onlineUsers: new Set()
    };
  },

  computed: {
    filteredConversations() {
      if (!this.searchKeyword.trim()) {
        return this.conversations;
      }
      const keyword = this.searchKeyword.toLowerCase();
      return this.conversations.filter((conv) => {
        const userName = this.getUserName(this.getOtherUser(conv)).toLowerCase();
        const lastMsg = this.getLastMessageText(conv).toLowerCase();
        return userName.includes(keyword) || lastMsg.includes(keyword);
      });
    }
  },

  onLoad() {
    this.initSystemInfo();
    this.loadConversations();
  },

  onShow() {
    this.loadConversations();
  },

  methods: {
    initSystemInfo() {
      const systemInfo = uni.getSystemInfoSync();
      this.statusBarHeight = systemInfo.statusBarHeight || 20;
      const windowHeight = systemInfo.windowHeight;
      this.listHeight = `${windowHeight - 156 - this.statusBarHeight}px`;
    },

    async loadConversations() {
      this.loading = true;
      try {
        const result = await getConversations();
        if (result.success) {
          this.conversations = result.data || [];
          await this.checkMutualFollows();
        }
      } catch (error) {
        console.error('获取消息列表失败:', error);
      } finally {
        this.loading = false;
        this.refreshing = false;
      }
    },

    async checkMutualFollows() {
      const currentUserId = Number(uni.getStorageSync('userId'));
      const mutualFollowMap = {};

      for (const conversation of this.conversations) {
        const otherUserId = conversation.senderId === currentUserId
          ? conversation.receiverId
          : conversation.senderId;
        if (otherUserId) {
          try {
            const result = await isMutualFollow(otherUserId);
            mutualFollowMap[otherUserId] = result.success && result.data;
          } catch (error) {
            mutualFollowMap[otherUserId] = false;
          }
        }
      }

      this.isMutualFollowMap = mutualFollowMap;
    },

    onRefresh() {
      this.refreshing = true;
      this.loadConversations();
    },

    refreshList() {
      this.loadConversations();
    },

    isOnline(userId) {
      return this.onlineUsers.has(userId);
    },

    openConversation(conversation) {
      if (!conversation.id) return;
      const currentUserId = Number(uni.getStorageSync('userId'));
      const otherUserId = conversation.senderId === currentUserId
        ? conversation.receiverId
        : conversation.senderId;
      uni.navigateTo({
        url: `/pages/message-detail/message-detail?userId=${otherUserId}`
      });
    },

    openUserProfile(userId) {
      if (!userId) return;
      uni.navigateTo({
        url: `/pages/user-profile/user-profile?id=${userId}`
      });
    },

    getUserName(user) {
      if (!user) return '未知用户';
      return user.name || user.nickname || user.username || `用户${user.id}`;
    },

    getOtherUser(conversation) {
      const currentUserId = Number(uni.getStorageSync('userId'));
      return conversation.senderId === currentUserId
        ? conversation.receiver
        : conversation.sender;
    },

    getUnreadCount(conversation) {
      return conversation.unreadCount || 0;
    },

    getLastMessageText(conversation) {
      return conversation.lastMessage || conversation.content || '暂无消息';
    },

    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      const now = new Date();
      const diff = now - date;

      if (diff < 60000) return '刚刚';
      if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
      if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
      if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`;
      return `${date.getMonth() + 1}月${date.getDate()}日`;
    }
  }
};
</script>

<style scoped>
.message-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at top left, rgba(54, 118, 255, 0.16) 0, rgba(54, 118, 255, 0) 34%),
    radial-gradient(circle at top right, rgba(37, 193, 163, 0.14) 0, rgba(37, 193, 163, 0) 26%),
    linear-gradient(180deg, #f7f9fd 0%, #eef3f8 100%);
  overflow: hidden;
}

.page-glow {
  position: absolute;
  width: 320rpx;
  height: 320rpx;
  border-radius: 50%;
  filter: blur(58rpx);
  opacity: 0.5;
  pointer-events: none;
}

.glow-left {
  top: -40rpx;
  left: -120rpx;
  background: rgba(54, 118, 255, 0.22);
}

.glow-right {
  top: 220rpx;
  right: -120rpx;
  background: rgba(37, 193, 163, 0.18);
}

.nav-header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 18rpx 28rpx 8rpx;
}

.nav-eyebrow {
  display: block;
  font-size: 20rpx;
  letter-spacing: 4rpx;
  color: #4578ef;
  font-weight: 800;
}

.nav-title {
  display: block;
  margin-top: 10rpx;
  font-size: 42rpx;
  font-weight: 800;
  color: #13233f;
}

.action-btn {
  width: 76rpx;
  height: 76rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.84);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 16rpx 32rpx rgba(19, 35, 63, 0.06);
}

.action-icon {
  font-size: 24rpx;
  font-weight: 700;
  color: #406fec;
}

.search-bar {
  position: relative;
  z-index: 2;
  padding: 14rpx 28rpx 12rpx;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  height: 84rpx;
  padding: 0 22rpx;
  gap: 12rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 16rpx 32rpx rgba(19, 35, 63, 0.05);
}

.search-icon {
  width: 38rpx;
  height: 38rpx;
  border-radius: 12rpx;
  background: rgba(64, 111, 236, 0.1);
  color: #406fec;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 21rpx;
  font-weight: 700;
}

.search-input {
  flex: 1;
  font-size: 27rpx;
  color: #1a2a44;
}

.conversation-list {
  position: relative;
  z-index: 2;
  flex: 1;
  padding: 8rpx 24rpx 0;
}

.conversation-card {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-bottom: 16rpx;
  padding: 22rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18rpx 34rpx rgba(19, 35, 63, 0.06);
  animation: slideIn 0.35s ease forwards;
  opacity: 0;
  transform: translateY(18rpx);
}

@keyframes slideIn {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 28rpx;
  overflow: hidden;
  background: linear-gradient(135deg, #3d6fec 0%, #29b6a5 100%);
}

.avatar image {
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
  font-size: 38rpx;
  font-weight: 700;
}

.online-indicator {
  position: absolute;
  right: 6rpx;
  bottom: 6rpx;
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
  background: #22c55e;
  border: 4rpx solid #ffffff;
}

.conversation-content {
  flex: 1;
  min-width: 0;
}

.content-header,
.content-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.content-body {
  margin-top: 14rpx;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
}

.user-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #152644;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.relation-badge {
  flex-shrink: 0;
  height: 38rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: rgba(64, 111, 236, 0.1);
  color: #406fec;
  font-size: 20rpx;
  display: flex;
  align-items: center;
}

.message-time {
  flex-shrink: 0;
  font-size: 22rpx;
  color: #8c99ab;
}

.message-preview {
  flex: 1;
  min-width: 0;
  font-size: 25rpx;
  color: #728097;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.message-preview.unread {
  color: #172843;
  font-weight: 600;
}

.unread-badge {
  min-width: 44rpx;
  height: 44rpx;
  padding: 0 12rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #f0634b 0%, #f59743 100%);
  color: #ffffff;
  font-size: 20rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pin-indicator {
  flex-shrink: 0;
  height: 40rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: #f1f4f8;
  color: #7b8798;
  font-size: 20rpx;
  display: flex;
  align-items: center;
}

.empty-state {
  padding: 110rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.empty-icon-shell {
  width: 132rpx;
  height: 132rpx;
  border-radius: 40rpx;
  background: rgba(64, 111, 236, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-icon {
  font-size: 44rpx;
  font-weight: 800;
  color: #406fec;
}

.empty-title {
  display: block;
  margin-top: 28rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #182945;
}

.empty-desc {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #8090a4;
}

.bottom-spacer {
  height: 40rpx;
}

.loading-overlay {
  position: absolute;
  inset: 0;
  background: rgba(247, 249, 253, 0.84);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 4;
}

.loading-orbit {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  border: 6rpx solid rgba(64, 111, 236, 0.14);
  border-top-color: #406fec;
  animation: spin 0.9s linear infinite;
}

.loading-text {
  margin-top: 18rpx;
  font-size: 24rpx;
  color: #6f8098;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
