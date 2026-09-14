import type { InputHTMLAttributes, ReactNode } from 'react'


type FieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string
  error?: string
  hint?: ReactNode
}

export function Field({ label, error, hint, id, className = '', ...props }: FieldProps) {
  const fieldId = id ?? props.name
  return (
    <label className="block space-y-1.5" htmlFor={fieldId}>
      <span className="text-sm font-medium text-ink">{label}</span>
      <input
        id={fieldId}
        className={`w-full rounded-xl border bg-white px-3.5 py-2.5 text-ink outline-none ring-saffron/30 transition focus:ring-4 ${
          error ? 'border-red-500' : 'border-ink/15 focus:border-saffron'
        } ${className}`}
        {...props}
      />
      {hint && !error ? <span className="block text-xs text-ink/60">{hint}</span> : null}
      {error ? <span className="block text-xs text-red-700">{error}</span> : null}
    </label>
  )
}
