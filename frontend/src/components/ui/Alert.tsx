
type AlertProps = {
  tone?: 'error' | 'info' | 'success'
  children: string
}

export function Alert({ tone = 'error', children }: AlertProps) {
  const styles = {
    error: 'border-red-200 bg-red-50 text-red-800',
    info: 'border-ink/10 bg-white text-ink/80',
    success: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  }[tone]

  return (
    <p className={`rounded-xl border px-4 py-3 text-sm ${styles}`} role={tone === 'error' ? 'alert' : 'status'}>
      {children}
    </p>
  )
}
