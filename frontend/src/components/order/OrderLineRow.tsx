import { formatInr } from '../../lib/cart'
import { resolveImageSrc } from '../../lib/media'

export type OrderLineView = {
  productId: string
  productName: string
  pricePaise: number
  qty: number
  image?: string | null
}

export function OrderLineRow({ line }: { line: OrderLineView }) {
  const image = resolveImageSrc(line.image)
  return (
    <li className="flex items-center gap-3">
      {image ? (
        <img
          src={image}
          alt=""
          width={56}
          height={56}
          loading="lazy"
          decoding="async"
          className="h-14 w-14 shrink-0 rounded-lg object-cover"
        />
      ) : (
        <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-lg bg-emerald-50 text-[10px] font-medium text-emerald-800" aria-hidden="true">
          Organic
        </span>
      )}
      <p className="min-w-0 text-sm text-emerald-950">
        {line.productName} × {line.qty}
        <span className="text-slate-600"> · {formatInr(line.pricePaise)}</span>
      </p>
    </li>
  )
}
