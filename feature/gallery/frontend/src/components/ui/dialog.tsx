import * as React from 'react'
import * as DialogPrimitive from '@radix-ui/react-dialog'
import { motion } from 'framer-motion'
import { IconX } from '@tabler/icons-react'
import { cn } from '../../lib/utils'

const Dialog = DialogPrimitive.Root

const DialogTrigger = DialogPrimitive.Trigger

const DialogPortal = DialogPrimitive.Portal

const DialogClose = DialogPrimitive.Close

const DialogOverlay = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Overlay>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Overlay>
>(function DialogOverlay({ className, ...props }, ref) {
  return (
    <DialogPrimitive.Overlay
      ref={ref}
      className={cn('dialog-overlay fixed inset-0 z-50 bg-ctp-crust/80', className)}
      {...props}
    />
  )
})

type DialogContentProps = React.ComponentPropsWithoutRef<typeof DialogPrimitive.Content> & {
  closeLabel?: string
  showClose?: boolean
}

const DialogContent = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Content>,
  DialogContentProps
>(function DialogContent({ className, children, closeLabel, showClose = true, ...props }, ref) {
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
            <motion.div
              whileTap={{ scale: 0.97 }}
              transition={{ type: 'spring', stiffness: 540, damping: 28, mass: 0.6 }}
              className="absolute right-4 top-4 inline-flex"
            >
              <DialogPrimitive.Close
                aria-label={closeLabel}
                className="inline-flex size-10 items-center justify-center rounded-full text-ctp-subtext0 transition-[background-color,color] duration-180 ease-[cubic-bezier(0.22,1,0.36,1)] hover:bg-ctp-surface0 hover:text-ctp-text"
              >
                <IconX className="size-4" stroke={1.8} />
              </DialogPrimitive.Close>
            </motion.div>
          ) : null}
        </div>
      </DialogPrimitive.Content>
    </DialogPortal>
  )
})

function DialogHeader({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('space-y-3', className)} {...props} />
}

function DialogFooter({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('flex justify-end', className)} {...props} />
}

const DialogTitle = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Title>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Title>
>(function DialogTitle({ className, ...props }, ref) {
  return (
    <DialogPrimitive.Title
      ref={ref}
      className={cn('text-2xl font-semibold tracking-tight text-ctp-text', className)}
      {...props}
    />
  )
})

const DialogDescription = React.forwardRef<
  React.ElementRef<typeof DialogPrimitive.Description>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Description>
>(function DialogDescription({ className, ...props }, ref) {
  return (
    <DialogPrimitive.Description
      ref={ref}
      className={cn('text-sm leading-6 text-ctp-subtext1', className)}
      {...props}
    />
  )
})

DialogOverlay.displayName = DialogPrimitive.Overlay.displayName
DialogContent.displayName = DialogPrimitive.Content.displayName
DialogTitle.displayName = DialogPrimitive.Title.displayName
DialogDescription.displayName = DialogPrimitive.Description.displayName

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
