import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import {
  toCategoryWrite,
  toProductWrite,
  type AdminCategory,
  type AdminProduct,
  type CategoryDraft,
  type ProductDraft
} from '../lib/adminDesk'
import { ADMIN_PAGE_SIZE, idsQuery, pageQuery, type PageResult } from '../lib/page'

export const adminProductsKey = ['admin', 'products'] as const
export const adminCategoriesKey = ['admin', 'categories'] as const

export function useAdminProducts(page: number, q = '') {
  return useQuery({
    queryKey: [...adminProductsKey, page, q],
    queryFn: () => api.get<PageResult<AdminProduct>>(`/admin/catalog/products${pageQuery(page, ADMIN_PAGE_SIZE, { q: q || undefined })}`)
  })
}

export function useAdminProductLookup(ids: string[]) {
  const key = [...ids].filter(Boolean).sort().join(',')
  return useQuery({
    queryKey: ['admin', 'product-lookup', key],
    queryFn: () => api.get<AdminProduct[]>(`/admin/catalog/products/lookup?${idsQuery(ids)}`),
    enabled: ids.length > 0
  })
}

export function useCatalogDesk() {
  return useQuery({
    queryKey: ['admin', 'desk', 'catalog'],
    queryFn: () => api.get<{ productCount: number; categoryCount: number }>('/admin/catalog/summary')
  })
}

export function useAdminCategories() {
  return useQuery({
    queryKey: adminCategoriesKey,
    queryFn: () => api.get<AdminCategory[]>('/admin/catalog/categories')
  })
}

export function useSaveProduct() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, draft }: { id?: string; draft: ProductDraft }) => {
      const body = toProductWrite(draft)
      return id
        ? api.put<AdminProduct>(`/admin/catalog/products/${id}`, body)
        : api.post<AdminProduct>('/admin/catalog/products', body)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminProductsKey })
    }
  })
}

export function useDeleteProduct() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api.delete<void>(`/admin/catalog/products/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminProductsKey })
    }
  })
}

export function useSaveCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, draft }: { id?: string; draft: CategoryDraft }) => {
      const body = toCategoryWrite(draft)
      return id
        ? api.put<AdminCategory>(`/admin/catalog/categories/${id}`, body)
        : api.post<AdminCategory>('/admin/catalog/categories', body)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminCategoriesKey })
    }
  })
}

export function useDeleteCategory() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: string) => api.delete<void>(`/admin/catalog/categories/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminCategoriesKey })
    }
  })
}
