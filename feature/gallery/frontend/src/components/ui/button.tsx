import { motion } from 'framer-motion'
import { cva, type VariantProps } from 'class-variance-authority'
import type { HTMLMotionProps, Transition } from 'framer-motion'
import { cn } from '../../lib/utils'

export const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-full border text-sm font-medium outline-none will-change-transform transition-[filter,box-shadow,background-color,border-color,color] duration-180 ease-[cubic-bezier(0.22,1,0.36,1)] disabled:pointer-events-none',
  {
    variants: {
      variant: {
        default: 'border-ctp-blue bg-ctp-blue text-ctp-base hover:border-ctp-sapphire hover:bg-ctp-sapphire',
        outline: 'border-ctp-surface1 bg-ctp-base text-ctp-text hover:bg-ctp-surface0',
        ghost: 'border-transparent bg-transparent text-ctp-text hover:bg-ctp-surface0',
      },
      size: {
        default: 'h-11 px-5',
        sm: 'h-9 px-4 text-sm',
        lg: 'h-12 px-6 text-base',
      },
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  },
)

const defaultTapAnimation = { scale: 0.985 }
const defaultTapTransition: Transition = { type: 'spring', stiffness: 540, damping: 34, mass: 0.7 }

type ButtonProps = HTMLMotionProps<'button'> & VariantProps<typeof buttonVariants>

export function Button({
  className,
  variant,
  size,
  disabled,
  style,
  whileTap,
  transition,
  ...props
}: ButtonProps) {
  return (
    <motion.button
      disabled={disabled}
      whileTap={disabled ? undefined : whileTap ?? defaultTapAnimation}
      transition={transition ?? defaultTapTransition}
      style={{ transformOrigin: 'center center', ...style }}
      className={cn(buttonVariants({ variant, size }), className)}
      {...props}
    />
  )
}
