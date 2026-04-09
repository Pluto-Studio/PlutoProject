import { useEffect, useMemo, useRef, useState } from 'react'

const brandLogoUrl = '/icon.png'

function App() {
  const sessionId = useMemo(() => {
    const match = window.location.pathname.match(/^\/upload\/([^/]+)$/)
    return match?.[1] ?? ''
  }, [])
  const uploadApiUrl = useMemo(() => `/api/upload-sessions/${sessionId}/file`, [sessionId])

  const inputRef = useRef(null)
  const [config, setConfig] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [selectedFile, setSelectedFile] = useState(null)
  const [validationMessage, setValidationMessage] = useState('')
  const [submitMessage, setSubmitMessage] = useState('')
  const [isDragging, setIsDragging] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [hasBrandLogo, setHasBrandLogo] = useState(false)

  useEffect(() => {
    const image = new Image()

    image.onload = () => setHasBrandLogo(true)
    image.onerror = () => setHasBrandLogo(false)
    image.src = brandLogoUrl

    return () => {
      image.onload = null
      image.onerror = null
    }
  }, [])

  useEffect(() => {
    if (!sessionId) {
      setLoadError('当前链接无效，缺少上传会话。')
      setIsLoading(false)
      return
    }

    const controller = new AbortController()

    async function loadConfig() {
      try {
        const response = await fetch(`/api/upload-sessions/${sessionId}/config`, {
          signal: controller.signal,
        })

        if (!response.ok) {
          const payload = await safeJson(response)
          throw new Error(payload.message || '无法加载上传配置。')
        }

        const payload = await response.json()
        setConfig(payload)
      } catch (error) {
        if (error.name !== 'AbortError') {
          setLoadError(error.message || '无法加载上传配置。')
        }
      } finally {
        setIsLoading(false)
      }
    }

    loadConfig()

    return () => controller.abort()
  }, [sessionId])

  async function handlePick(file) {
    setSubmitMessage('')

    if (!file || !config) {
      return
    }

    const message = await validateFile(file, config)
    setSelectedFile(file)
    setValidationMessage(message)
  }

  async function handleSubmit() {
    if (!config || !selectedFile || validationMessage) {
      return
    }

    setIsSubmitting(true)
    setSubmitMessage('')

    try {
      const formData = new FormData()
      formData.append('file', selectedFile)

      const response = await fetch(uploadApiUrl, {
        method: 'POST',
        body: formData,
      })

      const payload = await safeJson(response)

      if (response.status === 202) {
        setSubmitMessage('文件已提交。后端校验结果请回到游戏内查看。')
        return
      }

      throw new Error(payload.message || '上传失败，请稍后再试。')
    } catch (error) {
      setSubmitMessage(error.message || '上传失败，请稍后再试。')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return <Shell title="正在准备上传页面" description="正在向服务器获取本次上传的限制配置。" />
  }

  if (loadError) {
    return <Shell title="暂时无法上传" description={loadError} />
  }

  const accept = buildAccept(config)
  const canSubmit = selectedFile && !validationMessage && !isSubmitting

  return (
    <main className="page-shell">
      <section className="upload-panel">
        <header className="hero">
          <div className="brand-lockup">
            {hasBrandLogo ? <img className="brand-logo" src={brandLogoUrl} alt="站点标识" /> : null}

            <div>
              <p className="eyebrow">图像入口</p>
              <h1>星社图像上传</h1>
            </div>
          </div>
        </header>

        <section className="info-grid">
          <InfoCard label="允许格式" value={config.supportedFormatNames.join(' / ')} />
          <InfoCard label="文件上限" value={formatBytes(config.maxBytes)} />
          <InfoCard label="尺寸范围" value={`短边至少 ${config.minShortEdge}px`} />
          <InfoCard label="最大宽高比" value={`${config.maxAspectRatio} : 1`} />
        </section>

        <section
          className={`dropzone ${isDragging ? 'is-dragging' : ''}`}
          onDragOver={(event) => {
            event.preventDefault()
            setIsDragging(true)
          }}
          onDragLeave={() => setIsDragging(false)}
          onDrop={(event) => {
            event.preventDefault()
            setIsDragging(false)
            handlePick(event.dataTransfer.files?.[0] ?? null)
          }}
        >
          <input
            ref={inputRef}
            className="file-input"
            type="file"
            accept={accept}
            onChange={(event) => handlePick(event.target.files?.[0] ?? null)}
          />

          <div className="dropzone-inner">
            <p className="dropzone-title">拖拽文件到此</p>
            <p className="dropzone-subtitle">或使用下方按钮选择本地图片文件</p>

            <div className="action-row">
              <button className="primary-button" type="button" onClick={() => inputRef.current?.click()}>
                点击上传
              </button>
              <button className="secondary-button" type="button" onClick={handleSubmit} disabled={!canSubmit}>
                {isSubmitting ? '正在提交' : '开始提交'}
              </button>
            </div>
          </div>
        </section>

        <section className="status-panel">
          <div className="status-block">
            <p className="status-label">当前文件</p>
            <p className="status-value">{selectedFile ? selectedFile.name : '尚未选择文件'}</p>
          </div>

          <div className="status-block">
            <p className="status-label">基础校验</p>
            <p className={`status-value ${validationMessage ? 'is-error' : 'is-ok'}`}>
              {selectedFile ? validationMessage || '已通过前端基础校验，可以提交。' : '请选择图片后开始校验。'}
            </p>
          </div>

          <div className="status-block">
            <p className="status-label">提交状态</p>
            <p className="status-value">{submitMessage || '提交后，游戏内菜单将继续展示后端处理结果。'}</p>
          </div>
        </section>

        <section className="tips-panel">
          <p>支持的扩展名：{config.allowedFileExtensions.map((value) => `.${value}`).join('、')}</p>
          <p>最大尺寸：{config.maxWidth}px × {config.maxHeight}px</p>
          <p>最大像素数：{config.maxPixels.toLocaleString('zh-CN')}</p>
          <p>最大宽高比：{config.maxAspectRatio} : 1</p>
        </section>
      </section>
    </main>
  )
}

function Shell({ title, description }) {
  return (
    <main className="page-shell">
      <section className="upload-panel shell-panel">
        <p className="eyebrow">图像入口</p>
        <h1>{title}</h1>
        <p className="shell-description">{description}</p>
      </section>
    </main>
  )
}

function InfoCard({ label, value, monospace = false }) {
  return (
    <article className="info-card">
      <p className="info-label">{label}</p>
      <p className={`info-value ${monospace ? 'is-monospace' : ''}`}>{value}</p>
    </article>
  )
}

async function validateFile(file, config) {
  const extension = file.name.includes('.') ? file.name.split('.').pop().toLowerCase() : ''

  if (!config.allowedFileExtensions.includes(extension)) {
    return `文件扩展名不被允许，当前仅支持 ${config.allowedFileExtensions.map((value) => `.${value}`).join('、')}。`
  }

  if (file.size > config.maxBytes) {
    return `文件体积超出限制，最大允许 ${formatBytes(config.maxBytes)}。`
  }

  if (file.type && !config.allowedMimeTypes.includes(file.type)) {
    return '文件 MIME 类型不在允许列表中。'
  }

  const { width, height } = await readImageSize(file)
  const pixels = width * height
  const shortEdge = Math.min(width, height)
  const aspectRatio = Math.max(width, height) / Math.max(shortEdge, 1)

  if (width > config.maxWidth || height > config.maxHeight || pixels > config.maxPixels) {
    return '图片尺寸或总像素数超出允许范围。'
  }

  if (shortEdge < config.minShortEdge || pixels < config.minPixels) {
    return '图片尺寸过小，请选择更大的图片。'
  }

  if (aspectRatio > config.maxAspectRatio) {
    return '图片宽高比过于极端，请更换更接近常规比例的图片。'
  }

  return ''
}

function readImageSize(file) {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file)
    const image = new Image()

    image.onload = () => {
      URL.revokeObjectURL(objectUrl)
      resolve({ width: image.naturalWidth, height: image.naturalHeight })
    }

    image.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('当前文件无法被识别为图片。'))
    }

    image.src = objectUrl
  })
}

function buildAccept(config) {
  return [...config.allowedMimeTypes, ...config.allowedFileExtensions.map((value) => `.${value}`)].join(',')
}

async function safeJson(response) {
  try {
    return await response.json()
  } catch {
    return {}
  }
}

function formatBytes(bytes) {
  if (bytes < 1024) {
    return `${bytes} B`
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KiB`
  }

  return `${(bytes / 1024 / 1024).toFixed(1)} MiB`
}

export default App
