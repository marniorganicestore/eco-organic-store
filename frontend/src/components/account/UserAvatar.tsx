import { useState } from 'react'
import { resolveImageSrc } from '../../lib/media'
import { initials } from '../../lib/userDisplay'

type UserAvatarProps = {
  name: string
  avatar?: string | null
  size?: 'sm' | 'lg' | 'xl'
  alt?: string
}

export function UserAvatar({ name, avatar, size = 'sm', alt = '' }: UserAvatarProps) {
  const [failedAvatar, setFailedAvatar] = useState<string | null>(null)
  const src = resolveImageSrc(avatar)
  const dimension = size === 'xl' ? 'h-24 w-24 text-2xl' : size === 'lg' ? 'h-16 w-16 text-lg' : 'h-8 w-8 text-xs'
  if (src && failedAvatar !== avatar) {
    return (
      <img
        src={src}
        alt={alt}
        className={`${dimension} rounded-full object-cover ring-2 ring-white`}
        onError={() => setFailedAvatar(avatar ?? null)}
      />
    )
  }
  return (
    <span className={`flex ${dimension} items-center justify-center rounded-full bg-emerald-800 font-medium text-white`} aria-hidden="true">
      {initials(name)}
    </span>
  )
}
