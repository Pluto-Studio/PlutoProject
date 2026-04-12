import * as React from 'react'
import * as DialogPrimitive from '@radix-ui/react-dialog'
import { IconX } from '@tabler/icons-react'
import { cn } from '../../lib/utils.js'

const Dialog = DialogPrimitive.Root

const DialogTrigger = DialogPrimitive.Trigger

const DialogPortal = DialogPrimitive.Portal

const DialogClose = DialogPrimitive.Close

const DialogOverlay = React.forwardRef(function DialogOverlay({ className, ...props }, ref) {
  return (
    <DialogPrimitive.Overlay
      ref={ref}
      className={cn('dialog-overlay fixed inset-0 z-50 bg-ctp-crust/80', className)}
      {...props}
    />
  )
})

const DialogContent = React.forwardRef(function DialogContent(
  { className, children, closeLabel, showClose = true, ...props },
  ref,
) {
  return (
    <DialogPortal>
      <DialogOverlay />
      <DialogPrimitive.Content
        ref={ref}
        className="dialog-content fixed inset-0 z-50 grid place-items-center p-4 outline-none sm:p-6"
        {...props}
      >
        <div
          className={cn(
            'dialog-panel relative grid w-full max-w-md gap-4 rounded-[1.75rem] border border-ctp-surface1 bg-ctp-base p-6 shadow-2xl',
            className,
          )}
        >
          {children}
          {showClose ? (
            <DialogPrimitive.Close
              aria-label={closeLabel}
              className="absolute right-4 top-4 inline-flex size-10 items-center justify-center rounded-full text-ctp-subtext0 transition-[transform,background-color,color] duration-180 ease-[cubic-bezier(0.22,1,0.36,1)] hover:bg-ctp-surface0 hover:text-ctp-text active:translate-y-px active:scale-[0.97]"
            >
              <IconX className="size-4" stroke={1.8} />
            </DialogPrimitive.Close>
          ) : null}
        </div>
      </DialogPrimitive.Content>
    </DialogPortal>
  )
})

function DialogHeader({ className, ...props }) {
  return <div className={cn('space-y-3', className)} {...props} />
}

function DialogFooter({ className, ...props }) {
  return <div className={cn('flex justify-end', className)} {...props} />
}

const DialogTitle = React.forwardRef(function DialogTitle({ className, ...props }, ref) {
  return (
    <DialogPrimitive.Title
      ref={ref}
      className={cn('text-2xl font-semibold tracking-tight text-ctp-text', className)}
      {...props}
    />
  )
})

const DialogDescription = React.forwardRef(function DialogDescription({ className, ...props }, ref) {
  return (
    <DialogPrimitive.Description
      ref={ref}
      className={cn('text-sm leading-6 text-ctp-subtext1', className)}
      {...props}
    />
  )
})

export {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogOverlay,
  DialogPortal,
  DialogTitle,
  DialogTrigger,
}
