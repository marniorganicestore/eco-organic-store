import { useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ApiError, api } from '../lib/api'
import { cartCollectionPath, cartQueryKey, rememberCart, withQuantity, type CartView } from '../lib/cart'
import { useAuthStore } from '../store/authStore'
import { useCartStore } from '../store/cartStore'

function useCartScope(): string {
  const guestToken = useCartStore((state) => state.guestToken)
  const signedIn = useAuthStore((state) => Boolean(state.accessToken))
  return signedIn ? 'account' : guestToken
}

export function useCart() {
  const scope = useCartScope()
  const guestToken = useCartStore((state) => state.guestToken)
  const bootstrapped = useAuthStore((state) => state.bootstrapped)
  const acceptingCart = useCartStore((state) => state.acceptingCart)
  const query = useQuery({
    queryKey: cartQueryKey(scope),
    queryFn: () => api.get<CartView>(cartCollectionPath(guestToken)),
    enabled: bootstrapped && acceptingCart
  })

  const items = query.data?.items
  useEffect(() => {
    if (items) useCartStore.getState().setItems(items)
  }, [items])

  return query
}

export function useAddToCart() {
  const queryClient = useQueryClient()
  const scope = useCartScope()
  const guestToken = useCartStore((state) => state.guestToken)
  return useMutation({
    mutationFn: ({ productId, qty }: { productId: string; qty: number }) =>
      api.post<CartView>(cartCollectionPath(guestToken), { productId, qty }),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: cartQueryKey(scope) })
    },
    onSuccess: (cart, variables) => {
      rememberCart(cart, scope)
      const line = cart.items.find((item) => item.productId === variables.productId)
      const name = line?.name || 'Item'
      const message = variables.qty > 1
        ? `Added ${variables.qty} × ${name} to your basket.`
        : `Added ${name} to your basket.`
      useCartStore.getState().flash(message, 'success')
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : 'Unable to add this item.'
      useCartStore.getState().flash(message, 'error')
    }
  })
}

export function useSetCartQty() {
  const queryClient = useQueryClient()
  const scope = useCartScope()
  const guestToken = useCartStore((state) => state.guestToken)
  return useMutation({
    mutationFn: ({ productId, qty }: { productId: string; qty: number }) =>
      api.patch<CartView>(cartCollectionPath(guestToken), { productId, qty }),
    onMutate: async (variables) => {
      await queryClient.cancelQueries({ queryKey: cartQueryKey(scope) })
      const previous = queryClient.getQueryData<CartView>(cartQueryKey(scope))
      if (previous) {
        const next = withQuantity(previous, variables.productId, variables.qty)
        queryClient.setQueryData(cartQueryKey(scope), next)
        useCartStore.getState().setItems(next.items)
      }
      return { previous }
    },
    onError: (error, _variables, context) => {
      if (context?.previous) {
        queryClient.setQueryData(cartQueryKey(scope), context.previous)
        useCartStore.getState().setItems(context.previous.items)
      }
      const message = error instanceof ApiError ? error.message : 'Unable to update your basket.'
      useCartStore.getState().flash(message, 'error')
    },
    onSuccess: (cart) => {
      rememberCart(cart, scope)
    }
  })
}
