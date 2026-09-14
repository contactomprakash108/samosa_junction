import type { SelectHTMLAttributes, ReactNode } from 'react'


type SelectFieldProps = SelectHTMLAttributes<HTMLSelectElement> & {
  label: string
  error?: string
  children: ReactNode
}

export function SelectField({ label, error, id, className = '', children, ...props }: SelectFieldProps) {
  const fieldId = id ?? props.name
  return (
    <label className="block space-y-1.5" htmlFor={fieldId}>
      <span className="text-sm font-medium text-ink">{label}</span>
      <select
        id={fieldId}
        className={`w-full rounded-xl border bg-white px-3.5 py-2.5 text-ink outline-none ring-saffron/30 transition focus:ring-4 ${
          error ? 'border-red-500' : 'border-ink/15 focus:border-saffron'
        } ${className}`}
        {...props}
      >
        {children}
      </select>
      {error ? <span className="block text-xs text-red-700">{error}</span> : null}
    </label>
  )
}
