<template>
  <view class="address-page">
    <view class="nav-bar">
      <view class="nav-btn" @tap="goBack">
        <image src="/static/mall/back-button.png" class="nav-icon" />
      </view>
      <view class="nav-copy">
        <text class="nav-title">地址管理</text>
        <text class="nav-subtitle">维护你的常用收货信息</text>
      </view>
      <view class="nav-add" @tap="addAddress">新增</view>
    </view>

    <scroll-view class="address-scroll" scroll-y>
      <view v-if="addresses.length" class="address-list">
        <view class="address-card" v-for="address in addresses" :key="address.id" @tap="selectAddress(address)">
          <view class="card-head">
            <view class="receiver-block">
              <text class="receiver-name">{{ address.receiverName }}</text>
              <text class="receiver-phone">{{ address.receiverPhone }}</text>
            </view>
            <text v-if="address.isDefault" class="default-tag">默认</text>
          </view>

          <text class="address-detail">
            {{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}
          </text>

          <view class="card-foot" @tap.stop>
            <view class="action-item" @tap="editAddress(address.id)">
              <text class="action-mark">编</text>
              <text>编辑</text>
            </view>
            <view class="action-item delete-item" @tap="deleteAddress(address.id)">
              <text class="action-mark">删</text>
              <text>删除</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty-state">
        <view class="empty-icon">址</view>
        <text class="empty-title">还没有收货地址</text>
        <text class="empty-copy">新增一个常用地址，下单时会更方便。</text>
        <view class="empty-btn" @tap="addAddress">新增地址</view>
      </view>

      <view class="bottom-space"></view>
    </scroll-view>

    <view class="floating-add" @tap="addAddress">+ 新增地址</view>
  </view>
</template>

<script>
export default {
  data() {
    return {
      addresses: [],
      token: ''
    };
  },
  onLoad() {
    this.getToken();
    this.loadAddresses();
  },
  onShow() {
    this.loadAddresses();
  },
  methods: {
    getToken() {
      try {
        this.token = uni.getStorageSync('token') || '';
      } catch (e) {
        console.error('获取 token 失败', e);
      }
    },
    goBack() {
      uni.navigateBack({
        delta: 1
      });
    },
    loadAddresses() {
      uni.request({
        url: 'http://localhost:8080/address/list',
        method: 'GET',
        header: {
          Authorization: 'Bearer ' + this.token
        },
        success: (res) => {
          if (res.data.success) {
            this.addresses = res.data.data || [];
          } else {
            uni.showToast({
              title: res.data.message || '获取地址列表失败',
              icon: 'none'
            });
          }
        },
        fail: (err) => {
          console.error('加载地址列表失败', err);
          uni.showToast({
            title: '网络错误，请稍后重试',
            icon: 'none'
          });
        }
      });
    },
    addAddress() {
      uni.navigateTo({
        url: '/pages/mall/address-edit'
      });
    },
    editAddress(id) {
      uni.navigateTo({
        url: `/pages/mall/address-edit?id=${id}`
      });
    },
    deleteAddress(id) {
      uni.showModal({
        title: '确认删除',
        content: '确定要删除这个地址吗？',
        success: (res) => {
          if (!res.confirm) return;
          uni.request({
            url: 'http://localhost:8080/address/delete',
            method: 'POST',
            data: {
              id
            },
            header: {
              Authorization: 'Bearer ' + this.token
            },
            success: (result) => {
              if (result.data.success) {
                uni.showToast({
                  title: '删除成功',
                  icon: 'success'
                });
                this.loadAddresses();
              } else {
                uni.showToast({
                  title: result.data.message || '删除失败',
                  icon: 'none'
                });
              }
            },
            fail: (err) => {
              console.error('删除地址失败', err);
              uni.showToast({
                title: '网络错误，请稍后重试',
                icon: 'none'
              });
            }
          });
        }
      });
    },
    selectAddress(address) {
      const pages = getCurrentPages();
      const prevPage = pages[pages.length - 2];

      if (prevPage && prevPage.route === 'pages/mall/order') {
        prevPage.$vm.address = {
          id: address.id,
          receiverName: address.receiverName,
          receiverPhone: address.receiverPhone,
          address: address.province + address.city + address.district + address.detailAddress
        };
      }

      uni.navigateBack({
        delta: 1
      });
    }
  }
};
</script>

<style scoped>
.address-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f6f8fc 0%, #eef3f9 100%);
  padding-bottom: calc(156rpx + env(safe-area-inset-bottom));
  box-sizing: border-box;
}

.nav-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: calc(22rpx + env(safe-area-inset-top)) 28rpx 18rpx;
  background: rgba(246, 248, 252, 0.94);
  backdrop-filter: blur(16rpx);
}

.nav-btn,
.nav-add {
  height: 82rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 14rpx 28rpx rgba(18, 35, 63, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nav-btn {
  width: 82rpx;
}

.nav-add {
  min-width: 116rpx;
  padding: 0 20rpx;
  font-size: 24rpx;
  font-weight: 700;
  color: #335eab;
}

.nav-icon {
  width: 32rpx;
  height: 32rpx;
}

.nav-copy {
  flex: 1;
  min-width: 0;
}

.nav-title {
  display: block;
  font-size: 38rpx;
  font-weight: 800;
  color: #162643;
}

.nav-subtitle {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #77859a;
}

.address-scroll {
  height: calc(100vh - 126rpx - env(safe-area-inset-top));
}

.address-list {
  padding: 12rpx 28rpx 0;
}

.address-card,
.empty-state,
.floating-add {
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16rpx 34rpx rgba(18, 35, 63, 0.06);
}

.address-card {
  padding: 22rpx;
  border-radius: 30rpx;
  margin-bottom: 16rpx;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}

.receiver-block {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10rpx 14rpx;
}

.receiver-name {
  font-size: 30rpx;
  font-weight: 800;
  color: #162643;
}

.receiver-phone {
  font-size: 24rpx;
  color: #64748b;
}

.default-tag {
  min-width: 78rpx;
  height: 40rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: rgba(51, 94, 171, 0.12);
  color: #335eab;
  font-size: 20rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.address-detail {
  display: block;
  margin-top: 18rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #5f6c80;
}

.card-foot {
  margin-top: 18rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid rgba(18, 35, 63, 0.06);
  display: flex;
  justify-content: flex-end;
  gap: 14rpx;
}

.action-item {
  min-width: 104rpx;
  height: 48rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: #f4f7fb;
  color: #4f5f7b;
  font-size: 22rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
}

.delete-item {
  background: rgba(227, 95, 70, 0.1);
  color: #de6f4f;
}

.action-mark {
  font-size: 20rpx;
}

.empty-state {
  margin: 28rpx;
  padding: 88rpx 36rpx;
  border-radius: 34rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.empty-icon {
  width: 120rpx;
  height: 120rpx;
  border-radius: 36rpx;
  background: linear-gradient(135deg, #dbe7fa 0%, #edf3ff 100%);
  color: #315f92;
  font-size: 42rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-title {
  display: block;
  margin-top: 28rpx;
  font-size: 32rpx;
  font-weight: 800;
  color: #162643;
}

.empty-copy {
  display: block;
  margin-top: 14rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #7a8799;
}

.empty-btn {
  margin-top: 28rpx;
  min-width: 220rpx;
  height: 78rpx;
  padding: 0 26rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #18304d 0%, #315f92 100%);
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bottom-space {
  height: 36rpx;
}

.floating-add {
  position: fixed;
  left: 24rpx;
  right: 24rpx;
  bottom: calc(24rpx + env(safe-area-inset-bottom));
  height: 88rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #18304d 0%, #315f92 100%);
  color: #fff;
  font-size: 28rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
