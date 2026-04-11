import { useEffect, useId, useMemo, useRef, useState } from 'react'
import { motion } from 'framer-motion'
import {
  IconCheck,
  IconChevronDown,
  IconDeviceDesktop,
  IconLoader2,
  IconMoon,
  IconSun,
  IconUpload,
} from '@tabler/icons-react'
import { Button } from './components/ui/button.jsx'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './components/ui/dialog.jsx'
import { cn, formatBinarySize, formatFormatList, getFileExtension } from './lib/utils.js'
import { texts } from './texts.js'

const THEME_STORAGE_KEY = 'gallery-upload-theme'

const themeModes = {
  system: 'system',
  light: 'light',
  dark: 'dark',
}

const uploadStates = {
  idle: 'idle',
  uploading: 'uploading',
  success: 'success',
}

const uploadButtonStyles = {
  [uploadStates.idle]: 'hover:bg-ctp-sapphire hover:border-ctp-sapphire',
  [uploadStates.uploading]: '',
  [uploadStates.success]: '',
}

const uploadButtonMotionStyles = {
  [uploadStates.idle]: {
    backgroundColor: 'var(--color-ctp-blue)',
    borderColor: 'var(--color-ctp-blue)',
    color: 'var(--color-ctp-base)',
  },
  [uploadStates.uploading]: {
    backgroundColor: 'var(--color-ctp-yellow)',
    borderColor: 'var(--color-ctp-yellow)',
    color: 'var(--color-ctp-crust)',
  },
  [uploadStates.success]: {
    backgroundColor: 'var(--color-ctp-green)',
    borderColor: 'var(--color-ctp-green)',
    color: 'var(--color-ctp-base)',
  },
}

const tapAnimation = { scale: 0.985 }
const tapTransition = { type: 'spring', stiffness: 540, damping: 34, mass: 0.7 }
const uploadButtonTransition = { duration: 0.24, ease: [0.22, 1, 0.36, 1] }

const themeModeButtons = [
  { value: themeModes.system, icon: IconDeviceDesktop },
  { value: themeModes.light, icon: IconSun },
  { value: themeModes.dark, icon: IconMoon },
]

function extractSessionId(pathname) {
  const match = pathname.match(/^\/upload\/([^/]+)\/?$/)
  return match?.[1] ?? null
}

function getStoredThemeMode() {
  if (typeof window === 'undefined') {
    return themeModes.system
  }

  const stored = window.localStorage.getItem(THEME_STORAGE_KEY)
  return Object.values(themeModes).includes(stored) ? stored : themeModes.system
}

function resolveTheme(mode, prefersDark) {
  if (mode === themeModes.system) {
    return prefersDark ? themeModes.dark : themeModes.light
  }

  return mode
}

function applyResolvedTheme(theme) {
  const root = document.documentElement
  root.classList.remove('latte', 'mocha')
  root.classList.add(theme === themeModes.dark ? 'mocha' : 'latte')
  root.style.colorScheme = theme === themeModes.dark ? 'dark' : 'light'
}

async function readImageDimensions(file) {
  const objectUrl = URL.createObjectURL(file)

  try {
    return await new Promise((resolve, reject) => {
      const image = new Image()
      image.onload = () => resolve({ width: image.naturalWidth, height: image.naturalHeight })
      image.onerror = () => reject(new Error('image-load-failed'))
      image.src = objectUrl
    })
  } finally {
    URL.revokeObjectURL(objectUrl)
  }
}

async function validateFile(file, config) {
  const extension = getFileExtension(file.name)
  const normalizedMime = file.type.toLowerCase()
  const supportsExtension = extension !== '' && config.supportedFileExtensions.includes(extension)
  const supportsMime = normalizedMime !== '' && config.supportedMimeTypes.includes(normalizedMime)

  if (!supportsExtension && !supportsMime) {
    return {
      title: texts.dialog.unsupportedTypeTitle,
      description: texts.dialog.unsupportedTypeDescription(formatFormatList(config.supportedFormatNames)),
    }
  }

  if (file.size > config.maxBytes) {
    return {
      title: texts.dialog.fileTooLargeTitle,
      description: texts.dialog.fileTooLargeDescription(formatBinarySize(config.maxBytes)),
    }
  }

  try {
    const { width, height } = await readImageDimensions(file)
    if (width * height > config.maxPixels) {
      return {
        title: texts.dialog.tooManyPixelsTitle,
        description: texts.dialog.tooManyPixelsDescription(
          config.suggestedMaxWidth,
          config.suggestedMaxHeight,
        ),
      }
    }
  } catch {
    return {
      title: texts.dialog.unreadableImageTitle,
      description: texts.dialog.unreadableImageDescription,
    }
  }

  return null
}

function getUploadErrorMessage(status) {
  switch (status) {
    case 404:
      return texts.uploadErrors.notFound
    case 409:
      return texts.uploadErrors.conflict
    case 410:
      return texts.uploadErrors.gone
    case 413:
      return texts.uploadErrors.payloadTooLarge
    default:
      return texts.uploadErrors.generic
  }
}

function ThemeModePicker({ currentMode, onChange }) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef(null)

  useEffect(() => {
    if (!open) {
      return undefined
    }

    function handlePointerDown(event) {
      if (!containerRef.current?.contains(event.target)) {
        setOpen(false)
      }
    }

    document.addEventListener('pointerdown', handlePointerDown)
    return () => document.removeEventListener('pointerdown', handlePointerDown)
  }, [open])

  return (
    <div ref={containerRef} className="relative flex justify-end">
      <motion.button
        type="button"
        onClick={() => setOpen((current) => !current)}
        initial={false}
        whileTap={tapAnimation}
        transition={tapTransition}
        style={{ transformOrigin: 'center center' }}
        className="inline-flex h-11 items-center gap-2 rounded-full border border-ctp-surface1 bg-ctp-mantle px-4 text-sm font-medium text-ctp-text transition-[background-color,box-shadow] duration-180 ease-[cubic-bezier(0.22,1,0.36,1)] hover:bg-ctp-surface0"
        aria-expanded={open}
        aria-haspopup="menu"
      >
        {texts.theme.options[currentMode]}
        <IconChevronDown className={cn('size-4 transition-transform', open && 'rotate-180')} stroke={1.8} />
      </motion.button>

      {open ? (
        <div className="absolute right-0 top-full z-10 mt-2 min-w-40 overflow-hidden rounded-2xl border border-ctp-surface1 bg-ctp-mantle p-1 shadow-xl">
        {themeModeButtons.map(({ value, icon: Icon }) => {
          const active = currentMode === value

          return (
            <button
              key={value}
              type="button"
              onClick={() => {
                onChange(value)
                setOpen(false)
              }}
              aria-label={texts.theme.options[value]}
              className={cn(
                'inline-flex h-10 w-full items-center gap-2 rounded-[1rem] px-3 text-left text-sm font-medium transition-[transform,background-color,color] duration-180 ease-[cubic-bezier(0.22,1,0.36,1)] active:translate-y-px active:scale-[0.985]',
                active
                  ? 'bg-ctp-surface1 text-ctp-text'
                  : 'text-ctp-subtext0 hover:bg-ctp-surface0 hover:text-ctp-text',
              )}
              aria-pressed={active}
            >
              <Icon className="size-4" stroke={1.8} />
              <span>{texts.theme.options[value]}</span>
            </button>
          )
        })}
        </div>
      ) : null}
    </div>
  )
}

function LoadingState() {
  return (
    <div className="space-y-3 rounded-[1.75rem] border border-ctp-surface1 bg-ctp-mantle p-6">
      <div className="flex items-center gap-3 text-ctp-text">
        <IconLoader2 className="size-5 animate-spin text-ctp-blue" stroke={1.8} />
        <span className="text-sm font-medium">{texts.config.loading}</span>
      </div>
      <p className="text-sm leading-6 text-ctp-subtext1">{texts.config.loadingDescription}</p>
    </div>
  )
}

function ErrorState({ message, onRetry }) {
  return (
    <div className="space-y-4 rounded-[1.75rem] border border-ctp-red bg-ctp-mantle p-6">
      <div className="space-y-2">
        <h2 className="text-xl font-semibold text-ctp-text">{texts.config.errorTitle}</h2>
        <p className="text-sm leading-6 text-ctp-subtext1">{message}</p>
      </div>
      {onRetry ? (
        <Button type="button" variant="outline" onClick={onRetry}>
          {texts.actions.retry}
        </Button>
      ) : null}
    </div>
  )
}

export default function App() {
  const fileInputId = useId()
  const sessionId = useMemo(() => extractSessionId(window.location.pathname), [])
  const [configReloadToken, setConfigReloadToken] = useState(0)
  const [themeMode, setThemeMode] = useState(getStoredThemeMode)
  const [configState, setConfigState] = useState({ status: 'loading', data: null, error: null })
  const fileValidationTokenRef = useRef(0)
  const [selectedFile, setSelectedFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState(null)
  const [dragActive, setDragActive] = useState(false)
  const [uploadState, setUploadState] = useState(uploadStates.idle)
  const [statusText, setStatusText] = useState(texts.uploadStatus.idle)
  const [uploadError, setUploadError] = useState('')
  const [dialogContent, setDialogContent] = useState({ open: false, title: '', description: '' })

  useEffect(() => {
    document.title = texts.documentTitle
  }, [])

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    const syncTheme = () => {
      const nextTheme = resolveTheme(themeMode, mediaQuery.matches)
      applyResolvedTheme(nextTheme)
    }

    syncTheme()
    window.localStorage.setItem(THEME_STORAGE_KEY, themeMode)

    if (themeMode !== themeModes.system) {
      return undefined
    }

    mediaQuery.addEventListener('change', syncTheme)
    return () => mediaQuery.removeEventListener('change', syncTheme)
  }, [themeMode])

  useEffect(() => {
    if (!selectedFile) {
      setPreviewUrl(null)
      return undefined
    }

    const objectUrl = URL.createObjectURL(selectedFile)
    setPreviewUrl(objectUrl)

    return () => URL.revokeObjectURL(objectUrl)
  }, [selectedFile])

  useEffect(() => {
    if (!sessionId) {
      setConfigState({ status: 'error', data: null, error: texts.config.invalidSessionDescription })
      return
    }

    let cancelled = false

    async function loadConfig() {
      setConfigState({ status: 'loading', data: null, error: null })

      try {
        const response = await fetch('/api/config', { cache: 'no-store' })
        if (!response.ok) {
          throw new Error('config-response-not-ok')
        }

        const config = await response.json()
        if (!cancelled) {
          setConfigState({ status: 'ready', data: config, error: null })
        }
      } catch {
        if (!cancelled) {
          setConfigState({
            status: 'error',
            data: null,
            error: texts.config.errorDescription,
          })
        }
      }
    }

    loadConfig()

    return () => {
      cancelled = true
    }
  }, [configReloadToken, sessionId])

  const config = configState.data
  const canSelectFile = configState.status === 'ready' && uploadState !== uploadStates.uploading && uploadState !== uploadStates.success
  const canUpload = Boolean(selectedFile) && canSelectFile
  const supportText = config ? texts.dropzone.supportedFormats(formatFormatList(config.supportedFormatNames)) : texts.config.loadingHint
  const accept = config
    ? [...config.supportedMimeTypes, ...config.supportedFileExtensions.map((extension) => `.${extension}`)].join(',')
    : undefined

  function updateSelectedFile(file) {
    setSelectedFile(file)
    setUploadState(uploadStates.idle)
    setStatusText(texts.uploadStatus.idle)
    setUploadError('')
  }

  async function trySelectFile(file) {
    if (!config) {
      return
    }

    const currentValidationToken = fileValidationTokenRef.current + 1
    fileValidationTokenRef.current = currentValidationToken

    const validationMessage = await validateFile(file, config)
    if (currentValidationToken !== fileValidationTokenRef.current) {
      return
    }

    if (validationMessage) {
      openValidationDialog(validationMessage.title, validationMessage.description)
      return
    }

    updateSelectedFile(file)
  }

  function handleFileChange(event) {
    const nextFile = event.target.files?.[0]
    event.target.value = ''

    if (!nextFile) {
      return
    }

    void trySelectFile(nextFile)
  }

  function openValidationDialog(title, description) {
    setDialogContent({ open: true, title, description })
  }

  function handleDrop(event) {
    event.preventDefault()
    setDragActive(false)

    if (!canSelectFile) {
      return
    }

    const nextFile = event.dataTransfer.files?.[0]
    if (!nextFile) {
      return
    }

    void trySelectFile(nextFile)
  }

  async function handleUpload() {
    if (!config || !selectedFile || !sessionId || !canUpload) {
      return
    }

    setUploadState(uploadStates.uploading)
    setStatusText(texts.uploadStatus.uploading)
    setUploadError('')

    try {
      const formData = new FormData()
      formData.append('file', selectedFile)

      const response = await fetch(`/api/sessions/${sessionId}/upload`, {
        method: 'POST',
        body: formData,
      })

      if (!response.ok) {
        throw new Error(String(response.status))
      }

      setUploadState(uploadStates.success)
      setStatusText(texts.uploadStatus.success)
    } catch (error) {
      const status = Number(error.message)
      setUploadState(uploadStates.idle)
      setStatusText(texts.uploadStatus.failed)
      setUploadError(getUploadErrorMessage(Number.isNaN(status) ? 0 : status))
    }
  }

  const UploadIcon =
    uploadState === uploadStates.uploading
      ? IconLoader2
      : uploadState === uploadStates.success
        ? IconCheck
        : IconUpload

  return (
    <main className="min-h-screen bg-ctp-base text-ctp-text">
      <div className="mx-auto flex min-h-screen w-full max-w-3xl flex-col px-4 py-4 sm:px-6 lg:px-8">
        <header className="flex justify-end pb-4">
          <ThemeModePicker currentMode={themeMode} onChange={setThemeMode} />
        </header>

        <section className="flex flex-1 items-center justify-center py-4 sm:py-6 lg:py-8">
          <div className="w-full">
            <section className="rounded-[2rem] border border-ctp-surface1 bg-ctp-mantle p-4 sm:p-6 lg:p-7">
              {configState.status === 'loading' ? (
                <LoadingState />
              ) : configState.status === 'error' ? (
                <ErrorState
                  message={configState.error ?? texts.config.errorDescription}
                  onRetry={sessionId ? () => setConfigReloadToken((current) => current + 1) : null}
                />
              ) : (
                <div className="space-y-6">
                  <div className="space-y-2">
                    <h2 className="text-2xl font-semibold text-ctp-text sm:text-3xl">{texts.panel.title}</h2>
                  </div>

                  <label
                    htmlFor={fileInputId}
                    onDragOver={(event) => {
                      event.preventDefault()
                      if (canSelectFile) {
                        setDragActive(true)
                      }
                    }}
                    onDragEnter={(event) => {
                      event.preventDefault()
                      if (canSelectFile) {
                        setDragActive(true)
                      }
                    }}
                    onDragLeave={(event) => {
                      event.preventDefault()
                      if (event.currentTarget.contains(event.relatedTarget)) {
                        return
                      }

                      setDragActive(false)
                    }}
                    onDrop={handleDrop}
                    className={cn(
                      'group flex min-h-[22rem] cursor-pointer flex-col items-center justify-center rounded-[1.75rem] border border-dashed p-4 text-center transition-colors sm:p-5',
                      dragActive
                        ? 'border-ctp-blue bg-ctp-crust'
                        : 'border-ctp-surface2 bg-ctp-base hover:border-ctp-blue hover:bg-ctp-crust',
                      !canSelectFile && 'cursor-not-allowed opacity-70 hover:border-ctp-surface2 hover:bg-ctp-base',
                    )}
                  >
                    <input
                      id={fileInputId}
                      type="file"
                      className="sr-only"
                      accept={accept}
                      disabled={!canSelectFile}
                      onChange={handleFileChange}
                    />

                    {previewUrl ? (
                      <div className="flex w-full max-w-md flex-col items-center space-y-4 text-center">
                        <div className="overflow-hidden rounded-[1.5rem] border border-ctp-surface1 bg-ctp-crust">
                          <img
                            src={previewUrl}
                            alt={texts.preview.alt}
                            className="aspect-[4/3] w-full object-contain"
                          />
                        </div>

                        <div className="space-y-1">
                          <p className="truncate text-base font-medium text-ctp-text">{selectedFile.name}</p>
                          <p className="text-sm text-ctp-subtext0">
                            {texts.preview.fileSize(formatBinarySize(selectedFile.size))}
                          </p>
                        </div>
                      </div>
                    ) : (
                      <div className="flex flex-col items-center space-y-4">
                        <div className="inline-flex size-12 items-center justify-center rounded-2xl border border-ctp-surface1 bg-ctp-mantle text-ctp-blue">
                          <IconUpload className="size-6" stroke={1.8} />
                        </div>
                        <p className="text-xl font-semibold text-ctp-text">{texts.dropzone.title}</p>
                      </div>
                    )}
                  </label>

                  <p className="text-sm leading-6 text-ctp-subtext0">{supportText}</p>

                  <div className="space-y-3 border-t border-ctp-surface0 pt-5">
                    {statusText ? <p className="text-sm leading-6 text-ctp-subtext1">{statusText}</p> : null}
                    {uploadError ? <p className="text-sm leading-6 text-ctp-red">{uploadError}</p> : null}
                    <Button
                      type="button"
                      onClick={handleUpload}
                      disabled={!canUpload}
                      initial={false}
                      animate={uploadButtonMotionStyles[uploadState]}
                      transition={uploadButtonTransition}
                      className={cn(
                        'w-full justify-center rounded-[1.2rem] border text-base shadow-[0_12px_24px_-14px_rgba(17,17,27,0.55)] hover:brightness-105',
                        uploadButtonStyles[uploadState],
                        selectedFile ? 'disabled:opacity-100' : 'disabled:opacity-50',
                      )}
                    >
                      <UploadIcon
                        className={cn('size-5', uploadState === uploadStates.uploading && 'animate-spin')}
                        stroke={1.8}
                      />
                      {texts.uploadButton[uploadState]}
                    </Button>
                  </div>
                </div>
              )}
            </section>
          </div>
        </section>
      </div>

      <Dialog
        open={dialogContent.open}
        onOpenChange={(open) => setDialogContent((current) => ({ ...current, open }))}
      >
        <DialogContent closeLabel={texts.actions.closeDialog}>
          <DialogHeader>
            <DialogTitle>{dialogContent.title}</DialogTitle>
            <DialogDescription>{dialogContent.description}</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              type="button"
              className="hover:bg-ctp-blue hover:border-ctp-blue hover:brightness-110"
              onClick={() => setDialogContent((current) => ({ ...current, open: false }))}
            >
              {texts.actions.confirm}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </main>
  )
}
