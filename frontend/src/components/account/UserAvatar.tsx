import { useState } from 'react'
import { initials } from '../../lib/userDisplay'

type UserAvatarProps = {
  name: string
  avatar?: string | null
  size?: 'sm' | 'lg'
}

export function UserAvatar({ name, avatar, size = 'sm' }: UserAvatarProps) {
  const [failedAvatar, setFailedAvatar] = useState<string | null>(null)
  const dimension = size === 'lg' ? 'h-16 w-16 text-lg' : 'h-8 w-8 text-xs'
  if (avatar && failedAvatar !== avatar) {
    return (
      <img
        src={avatar}
        alt=""
        className={`${dimension} rounded-full object-cover`}
        onError={() => setFailedAvatar(avatar)}
      />
    )
  }
  return (
    <span className={`flex ${dimension} items-center justify-center rounded-full bg-emerald-800 font-medium text-white`} aria-hidden="true">
      {initials(name)}
    </span>
  )
}
