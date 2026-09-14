import type { ButtonHTMLAttributes } from 'react'


type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'ghost' | 'secondary'
  pending?: boolean
}

export function Button({
  variant = 'primary',
  pending = false,
  className = '',
  disabled,
  children,
  ...props
}: ButtonProps) {
  const styles = {
    primary: 'bg-saffron text-cream hover:bg-saffron-dark',
    secondary: 'bg-ink text-cream hover:bg-ink/90',
    ghost: 'bg-transparent text-ink hover:bg-ink/5 border border-ink/15',
  }[variant]

  return (
    <button
      className={`inline-flex items-center justify-center rounded-full px-5 py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-60 ${styles} ${className}`}
      disabled={disabled || pending}
      {...props}
    >
      {pending ? 'Please wait…' : children}
    </button>
  )
}
