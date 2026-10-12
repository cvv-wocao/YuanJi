// AI聊天功能模块
import { ref, onMounted } from 'vue';

export function useAIChat() {
  const showChatModal = ref(false);
  const chatMessages = ref([]);
  const chatInput = ref('');
  const isAiTyping = ref(false);
  const chatScrollTop = ref(0);
  const currentThreadSlug = ref(null);
  const AI_BASE_URL = 'http://localhost:8080/sport-ai';
  const threads = ref([]);
  const showThreadList = ref(false);
  const loadingThreads = ref(false);

  // 获取token
  const getToken = () => {
    return uni.getStorageSync('token') || '';
  };

  // 获取用户ID
  const getUserId = () => {
    return uni.getStorageSync('userNumber') || 'default';
  };

  // 保存聊天记录
  const saveChatHistory = () => {
    const userId = getUserId();
    const chatHistory = {
      messages: chatMessages.value,
      threadSlug: currentThreadSlug.value,
      lastUpdated: new Date().toISOString()
    };
    uni.setStorageSync(`chat_history_${userId}`, chatHistory);
    console.log('聊天记录已保存');
  };

  // 加载聊天记录
  const loadChatHistory = () => {
    const userId = getUserId();
    const chatHistory = uni.getStorageSync(`chat_history_${userId}`);
    if (chatHistory) {
      chatMessages.value = chatHistory.messages || [];
      currentThreadSlug.value = chatHistory.threadSlug;
      console.log('聊天记录已加载');
    }
  };

  // 关闭聊天弹窗
  const closeChatModal = () => {
    saveChatHistory();
    showChatModal.value = false;
  };

  // 创建新的对话线程
  const createNewThread = async () => {
    try {
      const token = getToken();
      const userId = getUserId();
      
      // ===== 创建新线程前，先清空当前显示的聊天记录 =====
      chatMessages.value = [];
      
      const response = await fetch(`${AI_BASE_URL}/thread/new`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          workspaceSlug: '9a719805-88c9-4aaf-ac7b-5b3d82118ad8',
          threadName: `${userId}_${Date.now()}`,
          userId: userId
        })
      });

      const data = await response.json();
      console.log('线程创建返回数据:', data);

      if (data.code === 200 && data.data) {
        const threadData = data.data;
        console.log('threadData:', threadData);
        // 使用JSON.parse确保获取到原始值
        const parsedData = JSON.parse(JSON.stringify(threadData));
        currentThreadSlug.value = parsedData.thread?.slug;
        console.log('线程创建成功，threadSlug:', currentThreadSlug.value);
        
        // ===== 再次确认清空聊天记录 =====
        chatMessages.value = [];
        
        // 重新获取线程列表
        await getThreads();
        
        console.log('新线程创建成功，聊天记录已清空');
      } else {
        console.error('线程创建失败:', data.message);
        throw new Error(data.message || '线程创建失败');
      }
    } catch (err) {
      console.error('创建线程失败:', err);
    }
  };

  // 获取线程列表
  const getThreads = async () => {
    try {
      loadingThreads.value = true;
      const token = getToken();
      const response = await fetch(`${AI_BASE_URL}/threads`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      const data = await response.json();
      console.log('线程列表返回数据:', data);

      if (data.code === 200 && data.data) {
        // 尝试从不同的数据结构中提取线程列表
        let extractedThreads = [];
        
        // 方式1: workspace.threads
        if (data.data.workspace && Array.isArray(data.data.workspace.threads)) {
          extractedThreads = data.data.workspace.threads;
        }
        // 方式2: workspace 是一个数组
        else if (Array.isArray(data.data.workspace) && data.data.workspace.length > 0) {
          // 检查 workspace[0].threads
          if (data.data.workspace[0].threads && Array.isArray(data.data.workspace[0].threads)) {
            extractedThreads = data.data.workspace[0].threads;
          }
        }
        // 方式3: 直接是数组
        else if (Array.isArray(data.data)) {
          extractedThreads = data.data;
        }
        
        console.log('提取的线程列表:', extractedThreads);
        console.log('完整数据:', data.data);
        
        // 为每个线程添加名称和时间
        threads.value = extractedThreads.map((thread, index) => {
          let name = thread.name || '';
          let displayTime = '';
          
          // 尝试从名称中提取时间戳
          const timestampMatch = name.match(/(\d{13})$/);
          if (timestampMatch) {
            const timestamp = parseInt(timestampMatch[1]);
            const date = new Date(timestamp);
            // 只显示时间
            displayTime = `${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`;
          } else if (thread.slug) {
            // 如果没有名称但有slug，使用slug的前8位作为标识
            displayTime = thread.slug.substring(0, 8);
          } else {
            // 如果都没有，使用默认格式
            displayTime = `对话 ${index + 1}`;
          }
          
          return {
            ...thread,
            name: displayTime,
            updatedAt: new Date().toISOString()
          };
        });
        console.log('线程列表获取成功:', threads.value);
        console.log('完整数据结构:', JSON.stringify(data.data, null, 2));
      } else {
        console.error('获取线程列表失败:', data.message);
        threads.value = [];
      }
    } catch (err) {
      console.error('获取线程列表失败:', err);
      threads.value = [];
    } finally {
      loadingThreads.value = false;
    }
  };

  // 获取线程聊天记录
  const getThreadChats = async (threadSlug) => {
    try {
      const token = getToken();
      const response = await fetch(`${AI_BASE_URL}/thread/chats?threadSlug=${threadSlug}`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      const data = await response.json();
      console.log('线程聊天记录返回数据:', data);

      if (data.code === 200 && data.data) {
        const history = data.data.history || [];
        // 转换聊天记录格式
        const messages = history.map(item => ({
          role: item.role,
          content: item.content,
          timestamp: new Date(item.sentAt * 1000).toISOString(),
          userId: getUserId()
        }));
        chatMessages.value = messages;
        currentThreadSlug.value = threadSlug;
        saveChatHistory();
        console.log('线程聊天记录获取成功');
      } else {
        console.error('获取线程聊天记录失败:', data.message);
      }
    } catch (err) {
      console.error('获取线程聊天记录失败:', err);
    }
  };

  // 切换线程
  const switchThread = async (threadSlug) => {
    // ===== 切换线程前，保存当前线程记录 =====
    saveChatHistory();
    
    // ===== 清空当前显示的聊天记录 =====
    chatMessages.value = [];
    
    // 更新当前线程
    currentThreadSlug.value = threadSlug;
    
    // 从后端获取该线程的聊天记录
    await getThreadChats(threadSlug);
    
    showThreadList.value = false;
  };

  // 发送消息
  const sendMessage = async () => {
    const message = chatInput.value.trim();
    if (!message) return;

    const userId = getUserId();
    const timestamp = new Date().toISOString();

    // 添加用户消息
    chatMessages.value.push({
      role: 'user',
      content: message,
      timestamp: timestamp,
      userId: userId
    });

    chatInput.value = '';
    isAiTyping.value = true;

    // 如果还没有线程，先创建一个
    if (!currentThreadSlug.value) {
      await createNewThread();
    }

    try {
      const token = getToken();
      const response = await fetch(`${AI_BASE_URL}/thread/stream-chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          workspaceSlug: '9a719805-88c9-4aaf-ac7b-5b3d82118ad8',
          threadSlug: currentThreadSlug.value,
          message: message
        })
      });

      if (!response.ok) {
        throw new Error('网络请求失败');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let aiMessage = '';

      // 创建AI消息占位
      const aiMsgIndex = chatMessages.value.length;
      const aiTimestamp = new Date().toISOString();
      chatMessages.value.push({
        role: 'ai',
        content: '',
        timestamp: aiTimestamp,
        userId: userId
      });

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        // 处理SSE数据
        const lines = chunk.split('\n');
        for (const line of lines) {
          const trimmedLine = line.trim();
          if (trimmedLine.startsWith('data:')) {
            const data = trimmedLine.slice(5).trim();
            if (data === '[DONE]'|| data === '') continue;

            if (data) {
              aiMessage += data;
              chatMessages.value[aiMsgIndex].content = aiMessage;
              // 滚动到底部
              chatScrollTop.value = 999999;
            }
          }
        }
      }

      isAiTyping.value = false;
      saveChatHistory();
    } catch (err) {
      console.error('发送消息失败:', err);
      isAiTyping.value = false;
      const errorTimestamp = new Date().toISOString();
      chatMessages.value.push({
        role: 'ai',
        content: '抱歉，我遇到了一些问题。请稍后再试。',
        timestamp: errorTimestamp,
        userId: userId
      });
      saveChatHistory();
    }
  };

  // 清空聊天记录
  const clearChatHistory = () => {
    const userId = getUserId();
    chatMessages.value = [];
    currentThreadSlug.value = null;
    uni.removeStorageSync(`chat_history_${userId}`);
    console.log('聊天记录已清空');
  };

  // 初始化时加载聊天记录
  onMounted(() => {
    loadChatHistory();
  });

  return {
    showChatModal,
    chatMessages,
    chatInput,
    isAiTyping,
    chatScrollTop,
    currentThreadSlug,
    threads,
    showThreadList,
    loadingThreads,
    closeChatModal,
    sendMessage,
    clearChatHistory,
    loadChatHistory,
    saveChatHistory,
    createNewThread,
    getThreads,
    switchThread
  };
}
