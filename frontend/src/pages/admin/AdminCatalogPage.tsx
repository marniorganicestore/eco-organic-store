import { useEffect, useMemo, useState } from 'react'
import { ApiError } from '../../lib/api'
import {
  categoryToDraft,
  emptyCategoryDraft,
  emptyProductDraft,
  formatInr,
  productToDraft,
  type AdminCategory,
  type AdminProduct,
  type CategoryDraft,
  type ProductDraft
} from '../../lib/adminDesk'
import {
  useAdminCategories,
  useAdminProducts,
  useDeleteCategory,
  useDeleteProduct,
  useSaveCategory,
  useSaveProduct
} from '../../hooks/useAdminCatalog'
import { AdminPending } from '../../components/admin/AdminPending'
import { CategoryForm } from '../../components/admin/CategoryForm'
import { ProductForm } from '../../components/admin/ProductForm'
import { StatusPill } from '../../components/admin/StatusPill'
import { FormBanner } from '../../components/account/FormBanner'
import { Pager } from '../../components/layout/Pager'
import { useClampPage } from '../../hooks/useClampPage'
import { PageShell, storeBtn, storeBtnGhost, storeCard, storeInput } from '../../components/layout/PageShell'

type ProductEditor = { id?: string; draft: ProductDraft }
type CategoryEditor = { id?: string; draft: CategoryDraft }

export default function AdminCatalogPage() {
  const [page, setPage] = useState(0)
  const [q, setQ] = useState('')
  const products = useAdminProducts(page, q)
  const categories = useAdminCategories()
  const saveProduct = useSaveProduct()
  const removeProduct = useDeleteProduct()
  const saveCategory = useSaveCategory()
  const removeCategory = useDeleteCategory()
  const [query, setQuery] = useState('')

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setQ(query.trim())
      setPage(0)
    }, 300)
    return () => window.clearTimeout(timer)
  }, [query])
  const [productEditor, setProductEditor] = useState<ProductEditor | null>(null)
  const [categoryEditor, setCategoryEditor] = useState<CategoryEditor | null>(null)
  const [productError, setProductError] = useState('')
  const [categoryError, setCategoryError] = useState('')
  const [confirmProduct, setConfirmProduct] = useState<string | null>(null)
  const [confirmCategory, setConfirmCategory] = useState<string | null>(null)

  const categoryName = useMemo(() => {
    const names = new Map<string, string>()
    for (const category of categories.data ?? []) names.set(category.id, category.name)
    return names
  }, [categories.data])

  const visible = products.data?.items ?? []
  useClampPage(page, products.data?.totalPages, setPage)

  async function submitProduct(draft: ProductDraft) {
    setProductError('')
    try {
      await saveProduct.mutateAsync({ id: productEditor?.id, draft })
      setProductEditor(null)
    } catch (err) {
      setProductError(err instanceof ApiError ? err.message : 'Unable to save that product.')
    }
  }

  async function submitCategory(draft: CategoryDraft) {
    setCategoryError('')
    try {
      await saveCategory.mutateAsync({ id: categoryEditor?.id, draft })
      setCategoryEditor(null)
    } catch (err) {
      setCategoryError(err instanceof ApiError ? err.message : 'Unable to save that category.')
    }
  }

  async function deleteProduct(id: string) {
    setProductError('')
    try {
      await removeProduct.mutateAsync(id)
      setConfirmProduct(null)
      if (productEditor?.id === id) setProductEditor(null)
    } catch (err) {
      setProductError(err instanceof ApiError ? err.message : 'Unable to remove that product.')
    }
  }

  async function deleteCategory(id: string) {
    setCategoryError('')
    try {
      await removeCategory.mutateAsync(id)
      setConfirmCategory(null)
      if (categoryEditor?.id === id) setCategoryEditor(null)
    } catch (err) {
      setCategoryError(err instanceof ApiError ? err.message : 'Unable to remove that category.')
    }
  }

  return (
    <PageShell
      title="Catalog"
      subtitle="Products and categories the shop shows. Stock lives on the inventory desk."
      actions={<button type="button" className={storeBtn} onClick={() => openProduct()}>Add product</button>}
    >
      {products.isPending || categories.isPending ? <AdminPending label="Loading the catalog..." /> : null}
      {products.isError || categories.isError ? (
        <FormBanner tone="error">Unable to load the catalog. Refresh and try again.</FormBanner>
      ) : null}
      {productEditor ? (
        <div className="mb-4">
          <ProductForm
            key={productEditor.id ?? 'new-product'}
            title={productEditor.id ? 'Edit product' : 'New product'}
            categories={categories.data ?? []}
            initial={productEditor.draft}
            preserveSlug={Boolean(productEditor.id)}
            pending={saveProduct.isPending}
            serverError={productError}
            onSubmit={submitProduct}
            onCancel={() => { setProductEditor(null); setProductError('') }}
          />
        </div>
      ) : null}
      {products.data ? (
        <section className={`${storeCard} overflow-x-auto p-4`}>
          <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
            <h2 className="font-semibold text-emerald-950">Products</h2>
            <input
              className={`${storeInput} w-full sm:w-64`}
              value={query}
              placeholder="Search name or origin"
              aria-label="Search products"
              onChange={(event) => setQuery(event.target.value)}
            />
          </div>
          {productError && !productEditor ? <div className="mb-3"><FormBanner tone="error">{productError}</FormBanner></div> : null}
          {visible.length === 0 ? (
            <p className="text-sm text-slate-600">No products match. Add one, or clear the search.</p>
          ) : (
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-emerald-800/80">
                <tr>
                  <th className="px-2 py-2 font-medium">Product</th>
                  <th className="px-2 py-2 font-medium">Category</th>
                  <th className="px-2 py-2 font-medium">Price</th>
                  <th className="px-2 py-2 font-medium">Shop</th>
                  <th className="px-2 py-2 font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((product) => (
                  <tr key={product.id} className="border-t border-emerald-100">
                    <td className="px-2 py-3">
                      <p className="font-medium text-emerald-950">{product.name}</p>
                      <p className="text-slate-600">{product.unit} · {product.origin || 'Origin not set'}</p>
                    </td>
                    <td className="px-2 py-3">{categoryName.get(product.categoryId) ?? 'Uncategorised'}</td>
                    <td className="px-2 py-3">{formatInr(product.pricePaise)}</td>
                    <td className="px-2 py-3">
                      <span className="flex flex-wrap gap-1">
                        <StatusPill status={product.active ? 'Active' : 'Hidden'} label={product.active ? 'In shop' : 'Hidden'} />
                        {product.featured ? <StatusPill status="CONFIRMED" label="Featured" /> : null}
                      </span>
                    </td>
                    <td className="px-2 py-3">
                      <div className="flex flex-wrap gap-2">
                        <button type="button" className={storeBtnGhost} onClick={() => editProduct(product)}>Edit</button>
                        {confirmProduct === product.id ? (
                          <>
                            <button type="button" className={storeBtnGhost} onClick={() => deleteProduct(product.id)}>
                              {removeProduct.isPending ? 'Removing...' : 'Confirm remove'}
                            </button>
                            <button type="button" className={storeBtnGhost} onClick={() => setConfirmProduct(null)}>Cancel</button>
                          </>
                        ) : (
                          <button type="button" className={storeBtnGhost} onClick={() => setConfirmProduct(product.id)}>Remove</button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {products.data ? (
            <Pager
              page={products.data.page}
              size={products.data.size}
              totalElements={products.data.totalElements}
              totalPages={products.data.totalPages}
              onPage={setPage}
              label="Product pages"
            />
          ) : null}
        </section>
      ) : null}

      <section className="mt-6">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 className="text-xl font-semibold text-emerald-950">Categories</h2>
          <button type="button" className={storeBtnGhost} onClick={() => openCategory()}>Add category</button>
        </div>
        {categoryEditor ? (
          <div className="mb-4">
            <CategoryForm
              key={categoryEditor.id ?? 'new-category'}
              title={categoryEditor.id ? 'Edit category' : 'New category'}
              initial={categoryEditor.draft}
              preserveSlug={Boolean(categoryEditor.id)}
              pending={saveCategory.isPending}
              serverError={categoryError}
              onSubmit={submitCategory}
              onCancel={() => { setCategoryEditor(null); setCategoryError('') }}
            />
          </div>
        ) : null}
        {categoryError && !categoryEditor ? <div className="mb-3"><FormBanner tone="error">{categoryError}</FormBanner></div> : null}
        {categories.data && categories.data.length === 0 ? (
          <p className={`${storeCard} p-6 text-sm text-slate-600`}>Add a category before the first product.</p>
        ) : null}
        {categories.data && categories.data.length > 0 ? (
          <ul className={`${storeCard} divide-y divide-emerald-100`}>
            {categories.data.map((category) => (
              <li key={category.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3 text-sm">
                <div>
                  <p className="font-medium text-emerald-950">{category.name}</p>
                  <p className="text-slate-600">{category.slug} · sort {category.sortOrder}</p>
                </div>
                <div className="flex gap-2">
                  <button type="button" className={storeBtnGhost} onClick={() => editCategory(category)}>Edit</button>
                  {confirmCategory === category.id ? (
                    <>
                      <button type="button" className={storeBtnGhost} onClick={() => deleteCategory(category.id)}>
                        {removeCategory.isPending ? 'Removing...' : 'Confirm remove'}
                      </button>
                      <button type="button" className={storeBtnGhost} onClick={() => setConfirmCategory(null)}>Cancel</button>
                    </>
                  ) : (
                    <button type="button" className={storeBtnGhost} onClick={() => setConfirmCategory(category.id)}>Remove</button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        ) : null}
      </section>
    </PageShell>
  )

  function openProduct() {
    setProductError('')
    setProductEditor({ draft: emptyProductDraft(categories.data?.[0]?.id ?? '') })
  }

  function editProduct(product: AdminProduct) {
    setProductError('')
    setProductEditor({ id: product.id, draft: productToDraft(product) })
  }

  function openCategory() {
    setCategoryError('')
    const next = (categories.data?.reduce((max, category) => Math.max(max, category.sortOrder), 0) ?? 0) + 1
    setCategoryEditor({ draft: { ...emptyCategoryDraft(), sortOrder: String(next) } })
  }

  function editCategory(category: AdminCategory) {
    setCategoryError('')
    setCategoryEditor({ id: category.id, draft: categoryToDraft(category) })
  }
}
