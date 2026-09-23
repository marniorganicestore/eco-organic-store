import { useCartStore } from '../../store/cartStore'

export function CartNotice() {
  const notice = useCartStore((state) => state.notice)
  const tone = useCartStore((state) => state.noticeTone)
  const toneClass = tone === 'error'
    ? 'border-orange-200 bg-orange-50 text-orange-900'
    : 'border-emerald-200 bg-[#f8f6f1] text-emerald-950'

  return (
    <div aria-live="polite" className="pointer-events-none fixed inset-x-0 bottom-4 z-30 flex justify-center px-4 sm:justify-end sm:pr-6">
      {notice ? (
        <p className={`pointer-events-auto max-w-sm rounded-xl border px-4 py-3 text-sm shadow-lg ${toneClass}`}>
          {notice}
        </p>
      ) : null}
    </div>
  )
}
