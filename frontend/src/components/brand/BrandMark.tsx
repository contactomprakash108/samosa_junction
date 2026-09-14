import { Link } from 'react-router-dom'


export function BrandMark({ className = 'h-9 w-9' }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 40 40" aria-hidden="true">
      <rect width="40" height="40" rx="12" fill="#1f1915" />
      <path d="M20 8 L32 30 H8 Z" fill="#c45c26" />
      <path d="M20 14 L27.5 28 H12.5 Z" fill="#f6efe4" />
    </svg>
  )
}

export function BrandLink() {
  return (
    <Link to="/" className="flex items-center gap-2.5">
      <BrandMark />
      <span className="font-display text-xl leading-none text-ink">
        Samosa
        <span className="text-saffron"> Junction</span>
      </span>
    </Link>
  )
}
