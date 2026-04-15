export const texts = {
  documentTitle: '地图画上传',
  theme: {
    options: {
      system: '跟随设备',
      light: '浅色',
      dark: '深色',
    },
  },
  config: {
    loading: '加载中',
    loadingDescription: '正在加载上传配置...',
    loadingHint: '正在读取上传格式...',
    errorTitle: '暂时无法打开上传页面，可能是服务器内部错误。',
    errorDescription: '请稍后再试一次，或回到游戏内获取新的上传链接。',
    invalidSessionDescription: '链接不可用，请回到游戏内获取新的上传链接。',
  },
  panel: {
    title: '上传图片',
  },
  dropzone: {
    title: '拖拽到此处或点击选择',
    supportedFormats: (formats: string) => `支持上传 ${formats}。`,
  },
  preview: {
    alt: '待上传图片预览',
    fileSize: (size: string) => `文件大小：${size}`,
  },
  uploadButton: {
    idle: '开始上传',
    uploading: '正在上传',
    success: '上传完成',
  },
  uploadStatus: {
    idle: '',
    uploading: '正在上传，请不要关闭页面。',
    success: '上传完成，可回到游戏内继续操作。',
    failed: '上传失败，请稍后再试一次。',
  },
  uploadErrors: {
    notFound: '上传链接已失效，请回到游戏内获取新的上传链接。',
    conflict: '服务器正在处理这次上传，请稍后再试。',
    gone: '上传链接已过期，请回到游戏内获取新的上传链接。',
    payloadTooLarge: '文件过大，请更换一张更小的图片。',
    generic: '网络或服务器暂时没有响应，请稍后再试。',
  },
  dialog: {
    unsupportedTypeTitle: '不支持的格式',
    unsupportedTypeDescription: (formats: string) => `请选择 ${formats} 格式的图片。`,
    fileTooLargeTitle: '文件过大',
    fileTooLargeDescription: (maxSize: string) => `请选择不超过 ${maxSize} 的图片。`,
    tooManyPixelsTitle: '图片尺寸过大',
    tooManyPixelsDescription: (width: number, height: number) =>
      `建议大小不超过 ${width} x ${height}，请换一张更小的图片。`,
    unreadableImageTitle: '无法读取图片',
    unreadableImageDescription: '请确认文件没有损坏，并使用受支持的图片格式。',
  },
  actions: {
    retry: '重新加载',
    confirm: '好',
    closeDialog: '关闭',
  },
}
