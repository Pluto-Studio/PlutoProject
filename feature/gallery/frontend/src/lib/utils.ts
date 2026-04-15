import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs))
}

export function formatBinarySize(bytes: number): string {
  const units = ['KiB', 'MiB', 'GiB', 'TiB']
  let value = Math.max(bytes / 1024, 1)
  let unitIndex = 0

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex += 1
  }

  const maximumFractionDigits = value >= 10 ? 0 : 1

  return `${new Intl.NumberFormat('zh-CN', { maximumFractionDigits }).format(value)} ${units[unitIndex]}`
}

export function formatFormatList(formats: string[]): string {
  return formats.join('、')
}

export function getFileExtension(fileName: string): string {
  const parts = fileName.toLowerCase().split('.')
  return parts.length > 1 ? parts[parts.length - 1] ?? '' : ''
}
