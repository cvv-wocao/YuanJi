<template>
  <view class="message-detail-page">
    <view class="chat-glow glow-top"></view>
    <view class="chat-glow glow-bottom"></view>

    <view class="header" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="header-content">
        <view class="back-btn" @click="goBack">
          <image src="/static/mall/back-button.png" class="back-icon" />
        </view>
        <view class="title-block">
          <text class="title">{{ userName }}</text>
          <text class="subtitle">私信对话</text>
        </view>
        <view class="placeholder"></view>
      </view>
    </view>

    <view class="chat-summary">
      <text class="summary-badge">正在沟通</text>
      <text class="summary-text">你们的消息会在这里实时同步，订单沟通和日常聊天都能直接继续。</text>
    </view>

    <scroll-view
      class="message-scroll"
      scroll-y
      :scroll-into-view="scrollToView"
      :scroll-top="scrollTop"
      scroll-with-animation
      :style="{ height: scrollHeight + 'px' }"
    >
      <view class="message-list">
        <view v-if="messages.length === 0" class="empty-state">
          <view class="empty-icon-shell">
            <text class="empty-icon">聊</text>
          </view>
          <text class="empty-title">还没有聊天记录</text>
          <text class="empty-desc">发一条消息，开启这段对话。</text>
        </view>

        <view
          v-for="message in messages"
          :key="message.id"
          :id="`msg-${message.id}`"
          class="message-item"
          :class="{ 'is-mine': message.senderId === currentUserId }"
        >
          <view class="avatar">
            <image v-if="message.sender?.avatar" :src="message.sender.avatar" mode="aspectFill" />
            <view v-else class="avatar-default">
              {{ getUserName(message.sender).charAt(0) }}
            </view>
          </view>
          <view class="message-content">
            <view class="text-wrapper">
              <text class="text">{{ message.content }}</text>
            </view>
            <text class="time">{{ formatTime(message.createTime) }}</text>
          </view>
        </view>
        <view id="bottom-placeholder" class="bottom-placeholder"></view>
      </view>
    </scroll-view>

    <view class="input-fixed">
      <view class="input-area">
        <view class="input-wrapper">
          <input
            class="message-input"
            v-model="inputMessage"
            type="text"
            placeholder="输入消息..."
            :confirm-type="'send'"
            @confirm="sendMessage"
          />
          <button
            class="send-btn"
            :class="{ disabled: !inputMessage.trim() }"
            @click="sendMessage"
            :disabled="!inputMessage.trim()"
          >
            发送
          </button>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { getMessages, sendMessage } from '@/api/message.js';
import { getUserById } from '@/api/user.js';

export default {
  data() {
    return {
      targetUserId: null,
      currentUserId: null,
      userName: '',
      messages: [],
      inputMessage: '',
      loading: false,
      canSend: true,
      sendMessageCount: 0,
      scrollToView: '',
      scrollTop: -1,
      isNewConversation: false,
      statusBarHeight: 20,
      scrollHeight: 0
    };
  },

  async onLoad(options) {
    const systemInfo = uni.getSystemInfoSync();
    this.statusBarHeight = systemInfo.statusBarHeight || 20;

    this.currentUserId = Number(uni.getStorageSync('userId'));

    const id = options.id && options.id !== 'null' ? Number(options.id) : null;
    const userId = options.userId && options.userId !== 'null' ? Number(options.userId) : null;

    if (userId) {
      this.targetUserId = userId;
      this.isNewConversation = false;
      await this.loadUserInfo();
      await this.loadMessages();
    } else if (id) {
      this.targetUserId = id;
      this.isNewConversation = true;
      await this.loadMessages();
    } else {
      uni.showToast({ title: '参数错误', icon: 'none' });
      setTimeout(() => {
        uni.navigateBack();
      }, 1500);
    }

    this.calcScrollHeight();
  },

  onReady() {
    this.calcScrollHeight();
  },

  onShow() {
    if (!this.isNewConversation && this.targetUserId) {
      this.loadMessages();
    }
  },

  methods: {
    calcScrollHeight() {
      const systemInfo = uni.getSystemInfoSync();
      const windowHeight = systemInfo.windowHeight;
      const headerHeight = this.statusBarHeight + 154;
      const inputHeight = 108;
      this.scrollHeight = windowHeight - headerHeight - inputHeight;
    },

    async loadUserInfo() {
      try {
        const result = await getUserById(this.targetUserId);
        if (result.success && result.data) {
          this.userName = result.data.name || result.data.nickname || `用户${this.targetUserId}`;
        } else {
          this.userName = `用户${this.targetUserId}`;
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
        this.userName = `用户${this.targetUserId}`;
      }
    },

    async loadMessages() {
      if (!this.targetUserId) {
        return;
      }
      this.loading = true;
      try {
        const result = await getMessages(this.targetUserId);
        if (result.success) {
          this.messages = result.data || [];

          if (this.messages.length > 0) {
            const firstMessage = this.messages[0];
            const otherUser = firstMessage.senderId === this.currentUserId
              ? firstMessage.receiver
              : firstMessage.sender;
            this.userName = this.getUserName(otherUser);
            this.targetUserId = firstMessage.senderId === this.currentUserId
              ? firstMessage.receiverId
              : firstMessage.senderId;
          }

          this.$nextTick(() => {
            this.scrollToBottom();
          });
        }
      } catch (error) {
        console.error('获取消息失败:', error);
        uni.showToast({ title: '加载消息失败', icon: 'none' });
      } finally {
        this.loading = false;
      }
    },

    scrollToBottom() {
      if (this.messages.length === 0) return;

      const lastMessage = this.messages[this.messages.length - 1];
      this.scrollToView = `msg-${lastMessage.id}`;

      setTimeout(() => {
        this.scrollTop = 99999;
      }, 50);
    },

    async sendMessage() {
      if (!this.inputMessage.trim()) {
        uni.showToast({ title: '请输入消息内容', icon: 'none' });
        return;
      }

      const receiverId = this.getReceiverId();
      if (!receiverId) {
        uni.showToast({ title: '无法获取接收者ID', icon: 'none' });
        return;
      }

      try {
        const result = await sendMessage(receiverId, this.inputMessage);

        if (result.success) {
          this.inputMessage = '';

          if (this.isNewConversation) {
            this.isNewConversation = false;
          }

          await this.loadMessages();

          setTimeout(() => {
            this.scrollToBottom();
          }, 100);
        } else {
          uni.showToast({
            title: result.message || '发送失败',
            icon: 'none',
            duration: 3000
          });
        }
      } catch (error) {
        console.error('发送消息失败:', error);
        uni.showToast({ title: '发送失败，请重试', icon: 'none' });
      }
    },

    getReceiverId() {
      if (this.targetUserId) {
        return this.targetUserId;
      }

      if (this.messages.length === 0) return null;
      const firstMessage = this.messages[0];
      return firstMessage.senderId === this.currentUserId
        ? firstMessage.receiverId
        : firstMessage.senderId;
    },

    getUserName(user) {
      if (!user) return '未知用户';
      return user.name || user.nickname || user.username || `用户${user.id}`;
    },

    formatTime(time) {
      if (!time) return '';
      const date = new Date(time);
      const hours = date.getHours().toString().padStart(2, '0');
      const minutes = date.getMinutes().toString().padStart(2, '0');
      return `${hours}:${minutes}`;
    },

    goBack() {
      uni.navigateBack();
    }
  }
};
</script>

<style scoped>
.message-detail-page {
  position: relative;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at top right, rgba(58, 117, 255, 0.18) 0, rgba(58, 117, 255, 0) 34%),
    linear-gradient(180deg, #f6f8fc 0%, #edf2f8 100%);
  overflow: hidden;
}

.chat-glow {
  position: absolute;
  width: 320rpx;
  height: 320rpx;
  border-radius: 50%;
  filter: blur(64rpx);
  opacity: 0.48;
  pointer-events: none;
}

.glow-top {
  top: -100rpx;
  right: -100rpx;
  background: rgba(61, 111, 236, 0.22);
}

.glow-bottom {
  bottom: 120rpx;
  left: -120rpx;
  background: rgba(41, 182, 165, 0.16);
}

.header {
  position: relative;
  z-index: 2;
  padding: 0 20rpx 8rpx;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 18rpx;
}

.back-btn,
.placeholder {
  width: 76rpx;
  height: 76rpx;
  border-radius: 24rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.back-btn {
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 16rpx 32rpx rgba(19, 35, 63, 0.06);
}

.back-icon {
  width: 34rpx;
  height: 34rpx;
}

.title-block {
  flex: 1;
  text-align: center;
}

.title {
  display: block;
  font-size: 34rpx;
  font-weight: 800;
  color: #13233f;
}

.subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #7d8ba0;
}

.chat-summary {
  position: relative;
  z-index: 2;
  margin: 10rpx 20rpx 12rpx;
  padding: 22rpx 24rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 18rpx 34rpx rgba(19, 35, 63, 0.05);
}

.summary-badge {
  display: inline-flex;
  align-items: center;
  height: 40rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: rgba(61, 111, 236, 0.1);
  color: #3d6fec;
  font-size: 20rpx;
  font-weight: 700;
}

.summary-text {
  display: block;
  margin-top: 12rpx;
  font-size: 23rpx;
  line-height: 1.7;
  color: #68788f;
}

.message-scroll {
  position: relative;
  z-index: 2;
  width: 100%;
}

.message-list {
  padding: 0 20rpx 24rpx;
  box-sizing: border-box;
}

.message-item {
  display: flex;
  align-items: flex-end;
  gap: 12rpx;
  margin-bottom: 18rpx;
}

.message-item.is-mine {
  flex-direction: row-reverse;
}

.avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 22rpx;
  overflow: hidden;
  background: linear-gradient(135deg, #3d6fec 0%, #29b6a5 100%);
  flex-shrink: 0;
}

.avatar image {
  width: 100%;
  height: 100%;
}

.avatar-default {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 700;
}

.message-content {
  max-width: calc(100% - 96rpx);
  display: flex;
  flex-direction: column;
  gap: 8rpx;
}

.is-mine .message-content {
  align-items: flex-end;
}

.text-wrapper {
  max-width: 100%;
}

.text {
  display: inline-block;
  max-width: 100%;
  padding: 18rpx 22rpx;
  border-radius: 24rpx 24rpx 24rpx 10rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14rpx 28rpx rgba(19, 35, 63, 0.05);
  font-size: 28rpx;
  line-height: 1.65;
  color: #20314d;
  word-break: break-all;
  white-space: pre-wrap;
}

.is-mine .text {
  border-radius: 24rpx 24rpx 10rpx 24rpx;
  background: linear-gradient(135deg, #3d6fec 0%, #5e95ff 100%);
  color: #ffffff;
  box-shadow: 0 18rpx 30rpx rgba(61, 111, 236, 0.18);
}

.time {
  font-size: 20rpx;
  color: #8f9cb0;
  padding: 0 8rpx;
}

.empty-state {
  padding: 120rpx 30rpx 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.empty-icon-shell {
  width: 120rpx;
  height: 120rpx;
  border-radius: 36rpx;
  background: rgba(61, 111, 236, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-icon {
  font-size: 38rpx;
  font-weight: 800;
  color: #3d6fec;
}

.empty-title {
  display: block;
  margin-top: 24rpx;
  font-size: 30rpx;
  font-weight: 700;
  color: #142441;
}

.empty-desc {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #7f8da2;
}

.bottom-placeholder {
  height: 12rpx;
}

.input-fixed {
  position: relative;
  z-index: 3;
  flex-shrink: 0;
  padding: 12rpx 18rpx calc(12rpx + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.9);
  border-top: 1rpx solid rgba(19, 35, 63, 0.05);
  backdrop-filter: blur(18rpx);
}

.input-area {
  width: 100%;
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 12rpx;
  width: 100%;
}

.message-input {
  flex: 1;
  height: 88rpx;
  padding: 0 24rpx;
  border-radius: 28rpx;
  background: #f3f6fb;
  font-size: 27rpx;
  color: #1f304c;
}

.send-btn {
  width: 148rpx;
  height: 88rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, #3d6fec 0%, #5e95ff 100%);
  color: #ffffff;
  font-size: 27rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 16rpx 28rpx rgba(61, 111, 236, 0.18);
}

.send-btn.disabled {
  opacity: 0.45;
  box-shadow: none;
}
</style>
