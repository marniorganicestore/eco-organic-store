import { STORE_NAME } from '../../lib/brand'

type BrandMarkProps = {
  className?: string
  alt?: string
  priority?: boolean
}

export function BrandMark({ className = 'h-12 w-auto', alt = STORE_NAME, priority = false }: BrandMarkProps) {
  return (
    <img
      src="/brand/logo-light.png"
      alt={alt}
      width={640}
      height={226}
      decoding={priority ? 'sync' : 'async'}
      fetchPriority={priority ? 'high' : 'auto'}
      className={className}
    />
  )
}

type BrandPlateProps = {
  className?: string
  imgClassName?: string
  alt?: string
}

/** Cream plate so the forest wordmark stays readable on photography. */
export function BrandPlate({
  className = '',
  imgClassName = 'h-16 w-auto sm:h-20',
  alt
}: BrandPlateProps) {
  return (
    <div className={`inline-flex rounded-2xl bg-[#faf4e8]/95 p-2.5 shadow-[0_18px_50px_rgba(2,20,12,0.35)] ring-1 ring-white/50 ${className}`}>
      <BrandMark className={imgClassName} alt={alt} priority />
    </div>
  )
}
